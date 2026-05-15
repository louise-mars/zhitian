package com.weathercalendar.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.weathercalendar.MainActivity
import com.weathercalendar.R
import com.weathercalendar.data.local.AppDatabase
import com.weathercalendar.data.local.WeatherEntity
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.data.remote.OpenMeteoResponse
import com.weathercalendar.data.remote.WeatherCodeMapper
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * 智能天气通知 — 每 6 小时检查一次。
 *
 * 通知类型：
 * 1. 明天有雨/雪 → "记得带伞"
 * 2. 温差 > 10° → "注意穿衣"
 * 3. 日程+天气联动 → "明天有会议且下雨，建议提前出门"
 */
class WeatherNotificationWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun doWork(): Result {
        return try {
            val prefs = context.getSharedPreferences("user_prefs_fallback", Context.MODE_PRIVATE)
            val notificationEnabled = prefs.getBoolean("weather_notification", false)
            if (!notificationEnabled) return Result.success()

            val cityLat = prefs.getString("widget_city_lat", null)?.toDoubleOrNull() ?: 39.9042
            val cityLon = prefs.getString("widget_city_lon", null)?.toDoubleOrNull() ?: 116.4074

            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "weather_calendar.db",
            )
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
                .build()

            try {
                val cacheKey = WeatherEntity.key(cityLat, cityLon)
                val cached = db.weatherDao().get(cacheKey)

                // 读取明天的 App 内事件
                val tomorrow = LocalDate.now().plusDays(1)
                val tomorrowEvents = try {
                    db.eventDao().getByDate(tomorrow.toString())
                } catch (_: Exception) {
                    emptyList()
                }

                if (cached == null) return Result.success()

                val response = json.decodeFromString<OpenMeteoResponse>(cached.responseJson)
                checkAndNotify(response, tomorrowEvents.isNotEmpty())
            } finally {
                db.close()
            }

            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    private fun checkAndNotify(response: OpenMeteoResponse, hasTomorrowEvents: Boolean) {
        val daily = response.daily ?: return
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val index = daily.time.indexOf(tomorrow)
        if (index < 0) return

        val code = daily.weatherCode.getOrNull(index) ?: return
        val condition = WeatherCodeMapper.fromWmoCode(code)
        val min = daily.temperatureMin.getOrNull(index)?.toInt() ?: return
        val max = daily.temperatureMax.getOrNull(index)?.toInt() ?: return
        val tempDiff = max - min

        // 1. 雨/雪提醒
        val isRainOrSnow = condition in listOf(
            WeatherCondition.RAINY, WeatherCondition.DRIZZLE,
            WeatherCondition.SNOWY, WeatherCondition.STORMY,
        )
        if (isRainOrSnow) {
            val title = when (condition) {
                WeatherCondition.SNOWY -> "明天有雪 ❄️"
                WeatherCondition.STORMY -> "明天有雷暴 ⛈️"
                else -> "明天有雨 🌧️"
            }
            val text = when (condition) {
                WeatherCondition.SNOWY -> "明天 $min°~$max°，注意保暖"
                else -> "明天 $min°~$max°，记得带伞"
            }
            sendNotification(NOTIFICATION_ID_RAIN, title, text)
        }

        // 2. 温差提醒（>10°）
        if (tempDiff > 10) {
            sendNotification(
                NOTIFICATION_ID_TEMP,
                "明天温差较大 🌡️",
                "明天 $min°~$max°（温差${tempDiff}°），注意穿衣",
            )
        }

        // 3. 日程+天气联动提醒
        if (hasTomorrowEvents && isRainOrSnow) {
            sendNotification(
                NOTIFICATION_ID_EVENT,
                "明天有日程且天气不佳 📅",
                "明天有安排且预计${condition.label}，建议提前出门",
            )
        }
    }

    private fun sendNotification(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        ensureChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "天气提醒",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "天气变化、温差和日程联动提醒"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "weather_alerts"
        private const val WORK_NAME = "weather_notification"
        private const val NOTIFICATION_ID_RAIN = 1001
        private const val NOTIFICATION_ID_TEMP = 1002
        private const val NOTIFICATION_ID_EVENT = 1003

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
                6, TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
