package com.weathercalendar.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Air Quality API — 免费，无需 Key。
 */
interface AirQualityApi {

    @GET("v1/air-quality")
    suspend fun getCurrent(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "european_aqi",
        @Query("timezone") timezone: String = "auto",
    ): AirQualityResponse

    companion object {
        const val BASE_URL = "https://air-quality-api.open-meteo.com/"
    }
}

@Serializable
data class AirQualityResponse(
    val current: AirQualityCurrent? = null,
)

@Serializable
data class AirQualityCurrent(
    @SerialName("european_aqi") val europeanAqi: Int? = null,
)
