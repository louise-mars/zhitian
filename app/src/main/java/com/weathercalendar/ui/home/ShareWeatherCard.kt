package com.weathercalendar.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.weathercalendar.data.model.CurrentWeather
import java.io.File

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
        append("— 知天")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享天气"))
}

/**
 * 生成天气卡片图片并分享。
 */
fun shareWeatherImage(
    context: Context,
    cityName: String,
    dateText: String,
    currentWeather: CurrentWeather,
    lunarText: String,
) {
    val width = 1080
    val height = 1440
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw gradient background
    val gradient = android.graphics.LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        intArrayOf(0xFF4FACFE.toInt(), 0xFF00F2FE.toInt()),
        null, android.graphics.Shader.TileMode.CLAMP
    )
    val bgPaint = Paint().apply { shader = gradient }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Draw text elements with Paint
    val textPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
    }

    // City name
    textPaint.textSize = 48f * 3
    textPaint.textAlign = Paint.Align.LEFT
    canvas.drawText(cityName, 80f, 150f, textPaint)

    // Date
    textPaint.textSize = 32f * 3
    textPaint.alpha = 180
    canvas.drawText("$dateText · $lunarText", 80f, 230f, textPaint)

    // Weather emoji
    textPaint.textSize = 120f * 3
    textPaint.alpha = 255
    textPaint.textAlign = Paint.Align.CENTER
    canvas.drawText(currentWeather.condition.icon, width / 2f, 550f, textPaint)

    // Temperature
    textPaint.textSize = 144f * 3
    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("${currentWeather.temperature}°", width / 2f, 900f, textPaint)

    // Condition label
    textPaint.textSize = 36f * 3
    canvas.drawText(currentWeather.condition.label, width / 2f, 1020f, textPaint)

    // Feels like
    textPaint.textSize = 28f * 3
    textPaint.alpha = 180
    canvas.drawText("体感 ${currentWeather.feelsLike}°", width / 2f, 1100f, textPaint)

    // Branding
    textPaint.textSize = 24f * 3
    textPaint.alpha = 128
    textPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText("— 知天", width - 80f, height - 80f, textPaint)

    // Save to cache
    val file = File(context.cacheDir, "share_weather.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
    bitmap.recycle()

    // Share via FileProvider
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享天气"))
}
