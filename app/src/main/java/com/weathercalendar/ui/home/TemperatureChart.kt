package com.weathercalendar.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.weathercalendar.data.model.DayInfo

/**
 * 温度趋势折线图 — 最高温和最低温双线，支持横向滚动和点击 tooltip。
 */
@Composable
fun TemperatureChart(
    days: List<DayInfo>,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    // 单天数据：显示简单文字而非空图表
    if (days.size == 1) {
        val day = days[0]
        androidx.compose.material3.Text(
            text = "${day.weather.condition.icon} ${day.weather.tempMin}° ~ ${day.weather.tempMax}°",
            color = textColor,
            fontSize = 16.sp,
            modifier = modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        return
    }

    var selectedIndex by remember { mutableStateOf(-1) }

    Box(modifier = modifier.padding(horizontal = 16.dp)) {
        ChartCanvas(
            days = days,
            textColor = textColor,
            selectedIndex = selectedIndex,
            onTap = { index -> selectedIndex = if (selectedIndex == index) -1 else index },
        )

        // Tooltip popup
        if (selectedIndex in days.indices) {
            val day = days[selectedIndex]
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, 16),
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "${day.date.monthValue}/${day.date.dayOfMonth} ${day.weather.condition.icon} ${day.weather.tempMax}°/${day.weather.tempMin}°",
                        color = Color.White,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartCanvas(
    days: List<DayInfo>,
    textColor: Color,
    selectedIndex: Int,
    onTap: (Int) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val maxTemp = days.maxOf { it.weather.tempMax }
    val minTemp = days.minOf { it.weather.tempMin }
    val tempRange = (maxTemp - minTemp).coerceAtLeast(1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .pointerInput(days.size) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val stepX = w / (days.size - 1).coerceAtLeast(1)
                    val tappedIndex = ((offset.x + stepX / 2) / stepX).toInt()
                        .coerceIn(0, days.lastIndex)
                    onTap(tappedIndex)
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val paddingTop = 40f   // space for weather emoji
        val paddingBottom = 36f // space for date labels
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
            val highDotRadius = if (index == selectedIndex) 8f else 5f
            val lowDotRadius = if (index == selectedIndex) 8f else 5f
            drawCircle(
                color = Color(0xFFFFD54F),  // 金黄色
                radius = highDotRadius,
                center = Offset(x, highY),
            )
            drawCircle(
                color = Color(0xFFCE93D8),  // 淡紫色
                radius = lowDotRadius,
                center = Offset(x, lowY),
            )

            // 天气 emoji 图标（高温线上方）
            val emojiStyle = TextStyle(fontSize = 12.sp)
            val emojiResult = textMeasurer.measure(day.weather.condition.icon, emojiStyle)
            drawText(
                emojiResult,
                topLeft = Offset(x - emojiResult.size.width / 2f, highY - 32f),
            )

            // 温度标签
            val highLabel = "${day.weather.tempMax}°"
            val lowLabel = "${day.weather.tempMin}°"
            val labelStyle = TextStyle(color = textColor.copy(alpha = 0.6f), fontSize = 9.sp)

            val highResult = textMeasurer.measure(highLabel, labelStyle)
            drawText(
                highResult,
                topLeft = Offset(x - highResult.size.width / 2f, highY - 16f),
            )

            val lowResult = textMeasurer.measure(lowLabel, labelStyle)
            drawText(
                lowResult,
                topLeft = Offset(x - lowResult.size.width / 2f, lowY + 6f),
            )

            // 日期标签（底部）
            val dateLabel = "${day.date.dayOfMonth}日"
            val dateStyle = TextStyle(color = textColor.copy(alpha = 0.5f), fontSize = 10.sp)
            val dateResult = textMeasurer.measure(dateLabel, dateStyle)
            drawText(
                dateResult,
                topLeft = Offset(x - dateResult.size.width / 2f, h - paddingBottom + 8f),
            )
        }

        // 画折线
        drawPath(
            path = highPath,
            color = Color(0xFFFFD54F),  // 金黄色 — 高温线
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = lowPath,
            color = Color(0xFFCE93D8),  // 淡紫色 — 低温线
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
