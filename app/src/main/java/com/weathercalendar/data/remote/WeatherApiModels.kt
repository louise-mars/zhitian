package com.weathercalendar.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open-Meteo API 响应模型。
 */
@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: OpenMeteoCurrent? = null,
    val hourly: OpenMeteoHourly? = null,
    val daily: OpenMeteoDaily? = null,
    @SerialName("minutely_15") val minutely15: OpenMeteoMinutely15? = null,
)

@Serializable
data class OpenMeteoCurrent(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("is_day") val isDay: Int,
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
)

@Serializable
data class OpenMeteoDaily(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerialName("uv_index_max") val uvIndexMax: List<Double>,
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double>,
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
)

/** 15 分钟级降水数据 */
@Serializable
data class OpenMeteoMinutely15(
    val time: List<String> = emptyList(),
    val precipitation: List<Double> = emptyList(),
)

/**
 * Open-Meteo Geocoding API 响应模型。
 */
@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null,
)

@Serializable
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val admin1: String? = null,
)
