package com.weathercalendar.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.CurrentWeather
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.ui.theme.WeatherColors

/**
 * 生成天气分享文本（不需要截图，直接文字分享更轻量）。
 */
fun shareWeatherText(
    context: Context,
    cityName: String,
    dateText: String,
    currentWeather: CurrentWeather,
    lunarText: String,
) {
    val text = buildString {
        append("${currentWeather.condition.icon} $cityName · $dateText")
        if (lunarText.isNotEmpty()) append(" · $lunarText")
        appendLine()
        append("${currentWeather.temperature}° ${currentWeather.condition.label}")
        append(" | 体感 ${currentWeather.feelsLike}°")
        appendLine()
        append(currentWeather.condition.tip)
        appendLine()
        append("— 天气日历")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享天气"))
}
