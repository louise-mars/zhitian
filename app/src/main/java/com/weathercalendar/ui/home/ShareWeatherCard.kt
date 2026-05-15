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
        putExtra(Intent.EXTRA_SUBJECT, "知天天气 · $cityName")
        // 允许微信等 App 识别分享内容
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "分享到"))
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
    val width = 720
    val height = 960
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
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
    textPaint.textSize = 96f
    textPaint.textAlign = Paint.Align.LEFT
    canvas.drawText(cityName, 50f, 100f, textPaint)

    // Date
    textPaint.textSize = 64f
    textPaint.alpha = 180
    canvas.drawText("$dateText · $lunarText", 50f, 160f, textPaint)

    // Weather emoji
    textPaint.textSize = 200f
    textPaint.alpha = 255
    textPaint.textAlign = Paint.Align.CENTER
    canvas.drawText(currentWeather.condition.icon, width / 2f, 380f, textPaint)

    // Temperature
    textPaint.textSize = 280f
    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("${currentWeather.temperature}°", width / 2f, 620f, textPaint)

    // Condition label
    textPaint.textSize = 72f
    canvas.drawText(currentWeather.condition.label, width / 2f, 700f, textPaint)

    // Feels like
    textPaint.textSize = 56f
    textPaint.alpha = 180
    canvas.drawText("体感 ${currentWeather.feelsLike}°", width / 2f, 760f, textPaint)

    // Branding
    textPaint.textSize = 48f
    textPaint.alpha = 128
    textPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText("— 知天", width - 50f, height - 50f, textPaint)

    // Save to cache/share/ subdirectory (restricted by FileProvider)
    val shareDir = File(context.cacheDir, "share").also { it.mkdirs() }
    val file = File(shareDir, "share_weather.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
    bitmap.recycle()

    // Share via FileProvider — 使用 image/* 类型确保微信/微博等社交 App 能接收
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "知天天气 · $cityName")
        putExtra(Intent.EXTRA_TEXT, "${currentWeather.condition.icon} $cityName ${currentWeather.temperature}° ${currentWeather.condition.label} — 知天")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "分享到"))
}
