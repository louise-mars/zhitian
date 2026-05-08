package com.weathercalendar.data.repository

import com.weathercalendar.data.model.AirQuality
import com.weathercalendar.data.model.CurrentWeather
import com.weathercalendar.data.model.DailyWeather
import com.weathercalendar.data.model.HourlyForecast
import com.weathercalendar.data.model.LifeIndex
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
    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherData> {
        val location = "%.2f,%.2f".format(longitude, latitude)

        return try {
            coroutineScope {
                val nowDeferred = async { api.weatherNow(location) }
                val dailyDeferred = async {
                    // 优先 15 天预报，失败 fallback 到 7 天
                    try {
                        val resp = api.weather15d(location)
                        if (resp.code == "200") resp else api.weather7d(location)
                    } catch (_: Exception) {
                        api.weather7d(location)
                    }
                }
                val hourlyDeferred = async { api.weather24h(location) }
                val aqiDeferred = async {
                    try {
                        val airResp = api.airNow(location)
                        if (airResp.code == "200" && airResp.now != null) {
                            val aqi = airResp.now!!.aqi.toIntOrNull() ?: 0
                            AirQuality(
                                aqi = aqi,
                                category = airResp.now!!.category,
                                pm2p5 = airResp.now!!.pm2p5,
                                pm10 = airResp.now!!.pm10,
                                color = aqiColor(aqi),
                            )
                        } else null
                    } catch (_: Exception) { null }
                }
                val indicesDeferred = async {
                    try {
                        val resp = api.indices(location)
                        if (resp.code == "200") {
                            resp.daily.map { idx ->
                                LifeIndex(
                                    type = idx.type,
                                    name = idx.name,
                                    category = idx.category,
                                    text = idx.text,
                                )
                            }
                        } else emptyList()
                    } catch (_: Exception) { emptyList<LifeIndex>() }
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
                                    title = w.title, text = w.text,
                                    typeName = w.typeName, level = w.level,
                                    severityColor = w.severityColor,
                                )
                            }
                        } else emptyList()
                    } catch (_: Exception) { emptyList<com.weathercalendar.data.model.WeatherWarning>() }
                }

                val nowResp = nowDeferred.await()
                val dailyResp = dailyDeferred.await()
                val hourlyResp = hourlyDeferred.await()
                val airQuality = aqiDeferred.await()
                val lifeIndices = indicesDeferred.await()
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
                    isDay = isCurrentlyDay(dailyResp.daily.firstOrNull()?.sunrise, dailyResp.daily.firstOrNull()?.sunset),
                )

                val nowHour = LocalTime.now().hour
                val hourlyForecasts = hourlyResp.hourly.mapNotNull { h ->
                    val time = try {
                        LocalDateTime.parse(h.fxTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    } catch (_: Exception) { return@mapNotNull null }
                    HourlyForecast(
                        time = time.toLocalTime(),
                        temperature = h.temp.toIntOrNull() ?: 0,
                        condition = QWeatherCodeMapper.fromIconCode(h.icon),
                        isNow = time.hour == nowHour && time.toLocalDate() == LocalDate.now(),
                    )
                }

                val dailyForecasts = dailyResp.daily.map { d ->
                    DailyWeather(
                        date = LocalDate.parse(d.fxDate),
                        condition = QWeatherCodeMapper.fromIconCode(d.iconDay),
                        tempMin = d.tempMin.toIntOrNull() ?: 0,
                        tempMax = d.tempMax.toIntOrNull() ?: 0,
                    )
                }

                val todayDaily = dailyResp.daily.firstOrNull()
                val details = WeatherDetails(
                    humidity = now.humidity.toIntOrNull() ?: 0,
                    windSpeed = now.windSpeed.toIntOrNull() ?: 0,
                    uvIndex = formatUvIndex(todayDaily?.uvIndex?.toDoubleOrNull() ?: 0.0),
                    airQuality = airQuality?.category ?: "—",
                    sunrise = todayDaily?.sunrise ?: "",
                    sunset = todayDaily?.sunset ?: "",
                )

                Result.success(
                    WeatherData(
                        current = currentWeather,
                        hourly = hourlyForecasts,
                        daily = dailyForecasts,
                        details = details,
                        rainForecast = rainForecast,
                        warnings = warnings,
                        airQuality = airQuality,
                        lifeIndices = lifeIndices,
                        fromCache = false,
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isCurrentlyDay(sunrise: String?, sunset: String?): Boolean {
        if (sunrise.isNullOrBlank() || sunset.isNullOrBlank()) return true
        val now = LocalTime.now()
        val sunriseTime = try { LocalTime.parse(sunrise) } catch (_: Exception) { return true }
        val sunsetTime = try { LocalTime.parse(sunset) } catch (_: Exception) { return true }
        return now.isAfter(sunriseTime) && now.isBefore(sunsetTime)
    }

    private fun aqiColor(aqi: Int): Long = when {
        aqi <= 50 -> 0xFF4CAF50   // 优 - 绿
        aqi <= 100 -> 0xFFFFEB3B  // 良 - 黄
        aqi <= 150 -> 0xFFFF9800  // 轻度 - 橙
        aqi <= 200 -> 0xFFF44336  // 中度 - 红
        aqi <= 300 -> 0xFF9C27B0  // 重度 - 紫
        else -> 0xFF795548        // 严重 - 褐
    }

    private fun formatUvIndex(uv: Double): String = when {
        uv < 3 -> "低"
        uv < 6 -> "中等"
        uv < 8 -> "高"
        uv < 11 -> "很高"
        else -> "极高"
    }
}
