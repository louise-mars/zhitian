package com.weathercalendar.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.weathercalendar.MainActivity
import com.weathercalendar.R
import com.weathercalendar.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "EventReminder"

/**
 * 日程提醒广播接收器 — AlarmManager 精确触发。
 * 在指定时间收到广播后发送通知。
 */
class EventReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(KEY_TITLE) ?: return
        val description = intent.getStringExtra(KEY_DESCRIPTION) ?: ""
        val timeStr = intent.getStringExtra(KEY_TIME) ?: ""
        val eventId = intent.getLongExtra(KEY_EVENT_ID, 0)

        Log.d(TAG, "提醒触发: eventId=$eventId, title=$title")
        sendNotification(context, eventId, title, description, timeStr)
    }

    private fun sendNotification(context: Context, eventId: Long, title: String, description: String, timeStr: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, eventId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
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
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_BASE + eventId.toInt(),
            notification,
        )
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "日程提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "日历事件到期提醒"
            enableVibration(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "event_reminders"
        private const val NOTIFICATION_ID_BASE = 2000
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_TIME = "time"
        const val KEY_EVENT_ID = "event_id"
    }
}

/**
 * 日程提醒调度器 — 使用 AlarmManager 实现精确定时提醒。
 *
 * 优势（对比 WorkManager）：
 * - 精确到分钟级触发（setExactAndAllowWhileIdle）
 * - Doze 模式下也能工作
 * - 长时间延迟（1天/1周）也可靠
 * - 设备重启后通过 BOOT_COMPLETED 广播恢复
 */
object EventReminderScheduler {

    /**
     * 为事件安排提醒。
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
        val eventDateTime = if (time != null) {
            LocalDateTime.of(date, time)
        } else {
            // 全天事件：当天早上 8:00 提醒
            LocalDateTime.of(date, LocalTime.of(8, 0))
        }

        val triggerDateTime = eventDateTime.minusMinutes(reminderMinutes.toLong())
        val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        // 如果触发时间已过，不安排
        if (triggerMillis <= now) {
            Log.d(TAG, "提醒时间已过，跳过: eventId=$eventId")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, eventId, title, description, time?.toString() ?: "")

        // 使用精确闹钟（Doze 模式下也能触发）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要检查精确闹钟权限
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                // 没有精确闹钟权限，用非精确方式
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }

        Log.d(TAG, "提醒已安排: eventId=$eventId, trigger=${triggerDateTime}")
    }

    /** 取消事件的提醒 */
    fun cancel(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, eventId, "", "", "")
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "提醒已取消: eventId=$eventId")
    }

    private fun createPendingIntent(
        context: Context,
        eventId: Long,
        title: String,
        description: String,
        timeStr: String,
    ): PendingIntent {
        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            action = "com.weathercalendar.EVENT_REMINDER_$eventId"
            putExtra(EventReminderReceiver.KEY_EVENT_ID, eventId)
            putExtra(EventReminderReceiver.KEY_TITLE, title)
            putExtra(EventReminderReceiver.KEY_DESCRIPTION, description)
            putExtra(EventReminderReceiver.KEY_TIME, timeStr)
        }
        return PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /**
     * 重新安排未来 7 天内所有事件（含重复事件）的提醒。
     * 在设备重启和 App 启动时调用。
     */
    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "weather_calendar.db"
                ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
                    .build()

                val today = LocalDate.now()
                val endDate = today.plusDays(7)
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                var count = 0

                // 非重复事件
                val events = db.eventDao().getBetween(today.toString(), endDate.toString())
                events.filter { it.reminderMinutes != null && (it.recurrenceRule.isNullOrBlank() || it.recurrenceRule == "none") }
                    .forEach { event ->
                        val date = LocalDate.parse(event.date)
                        val time = event.time?.let { LocalTime.parse(it, timeFormatter) }
                        schedule(context, event.id, event.title, event.description, date, time, event.reminderMinutes!!)
                        count++
                    }

                // 重复事件 — 展开未来 7 天的实例并安排提醒
                val recurring = db.eventDao().getRecurringBefore(endDate.toString())
                recurring.filter { it.reminderMinutes != null }.forEach { event ->
                    val eventStart = LocalDate.parse(event.date)
                    val time = event.time?.let { LocalTime.parse(it, timeFormatter) }
                    val rule = event.recurrenceRule ?: return@forEach

                    var current = today
                    while (current <= endDate) {
                        if (matchesRule(current, eventStart, rule)) {
                            // 用唯一 ID 区分每次出现：eventId * 1000 + dayOfYear
                            val occurrenceId = event.id * 1000 + current.dayOfYear.toLong()
                            schedule(context, occurrenceId, event.title, event.description, current, time, event.reminderMinutes!!)
                            count++
                        }
                        current = current.plusDays(1)
                    }
                }

                db.close()
                Log.d(TAG, "rescheduleAll 完成，已安排 $count 个提醒")
            } catch (e: Exception) {
                Log.e(TAG, "rescheduleAll 失败", e)
            }
        }
    }

    private fun matchesRule(date: LocalDate, eventStart: LocalDate, rule: String): Boolean {
        if (date.isBefore(eventStart)) return false
        return when {
            rule == "daily" -> true
            rule == "weekly" -> date.dayOfWeek == eventStart.dayOfWeek
            rule == "monthly" -> {
                val targetDay = eventStart.dayOfMonth
                val lastDayOfMonth = date.lengthOfMonth()
                date.dayOfMonth == targetDay.coerceAtMost(lastDayOfMonth)
            }
            rule == "yearly" -> {
                date.monthValue == eventStart.monthValue &&
                    date.dayOfMonth == eventStart.dayOfMonth.coerceAtMost(date.lengthOfMonth())
            }
            rule.startsWith("custom:") -> {
                val days = rule.removePrefix("custom:").split(",").mapNotNull { it.trim().toIntOrNull() }
                date.dayOfWeek.value in days
            }
            else -> date == eventStart
        }
    }
}

// ── 兼容旧代码的别名 ──
object EventReminderWorker {
    fun schedule(
        context: Context,
        eventId: Long,
        title: String,
        description: String,
        date: LocalDate,
        time: LocalTime?,
        reminderMinutes: Int,
    ) = EventReminderScheduler.schedule(context, eventId, title, description, date, time, reminderMinutes)

    fun cancel(context: Context, eventId: Long) = EventReminderScheduler.cancel(context, eventId)
}
