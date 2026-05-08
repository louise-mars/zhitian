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
    init {
        AMapLocationClient.updatePrivacyShow(context, true, true)
        AMapLocationClient.updatePrivacyAgree(context, true)
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
                    // 每次创建新 client，避免并发调用共享 listener 的竞态
                    val locationClient = AMapLocationClient(context)
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
                        locationClient.onDestroy()
                        if (resumed) return@setLocationListener
                        resumed = true

                        if (location != null && location.errorCode == 0) {
                            val district = location.district?.removeSuffix("区")?.removeSuffix("县")?.removeSuffix("市")
                            val street = location.street?.removeSuffix("街道")?.removeSuffix("镇")?.removeSuffix("乡")
                            val city = when {
                                district != null && !street.isNullOrBlank() -> "$district·$street"
                                district != null -> district
                                else -> location.city?.removeSuffix("市") ?: location.province ?: "未知"
                            }
                            Log.d(TAG, "高德定位成功: lat=${location.latitude}, lon=${location.longitude}, display=$city")
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
                        locationClient.onDestroy()
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
