package com.weathercalendar.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 和风天气 API — 中国区数据质量最佳。
 * 文档: https://dev.qweather.com/docs/api/
 * 免费版: 1000 次/天
 */
interface QWeatherApi {

    /** 实时天气 */
    @GET("v7/weather/now")
    suspend fun weatherNow(
        @Query("location") location: String, // "经度,纬度" 或城市 ID
        @Query("key") key: String = API_KEY,
    ): QWeatherNowResponse

    /** 7 天预报 */
    @GET("v7/weather/7d")
    suspend fun weather7d(
        @Query("location") location: String,
        @Query("key") key: String = API_KEY,
    ): QWeatherDailyResponse

    /** 15 天预报 */
    @GET("v7/weather/15d")
    suspend fun weather15d(
        @Query("location") location: String,
        @Query("key") key: String = API_KEY,
    ): QWeatherDailyResponse

    /** 24 小时逐小时预报 */
    @GET("v7/weather/24h")
    suspend fun weather24h(
        @Query("location") location: String,
        @Query("key") key: String = API_KEY,
    ): QWeatherHourlyResponse

    /** 分钟级降雨预报 */
    @GET("v7/minutely/5m")
    suspend fun minutely(
        @Query("location") location: String,
        @Query("key") key: String = API_KEY,
    ): QWeatherMinutelyResponse

    /** 实时空气质量 */
    @GET("v7/air/now")
    suspend fun airNow(
        @Query("location") location: String,
        @Query("key") key: String = API_KEY,
    ): QWeatherAirResponse

    /** 生活指数（全部类型） */
    @GET("v7/indices/1d")
    suspend fun indices(
        @Query("location") location: String,
        @Query("type") type: String = "0",
        @Query("key") key: String = API_KEY,
    ): QWeatherIndicesResponse

    /** 天气灾害预警 */
    @GET("v7/warning/now")
    suspend fun warningNow(
        @Query("location") location: String,
        @Query("key") key: String = API_KEY,
    ): QWeatherWarningResponse

    /** 城市搜索 */
    @GET("geo/v2/city/lookup")
    suspend fun cityLookup(
        @Query("location") location: String,  // 城市名
        @Query("key") key: String = API_KEY,
        @Query("number") number: Int = 10,
    ): QWeatherCityResponse

    companion object {
        const val BASE_URL = "https://k678m2r6e6.re.qweatherapi.com/"
        val API_KEY: String get() = com.weathercalendar.BuildConfig.QWEATHER_API_KEY
    }
}

@Serializable
data class QWeatherCityResponse(
    val code: String,
    val location: List<QWeatherCity> = emptyList(),
)

@Serializable
data class QWeatherCity(
    val name: String,
    val id: String = "",
    val lat: String,
    val lon: String,
    val adm1: String = "",  // 省
    val adm2: String = "",  // 市
    val country: String = "",
)

// ─────────────────────────────────────────────
// 响应模型
// ─────────────────────────────────────────────

@Serializable
data class QWeatherNowResponse(
    val code: String,
    val now: QWeatherNow? = null,
)

@Serializable
data class QWeatherNow(
    val temp: String,           // 温度
    val feelsLike: String,      // 体感温度
    val icon: String,           // 天气图标代码
    val text: String,           // 天气描述（晴、多云等）
    val windSpeed: String,      // 风速 km/h
    val humidity: String,       // 湿度 %
    @SerialName("wind360") val windDir: String = "", // 风向角度
)

@Serializable
data class QWeatherDailyResponse(
    val code: String,
    val daily: List<QWeatherDaily> = emptyList(),
)

@Serializable
data class QWeatherDaily(
    val fxDate: String,         // 日期 "2026-04-16"
    val tempMax: String,
    val tempMin: String,
    val iconDay: String,
    val textDay: String,
    val iconNight: String,
    val textNight: String,
    val uvIndex: String = "0",
    val sunrise: String = "",   // "06:12"
    val sunset: String = "",    // "18:45"
    @SerialName("windSpeedDay") val windSpeed: String = "0",
)

@Serializable
data class QWeatherHourlyResponse(
    val code: String,
    val hourly: List<QWeatherHourly> = emptyList(),
)

@Serializable
data class QWeatherHourly(
    val fxTime: String,         // "2026-04-16T14:00+08:00"
    val temp: String,
    val icon: String,
    val text: String,
)

@Serializable
data class QWeatherMinutelyResponse(
    val code: String,
    val summary: String = "",   // "未来两小时无降水" 或 "32分钟后开始下雨"
    val minutely: List<QWeatherMinutely> = emptyList(),
)

@Serializable
data class QWeatherMinutely(
    val fxTime: String,
    val precip: String,         // 降水量 mm
    val type: String = "",      // rain/snow
)

@Serializable
data class QWeatherAirResponse(
    val code: String,
    val now: QWeatherAirNow? = null,
)

@Serializable
data class QWeatherAirNow(
    val aqi: String,            // AQI 指数
    val category: String,       // "优"/"良"/"轻度污染" 等
    val pm2p5: String = "",
    val pm10: String = "",
)

@Serializable
data class QWeatherIndicesResponse(
    val code: String,
    val daily: List<QWeatherIndex> = emptyList(),
)

@Serializable
data class QWeatherIndex(
    val type: String,           // 1=运动 2=洗车 3=穿衣 5=紫外线 ...
    val name: String,           // "运动指数"
    val category: String,       // "适宜"/"较适宜" 等
    val text: String,           // 详细描述
)

@Serializable
data class QWeatherWarningResponse(
    val code: String,
    val warning: List<QWeatherWarning> = emptyList(),
)

@Serializable
data class QWeatherWarning(
    val id: String = "",
    val sender: String = "",        // 发布单位
    val pubTime: String = "",       // 发布时间
    val title: String = "",         // "北京市气象台发布暴雨蓝色预警"
    val text: String = "",          // 详细内容
    val typeName: String = "",      // "暴雨"
    val level: String = "",         // "蓝色"
    val severity: String = "",      // Minor/Moderate/Severe/Extreme
    val severityColor: String = "", // 预警颜色
)
