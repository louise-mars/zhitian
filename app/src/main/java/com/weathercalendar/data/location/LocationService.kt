package com.weathercalendar.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "LocationService"

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
)

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var client: AMapLocationClient? = null

    init {
        AMapLocationClient.updatePrivacyShow(context, true, true)
        AMapLocationClient.updatePrivacyAgree(context, true)
    }

    private fun getClient(): AMapLocationClient {
        return client ?: AMapLocationClient(context).also { client = it }
    }

    suspend fun getCurrentLocation(): Result<LocationResult> {
        if (!hasLocationPermission()) {
            Log.w(TAG, "缺少定位权限")
            return Result.failure(SecurityException("缺少定位权限"))
        }

        return try {
            withTimeout(8_000L) {
                suspendCancellableCoroutine { cont ->
                    var resumed = false
                    val locationClient = getClient()
                    val option = AMapLocationClientOption().apply {
                        locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                        isOnceLocation = true
                        isOnceLocationLatest = true
                        isNeedAddress = true
                        httpTimeOut = 6_000
                    }
                    locationClient.setLocationOption(option)

                    locationClient.setLocationListener { location ->
                        locationClient.stopLocation()
                        if (resumed) return@setLocationListener
                        resumed = true

                        if (location != null && location.errorCode == 0) {
                            val city = location.district?.removeSuffix("区")?.removeSuffix("县")?.removeSuffix("市")
                                ?: location.city?.removeSuffix("市")
                                ?: location.province
                                ?: "未知"
                            Log.d(TAG, "高德定位成功: lat=${location.latitude}, lon=${location.longitude}, city=${location.city}, district=${location.district}, display=$city")
                            cont.resume(
                                Result.success(
                                    LocationResult(
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        cityName = city,
                                    )
                                )
                            )
                        } else {
                            val errMsg = location?.errorInfo ?: "未知错误"
                            Log.w(TAG, "高德定位失败: code=${location?.errorCode}, msg=$errMsg")
                            cont.resume(Result.failure(Exception(errMsg)))
                        }
                    }

                    cont.invokeOnCancellation {
                        locationClient.stopLocation()
                    }

                    locationClient.startLocation()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "定位超时", e)
            Result.failure(e)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }
}
