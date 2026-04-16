package com.weathercalendar.data.repository

import com.weathercalendar.data.local.WeatherDao
import com.weathercalendar.data.local.WeatherEntity
import com.weathercalendar.data.model.CurrentWeather
import com.weathercalendar.data.model.DailyWeather
import com.weathercalendar.data.model.HourlyForecast
import com.weathercalendar.data.model.RainForecast
import com.weathercalendar.data.model.WeatherDetails
import com.weathercalendar.data.remote.AirQualityApi
import com.weathercalendar.data.remote.OpenMeteoResponse
import com.weathercalendar.data.remote.WeatherApi
import com.weathercalendar.data.remote.WeatherCodeMapper
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class WeatherData(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyWeather>,
    val details: WeatherDetails,
    val rainForecast: RainForecast? = null,
    val fromCache: Boolean = false,
)

/**
 * 天气数据仓库 — 缓存优先策略：
 * 1. 先读 Room 缓存
 * 2. 缓存未过期（<30分钟）→ 直接返回
 * 3. 缓存过期或无缓存 → 请求 API
 * 4. API 成功 → 更新缓存 → 返回新数据
 * 5. API 失败 + 有旧缓存 → 返回旧缓存（标记 fromCache）
 * 6. API 失败 + 无缓存 → 返回错误
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val api: WeatherApi,
    private val airQualityApi: AirQualityApi,
    private val qWeatherRepository: QWeatherRepository,
    private val dao: WeatherDao,
    private val json: Json,
) {
    /**
     * 双数据源策略：
     * 1. 先读 Room 缓存（快速显示）
     * 2. 尝试和风天气（中国区主数据源）
     * 3. 和风失败 → fallback 到 Open-Meteo
     * 4. 成功后写入缓存
     */
    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherData> {
        val cacheKey = WeatherEntity.key(latitude, longitude)

        // 1. 读缓存（纯本地，不触发任何网络请求）
        val cached = dao.get(cacheKey)

        // 2. 缓存未过期 → 直接返回（零网络）
        if (cached != null && !cached.isExpired) {
            return try {
                val response = json.decodeFromString<OpenMeteoResponse>(cached.responseJson)
                Result.success(mapResponse(response, fromCache = true, aqiLabel = "—"))
            } catch (_: Exception) {
                fetchAndCache(latitude, longitude, cacheKey, staleCache = cached)
            }
        }

        // 3. 缓存过期或无缓存 → 请求 API（此时才做网络请求）
        return fetchAndCache(latitude, longitude, cacheKey, staleCache = cached)
    }

    /** 仅在需要网络刷新时才请求 AQI */
    private suspend fun fetchAqi(latitude: Double, longitude: Double): String {
        return try {
            val response = airQualityApi.getCurrent(latitude, longitude)
            val aqi = response.current?.europeanAqi ?: return "—"
            formatAqi(aqi)
        } catch (_: Exception) {
            "—"
        }
    }

    private fun formatAqi(aqi: Int): String = when {
        aqi <= 20 -> "优"
        aqi <= 40 -> "良"
        aqi <= 60 -> "轻度"
        aqi <= 80 -> "中度"
        aqi <= 100 -> "重度"
        else -> "严重"
    }
    }

    private suspend fun fetchAndCache(
        latitude: Double,
        longitude: Double,
        cacheKey: String,
        staleCache: WeatherEntity?,
    ): Result<WeatherData> {
        // AQI 只在网络刷新时请求
        val aqiLabel = fetchAqi(latitude, longitude)

        // 1. 尝试和风天气（中国区主数据源）
        val qResult = try {
            qWeatherRepository.getWeather(latitude, longitude)
        } catch (_: Exception) {
            Result.failure(Exception("QWeather failed"))
        }

        if (qResult.isSuccess) {
            val data = qResult.getOrThrow()
            // 缓存 Open-Meteo 格式的数据（保持缓存兼容性）
            try {
                val response = retryWithBackoff { api.getForecast(latitude, longitude) }
                val responseJson = json.encodeToString(OpenMeteoResponse.serializer(), response)
                dao.insert(WeatherEntity(cacheKey = cacheKey, responseJson = responseJson, updatedAt = System.currentTimeMillis()))
                dao.deleteOlderThan(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
            } catch (_: Exception) {
                // 缓存写入失败不影响返回
            }
            return Result.success(data)
        }

        // 2. 和风失败 → fallback 到 Open-Meteo
        return try {
            val response = retryWithBackoff { api.getForecast(latitude, longitude) }

            val responseJson = json.encodeToString(OpenMeteoResponse.serializer(), response)
            dao.insert(WeatherEntity(cacheKey = cacheKey, responseJson = responseJson, updatedAt = System.currentTimeMillis()))
            dao.deleteOlderThan(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)

            Result.success(mapResponse(response, fromCache = false, aqiLabel = aqiLabel))
        } catch (e: Exception) {
            if (staleCache != null) {
                try {
                    val response = json.decodeFromString<OpenMeteoResponse>(staleCache.responseJson)
                    Result.success(mapResponse(response, fromCache = true, aqiLabel = aqiLabel))
                } catch (_: Exception) {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    private fun mapResponse(response: OpenMeteoResponse, fromCache: Boolean, aqiLabel: String = "—"): WeatherData {
        val current = response.current!!
        val hourly = response.hourly!!
        val daily = response.daily!!

        val now = LocalDateTime.now()

        val currentWeather = CurrentWeather(
            temperature = current.temperature.toInt(),
            feelsLike = current.apparentTemperature.toInt(),
            condition = WeatherCodeMapper.fromWmoCode(current.weatherCode),
            isDay = current.isDay == 1,
        )

        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val hourlyForecasts = hourly.time.mapIndexedNotNull { index, timeStr ->
            val time = try {
                LocalDateTime.parse(timeStr, isoFormatter)
            } catch (_: Exception) {
                return@mapIndexedNotNull null
            }
            if (time.isBefore(now.minusHours(1)) || time.isAfter(now.plusHours(24))) {
                return@mapIndexedNotNull null
            }
            HourlyForecast(
                time = time.toLocalTime(),
                temperature = hourly.temperature.getOrElse(index) { 0.0 }.toInt(),
                condition = WeatherCodeMapper.fromWmoCode(
                    hourly.weatherCode.getOrElse(index) { 0 }
                ),
                isNow = time.hour == now.hour && time.toLocalDate() == now.toLocalDate(),
            )
        }

        val dailyForecasts = daily.time.mapIndexed { index, dateStr ->
            DailyWeather(
                date = LocalDate.parse(dateStr),
                condition = WeatherCodeMapper.fromWmoCode(
                    daily.weatherCode.getOrElse(index) { 0 }
                ),
                tempMin = daily.temperatureMin.getOrElse(index) { 0.0 }.toInt(),
                tempMax = daily.temperatureMax.getOrElse(index) { 0.0 }.toInt(),
            )
        }

        // 日出日落
        val sunriseStr = daily.sunrise.firstOrNull()?.let {
            try { it.substringAfter("T").take(5) } catch (_: Exception) { "" }
        } ?: ""
        val sunsetStr = daily.sunset.firstOrNull()?.let {
            try { it.substringAfter("T").take(5) } catch (_: Exception) { "" }
        } ?: ""

        val details = WeatherDetails(
            humidity = current.humidity,
            windSpeed = current.windSpeed.toInt(),
            uvIndex = formatUvIndex(daily.uvIndexMax.firstOrNull() ?: 0.0),
            airQuality = aqiLabel,
            sunrise = sunriseStr,
            sunset = sunsetStr,
        )

        // 分钟级降雨预报
        val rainForecast = parseRainForecast(response.minutely15)

        return WeatherData(
            current = currentWeather,
            hourly = hourlyForecasts,
            daily = dailyForecasts,
            details = details,
            rainForecast = rainForecast,
            fromCache = fromCache,
        )
    }

    /**
     * 解析 15 分钟级降水数据，生成降雨预报摘要。
     */
    private fun parseRainForecast(minutely: com.weathercalendar.data.remote.OpenMeteoMinutely15?): RainForecast? {
        if (minutely == null || minutely.precipitation.isEmpty()) return null

        val now = LocalDateTime.now()
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        // 找到当前时间之后的数据
        val futureData = minutely.time.zip(minutely.precipitation).mapNotNull { (timeStr, precip) ->
            val time = try { LocalDateTime.parse(timeStr, isoFormatter) } catch (_: Exception) { return@mapNotNull null }
            if (time.isBefore(now.minusMinutes(15))) return@mapNotNull null
            time to precip
        }.take(8) // 未来 2 小时（8 个 15 分钟间隔）

        if (futureData.isEmpty()) return null

        val isCurrentlyRaining = futureData.firstOrNull()?.second?.let { it > 0.1 } ?: false

        if (isCurrentlyRaining) {
            // 当前在下雨，找什么时候停
            val stopIndex = futureData.indexOfFirst { it.second < 0.1 }
            val minutesToStop = if (stopIndex > 0) stopIndex * 15 else null
            val summary = if (minutesToStop != null) {
                "当前有雨，预计 ${minutesToStop} 分钟后停"
            } else {
                "持续降雨中"
            }
            return RainForecast(summary = summary, isRaining = true, minutesToRain = null, minutesToStop = minutesToStop)
        } else {
            // 当前没下雨，找什么时候开始
            val startIndex = futureData.indexOfFirst { it.second > 0.1 }
            if (startIndex < 0) {
                return RainForecast(summary = "2 小时内无降雨", isRaining = false, minutesToRain = null, minutesToStop = null)
            }
            val minutesToRain = startIndex * 15
            return RainForecast(
                summary = "${minutesToRain} 分钟后可能下雨",
                isRaining = false,
                minutesToRain = minutesToRain,
                minutesToStop = null,
            )
        }
    }

    private fun formatUvIndex(uv: Double): String = when {
        uv < 3 -> "低"
        uv < 6 -> "中等"
        uv < 8 -> "高"
        uv < 11 -> "很高"
        else -> "极高"
    }

    /** 指数退避重试：1s → 2s → 4s，最多 3 次 */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelayMs
        repeat(maxRetries - 1) {
            try {
                return block()
            } catch (_: Exception) {
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return block() // 最后一次不 catch，让异常抛出
    }
}
