package com.weathercalendar

import android.app.Application
import com.weathercalendar.notification.EventReminderScheduler
import com.weathercalendar.notification.WeatherNotificationWorker
import com.weathercalendar.widget.WeatherWidgetWorker
import com.weathercalendar.widget.WidgetRefreshWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeatherCalendarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Widget 定时刷新（每小时）
        WeatherWidgetWorker.enqueue(this)
        // Widget 自动刷新（每小时，带网络约束）
        WidgetRefreshWorker.enqueue(this)
        // 天气通知定时检查（每 6 小时）
        WeatherNotificationWorker.enqueue(this)
        // 恢复所有日程提醒（含重复事件未来 7 天）
        EventReminderScheduler.rescheduleAll(this)
    }
}
