package com.weathercalendar.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.DayInfo

/**
 * 7 天温度趋势折线图 — 最高温和最低温双线。
 */
@Composable
fun TemperatureChart(
    days: List<DayInfo>,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    if (days.size < 2) return

    val textMeasurer = rememberTextMeasurer()
    val maxTemp = days.maxOf { it.weather.tempMax }
    val minTemp = days.minOf { it.weather.tempMin }
    val tempRange = (maxTemp - minTemp).coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        val w = size.width
        val h = size.height
        val paddingTop = 20f
        val paddingBottom = 24f
        val chartHeight = h - paddingTop - paddingBottom
        val stepX = w / (days.size - 1).coerceAtLeast(1)

        fun tempToY(temp: Int): Float {
            val ratio = (temp - minTemp).toFloat() / tempRange
            return paddingTop + chartHeight * (1f - ratio)
        }

        // 最高温折线
        val highPath = Path()
        val lowPath = Path()

        days.forEachIndexed { index, day ->
            val x = index * stepX
            val highY = tempToY(day.weather.tempMax)
            val lowY = tempToY(day.weather.tempMin)

            if (index == 0) {
                highPath.moveTo(x, highY)
                lowPath.moveTo(x, lowY)
            } else {
                highPath.lineTo(x, highY)
                lowPath.lineTo(x, lowY)
            }

            // 数据点
            drawCircle(
                color = Color(0xFFFF8A65),
                radius = 3.5f,
                center = Offset(x, highY),
            )
            drawCircle(
                color = Color(0xFF4FC3F7),
                radius = 3.5f,
                center = Offset(x, lowY),
            )

            // 温度标签（首尾 + 中间隔一个显示）
            if (index == 0 || index == days.lastIndex || index % 2 == 0) {
                val highLabel = "${day.weather.tempMax}°"
                val lowLabel = "${day.weather.tempMin}°"
                val style = TextStyle(color = textColor.copy(alpha = 0.6f), fontSize = 9.sp)

                val highResult = textMeasurer.measure(highLabel, style)
                drawText(
                    highResult,
                    topLeft = Offset(x - highResult.size.width / 2f, highY - 18f),
                )

                val lowResult = textMeasurer.measure(lowLabel, style)
                drawText(
                    lowResult,
                    topLeft = Offset(x - lowResult.size.width / 2f, lowY + 6f),
                )
            }
        }

        // 画折线
        drawPath(
            path = highPath,
            color = Color(0xFFFF8A65),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = lowPath,
            color = Color(0xFF4FC3F7),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
