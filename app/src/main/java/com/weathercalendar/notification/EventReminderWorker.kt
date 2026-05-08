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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.weathercalendar.MainActivity
import com.weathercalendar.R
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 日程提醒 Worker — 在指定时间触发通知。
 * 每个事件创建一个 OneTimeWorkRequest，到时间自动触发。
 */
class EventReminderWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val description = inputData.getString(KEY_DESCRIPTION) ?: ""
        val timeStr = inputData.getString(KEY_TIME) ?: ""
        val eventId = inputData.getLong(KEY_EVENT_ID, 0)

        sendNotification(eventId, title, description, timeStr)
        return Result.success()
    }

    private fun sendNotification(eventId: Long, title: String, description: String, timeStr: String) {
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
            context, eventId.toInt(), intent, PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = buildString {
            if (timeStr.isNotBlank()) append("$timeStr ")
            if (description.isNotBlank()) append(description) else append(title)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📅 $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_BASE + eventId.toInt(),
            notification,
        )
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "日程提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "日历事件到期提醒"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "event_reminders"
        private const val NOTIFICATION_ID_BASE = 2000
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_TIME = "time"
        private const val KEY_EVENT_ID = "event_id"

        /**
         * 为事件安排提醒通知。
         * @param reminderMinutes 提前多少分钟提醒（0 = 事件开始时）
         */
        fun schedule(
            context: Context,
            eventId: Long,
            title: String,
            description: String,
            date: LocalDate,
            time: LocalTime?,
            reminderMinutes: Int,
        ) {
            // 计算触发时间
            val eventDateTime = if (time != null) {
                LocalDateTime.of(date, time)
            } else {
                // 全天事件：当天早上 8:00 提醒
                LocalDateTime.of(date, LocalTime.of(8, 0))
            }

            val triggerTime = eventDateTime.minusMinutes(reminderMinutes.toLong())
            val now = LocalDateTime.now()
            val delay = Duration.between(now, triggerTime)

            // 如果触发时间已过，不安排
            if (delay.isNegative || delay.isZero) return

            val data = Data.Builder()
                .putLong(KEY_EVENT_ID, eventId)
                .putString(KEY_TITLE, title)
                .putString(KEY_DESCRIPTION, description)
                .putString(KEY_TIME, time?.toString() ?: "")
                .build()

            val workRequest = OneTimeWorkRequestBuilder<EventReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "event_reminder_$eventId",
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )
        }

        /** 取消事件的提醒 */
        fun cancel(context: Context, eventId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("event_reminder_$eventId")
        }
    }
}
