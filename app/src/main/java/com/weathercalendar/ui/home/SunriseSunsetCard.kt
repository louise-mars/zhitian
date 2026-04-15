package com.weathercalendar.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.ui.components.GlassCard
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 日出日落弧线图。
 * 半圆弧线标注日出/日落时间，黄色圆点标注当前太阳位置。
 */
@Composable
fun SunriseSunsetCard(
    sunrise: String,
    sunset: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    if (sunrise.isBlank() || sunset.isBlank()) return

    val sunriseTime = try { LocalTime.parse(sunrise) } catch (_: Exception) { return }
    val sunsetTime = try { LocalTime.parse(sunset) } catch (_: Exception) { return }
    val now = LocalTime.now()

    val dayMinutes = (sunsetTime.toSecondOfDay() - sunriseTime.toSecondOfDay()) / 60f
    val currentMinutes = (now.toSecondOfDay() - sunriseTime.toSecondOfDay()) / 60f
    val progress = (currentMinutes / dayMinutes).coerceIn(0f, 1f)
    val isDaytime = now.isAfter(sunriseTime) && now.isBefore(sunsetTime)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = 0.12f,
        cornerRadius = 20.dp,
        elevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "日出日落",
                color = textColor.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))

            // 弧线图
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            ) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val baseY = h - 8f
                val radiusX = w * 0.42f
                val radiusY = h * 0.85f

                // 画半圆弧线
                val arcPath = Path()
                val steps = 50
                for (i in 0..steps) {
                    val angle = PI * (1.0 - i.toDouble() / steps)
                    val x = centerX + (radiusX * cos(angle)).toFloat()
                    val y = baseY - (radiusY * sin(angle)).toFloat()
                    if (i == 0) arcPath.moveTo(x, y) else arcPath.lineTo(x, y)
                }
                drawPath(
                    path = arcPath,
                    color = Color(0xFFFFD54F).copy(alpha = 0.3f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round),
                )

                // 地平线
                drawLine(
                    color = textColor.copy(alpha = 0.15f),
                    start = Offset(centerX - radiusX - 10f, baseY),
                    end = Offset(centerX + radiusX + 10f, baseY),
                    strokeWidth = 1f,
                )

                // 当前太阳位置
                if (isDaytime) {
                    val sunAngle = PI * (1.0 - progress.toDouble())
                    val sunX = centerX + (radiusX * cos(sunAngle)).toFloat()
                    val sunY = baseY - (radiusY * sin(sunAngle)).toFloat()

                    // 光晕
                    drawCircle(
                        color = Color(0xFFFFD54F).copy(alpha = 0.2f),
                        radius = 14f,
                        center = Offset(sunX, sunY),
                    )
                    // 太阳点
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 6f,
                        center = Offset(sunX, sunY),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // 日出/日落时间
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🌅 $sunrise",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "🌇 $sunset",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}
