package com.weathercalendar.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 设备重启后恢复所有日程提醒闹钟。
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("BootReceiver", "设备重启，恢复日程提醒...")
        EventReminderScheduler.rescheduleAll(context)
    }
}
