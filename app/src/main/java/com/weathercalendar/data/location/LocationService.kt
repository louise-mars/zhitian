package com.weathercalendar.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.weathercalendar.data.remote.NominatimApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
)

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nominatimApi: NominatimApi,
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    suspend fun getCurrentLocation(): Result<LocationResult> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext Result.failure(SecurityException("缺少定位权限"))
        }

        try {
            val location = getLastOrCurrentLocation()
                ?: return@withContext Result.failure(Exception("无法获取位置"))

            val cityName = reverseGeocode(location.first, location.second)

            Result.success(
                LocationResult(
                    latitude = location.first,
                    longitude = location.second,
                    cityName = cityName,
                )
            )
        } catch (e: Exception) {
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

    @Suppress("MissingPermission")
    private suspend fun getLastOrCurrentLocation(): Pair<Double, Double>? {
        val lastLocation = suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(location.latitude to location.longitude)
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }

        if (lastLocation != null) return lastLocation

        return suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(location.latitude to location.longitude)
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }
    }

    /**
     * 三级 fallback 反向地理编码：
     * 1. Android Geocoder（需要 Google Play Services 后端）
     * 2. Nominatim API（OpenStreetMap，需要网络）
     * 3. 中国主要城市坐标查表（离线，永远可用）
     */
    private suspend fun reverseGeocode(lat: Double, lon: Double): String {
        // 1. Android Geocoder
        val androidResult = tryAndroidGeocoder(lat, lon)
        if (androidResult != null) return androidResult

        // 2. Nominatim API
        val nominatimResult = tryNominatim(lat, lon)
        if (nominatimResult != null) return nominatimResult

        // 3. 离线城市查表
        return findNearestCity(lat, lon)
    }

    private fun tryAndroidGeocoder(lat: Double, lon: Double): String? {
        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.locality
                ?: addresses?.firstOrNull()?.subAdminArea
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun tryNominatim(lat: Double, lon: Double): String? {
        return try {
            val response = nominatimApi.reverseGeocode(latitude = lat, longitude = lon)
            val address = response.address
            address?.city ?: address?.town ?: address?.village ?: address?.county
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 离线城市查表 — 根据经纬度找最近的中国主要城市。
     * 覆盖所有省会 + 主要城市，确保在无网络/无 Google 服务时也能显示城市名。
     */
    private fun findNearestCity(lat: Double, lon: Double): String {
        var minDist = Double.MAX_VALUE
        var nearest = "未知位置"
        for ((name, cLat, cLon) in CHINA_CITIES) {
            val dist = haversineApprox(lat, lon, cLat, cLon)
            if (dist < minDist) {
                minDist = dist
                nearest = name
            }
        }
        return nearest
    }

    /** 简化距离计算（不需要精确，只需要比较大小） */
    private fun haversineApprox(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = lat1 - lat2
        val dLon = (lon1 - lon2) * cos(Math.toRadians((lat1 + lat2) / 2))
        return sqrt(dLat * dLat + dLon * dLon)
    }

    companion object {
        // (城市名, 纬度, 经度)
        private val CHINA_CITIES = listOf(
            Triple("北京", 39.9042, 116.4074),
            Triple("上海", 31.2304, 121.4737),
            Triple("广州", 23.1291, 113.2644),
            Triple("深圳", 22.5431, 114.0579),
            Triple("成都", 30.5728, 104.0668),
            Triple("杭州", 30.2741, 120.1551),
            Triple("武汉", 30.5928, 114.3055),
            Triple("西安", 34.3416, 108.9398),
            Triple("南京", 32.0603, 118.7969),
            Triple("重庆", 29.4316, 106.9123),
            Triple("天津", 39.3434, 117.3616),
            Triple("苏州", 31.2990, 120.5853),
            Triple("长沙", 28.2282, 112.9388),
            Triple("郑州", 34.7466, 113.6254),
            Triple("东莞", 23.0208, 113.7518),
            Triple("青岛", 36.0671, 120.3826),
            Triple("沈阳", 41.8057, 123.4315),
            Triple("宁波", 29.8683, 121.5440),
            Triple("昆明", 25.0389, 102.7183),
            Triple("大连", 38.9140, 121.6147),
            Triple("厦门", 24.4798, 118.0894),
            Triple("合肥", 31.8206, 117.2272),
            Triple("佛山", 23.0218, 113.1219),
            Triple("福州", 26.0745, 119.2965),
            Triple("哈尔滨", 45.8038, 126.5350),
            Triple("济南", 36.6512, 116.9972),
            Triple("温州", 28.0000, 120.6722),
            Triple("长春", 43.8171, 125.3235),
            Triple("石家庄", 38.0428, 114.5149),
            Triple("贵阳", 26.6470, 106.6302),
            Triple("南宁", 22.8170, 108.3665),
            Triple("太原", 37.8706, 112.5489),
            Triple("南昌", 28.6820, 115.8579),
            Triple("兰州", 36.0611, 103.8343),
            Triple("海口", 20.0174, 110.3492),
            Triple("乌鲁木齐", 43.8256, 87.6168),
            Triple("呼和浩特", 40.8424, 111.7490),
            Triple("拉萨", 29.6500, 91.1000),
            Triple("西宁", 36.6171, 101.7782),
            Triple("银川", 38.4872, 106.2309),
            Triple("珠海", 22.2710, 113.5767),
            Triple("无锡", 31.4912, 120.3119),
            Triple("烟台", 37.4638, 121.4479),
            Triple("惠州", 23.1116, 114.4161),
            Triple("中山", 22.5176, 113.3926),
            Triple("台北", 25.0330, 121.5654),
            Triple("香港", 22.3193, 114.1694),
            Triple("澳门", 22.1987, 113.5439),
        )
    }
}
