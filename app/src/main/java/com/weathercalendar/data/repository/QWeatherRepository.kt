package com.weathercalendar.data.repository

import com.weathercalendar.data.model.CurrentWeather
import com.weathercalendar.data.model.DailyWeather
import com.weathercalendar.data.model.HourlyForecast
import com.weathercalendar.data.model.RainForecast
import com.weathercalendar.data.model.WeatherDetails
import com.weathercalendar.data.remote.QWeatherApi
import com.weathercalendar.data.remote.QWeatherCodeMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 和风天气数据仓库 — 中国区主数据源。
 * 将和风天气 API 响应转换为 App 通用的 WeatherData 模型。
 */
@Singleton
class QWeatherRepository @Inject constructor(
    private val api: QWeatherApi,
) {
    /**
     * 获取完整天气数据（实况 + 小时 + 7 天 + 降雨 + 空气质量 + 生活指数）。
     * @param location "经度,纬度" 格式
     */
    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherData> {
        val location = "%.2f,%.2f".format(longitude, latitude) // 和风用 "经度,纬度"

        return try {
            // 并行请求所有数据
            coroutineScope {
                val nowDeferred = async { api.weatherNow(location) }
                val dailyDeferred = async { api.weather7d(location) }
                val hourlyDeferred = async { api.weather24h(location) }
                val aqiDeferred = async {
                    try {
                        val airResp = api.airNow(location)
                        if (airResp.code == "200") airResp.now?.category ?: "—" else "—"
                    } catch (_: Exception) { "—" }
                }
                val rainDeferred = async {
                    try {
                        val minuteResp = api.minutely(location)
                        if (minuteResp.code == "200" && minuteResp.summary.isNotBlank()) {
                            RainForecast(
                                summary = minuteResp.summary,
                                isRaining = minuteResp.minutely.firstOrNull()?.precip?.toDoubleOrNull()?.let { it > 0 } ?: false,
                                minutesToRain = null,
                                minutesToStop = null,
                            )
                        } else null
                    } catch (_: Exception) { null }
                }
                val warningDeferred = async {
                    try {
                        val warnResp = api.warningNow(location)
                        if (warnResp.code == "200") {
                            warnResp.warning.map { w ->
                                com.weathercalendar.data.model.WeatherWarning(
                                    title = w.title,
                                    text = w.text,
                                    typeName = w.typeName,
                                    level = w.level,
                                    severityColor = w.severityColor,
                                )
                            }
                        } else emptyList<com.weathercalendar.data.model.WeatherWarning>()
                    } catch (_: Exception) { emptyList<com.weathercalendar.data.model.WeatherWarning>() }
                }

                val nowResp = nowDeferred.await()
                val dailyResp = dailyDeferred.await()
                val hourlyResp = hourlyDeferred.await()
                val aqiLabel = aqiDeferred.await()
                val rainForecast = rainDeferred.await()
                val warnings = warningDeferred.await()

                if (nowResp.code != "200" || nowResp.now == null) {
                    return@coroutineScope Result.failure(Exception("和风天气 API 错误: ${nowResp.code}"))
                }

                val now = nowResp.now!!
                val condition = QWeatherCodeMapper.fromIconCode(now.icon)

                val currentWeather = CurrentWeather(
                    temperature = now.temp.toIntOrNull() ?: 0,
                    feelsLike = now.feelsLike.toIntOrNull() ?: 0,
                    condition = condition,
                    isDay = true,
                )

                // 小时预报
                val nowHour = LocalTime.now().hour
                val hourlyForecasts = hourlyResp.hourly.mapNotNull { h ->
                    val time = try {
                        LocalDateTime.parse(h.fxTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    } catch (_: Exception) {
                        return@mapNotNull null
                    }
                    HourlyForecast(
                        time = time.toLocalTime(),
                        temperature = h.temp.toIntOrNull() ?: 0,
                        condition = QWeatherCodeMapper.fromIconCode(h.icon),
                        isNow = time.hour == nowHour && time.toLocalDate() == LocalDate.now(),
                    )
                }

                // 7 天预报
                val dailyForecasts = dailyResp.daily.map { d ->
                    DailyWeather(
                        date = LocalDate.parse(d.fxDate),
                        condition = QWeatherCodeMapper.fromIconCode(d.iconDay),
                        tempMin = d.tempMin.toIntOrNull() ?: 0,
                        tempMax = d.tempMax.toIntOrNull() ?: 0,
                    )
                }

                // 日出日落
                val todayDaily = dailyResp.daily.firstOrNull()
                val sunrise = todayDaily?.sunrise ?: ""
                val sunset = todayDaily?.sunset ?: ""

                val details = WeatherDetails(
                    humidity = now.humidity.toIntOrNull() ?: 0,
                    windSpeed = now.windSpeed.toIntOrNull() ?: 0,
                    uvIndex = formatUvIndex(todayDaily?.uvIndex?.toDoubleOrNull() ?: 0.0),
                    airQuality = aqiLabel,
                    sunrise = sunrise,
                    sunset = sunset,
                )

                Result.success(
                    WeatherData(
                        current = currentWeather,
                        hourly = hourlyForecasts,
                        daily = dailyForecasts,
                        details = details,
                        rainForecast = rainForecast,
                        warnings = warnings,
                        fromCache = false,
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatUvIndex(uv: Double): String = when {
        uv < 3 -> "低"
        uv < 6 -> "中等"
        uv < 8 -> "高"
        uv < 11 -> "很高"
        else -> "极高"
    }
}
