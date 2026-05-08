package com.weathercalendar.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.weathercalendar.data.remote.QWeatherApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Widget 自动刷新 Worker — 每小时获取最新天气并更新 Glance Widget。
 *
 * 流程:
 * 1. 从 SharedPreferences 读取城市坐标
 * 2. 调用 QWeatherApi.weatherNow() 获取实时天气
 * 3. 触发 Glance Widget 重新渲染（Widget 从 Room 缓存读取数据）
 */
class WidgetRefreshWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = context.getSharedPreferences("user_prefs_fallback", Context.MODE_PRIVATE)
            val lat = prefs.getString("widget_city_lat", null)?.toDoubleOrNull()
                ?: return Result.success()
            val lon = prefs.getString("widget_city_lon", null)?.toDoubleOrNull()
                ?: return Result.success()

            // 构建 API 客户端
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
            val contentType = "application/json".toMediaType()
            val retrofit = Retrofit.Builder()
                .baseUrl(QWeatherApi.BASE_URL)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
            val api = retrofit.create(QWeatherApi::class.java)

            // 调用实时天气 API（验证网络可达）
            val location = "$lon,$lat"
            val nowResponse = api.weatherNow(location)
            if (nowResponse.code != "200" || nowResponse.now == null) {
                return Result.retry()
            }

            // 触发 Widget 刷新（Widget 从 Room 缓存读取数据）
            val manager = GlanceAppWidgetManager(context)
            val widget = WeatherWidget()
            val glanceIds = manager.getGlanceIds(WeatherWidget::class.java)
            glanceIds.forEach { id ->
                widget.update(context, id)
            }

            Result.success()
        } catch (e: Exception) {
            // 只对网络错误重试，其他错误直接成功（避免无限重试）
            if (e is java.io.IOException) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "widget_refresh_worker"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                1, TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
