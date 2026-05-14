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
import com.weathercalendar.MainActivity
import com.weathercalendar.R
import com.weathercalendar.domain.alert.WeatherAlert

/**
 * 日程天气预警本地推送 — 当检测到恶劣天气与日程冲突时发送通知。
 *
 * 策略：
 * - 每次 App 打开刷新天气后检查
 * - 只推送明天和后天的预警（今天的用户已经看到首页卡片了）
 * - 每天最多推送 1 条（避免打扰）
 * - 用 SharedPreferences 记录上次推送日期，防止重复
 */
object WeatherAlertNotifier {

    private const val CHANNEL_ID = "weather_alert_schedule"
    private const val NOTIFICATION_ID = 3000
    private const val PREFS_KEY = "weather_alert_notifier"
    private const val KEY_LAST_PUSH_DATE = "last_push_date"

    /**
     * 检查是否需要推送预警通知。
     * 只推送明天/后天的预警，且每天最多 1 条。
     */
    fun checkAndNotify(context: Context, alerts: List<WeatherAlert>) {
        if (alerts.isEmpty()) return

        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        // 防止重复推送：每天最多 1 条
        val sp = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        val lastPushDate = sp.getString(KEY_LAST_PUSH_DATE, null)
        if (lastPushDate == today) return

        // 只推送未来 3 天的预警（今天的用户在首页已经看到了）
        val futureAlerts = alerts.filter {
            it.relativeDateText == "明天" || it.relativeDateText == "后天" || it.relativeDateText == "3天后"
        }
        if (futureAlerts.isEmpty()) return

        // 取第一条最紧急的预警
        val alert = futureAlerts.first()
        val title = "⚠️ ${alert.relativeDateText}天气提醒"
        val text = "${alert.relativeDateText}有${alert.eventName}，预计${alert.weatherLabel}，${alert.suggestion}"

        sendNotification(context, title, text)

        // 记录推送日期
        sp.edit().putString(KEY_LAST_PUSH_DATE, today).apply()
    }

    private fun sendNotification(context: Context, title: String, text: String) {
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "日程天气预警",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "当未来日程遇到恶劣天气时提醒"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
