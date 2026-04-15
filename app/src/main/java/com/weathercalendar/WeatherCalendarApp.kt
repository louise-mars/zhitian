package com.weathercalendar

import android.app.Application
import com.weathercalendar.notification.WeatherNotificationWorker
import com.weathercalendar.widget.WeatherWidgetWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WeatherCalendarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Widget 定时刷新（每小时）
        WeatherWidgetWorker.enqueue(this)
        // 天气通知定时检查（每 6 小时）
        WeatherNotificationWorker.enqueue(this)
    }
}
