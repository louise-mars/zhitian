package com.weathercalendar.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.WeatherWarning
import com.weathercalendar.ui.components.GlassCard

/**
 * 天气预警卡片 — 红/橙/黄/蓝色预警。
 */
@Composable
fun WeatherWarningCard(
    warnings: List<WeatherWarning>,
    modifier: Modifier = Modifier,
) {
    if (warnings.isEmpty()) return

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = 0.15f,
        cornerRadius = 20.dp,
        elevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            warnings.forEachIndexed { index, warning ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${warning.typeName}${warning.level}预警",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (warning.level) {
                                "红色" -> Color(0xFFE53935)
                                "橙色" -> Color(0xFFFF9800)
                                "黄色" -> Color(0xFFFFEB3B)
                                else -> Color(0xFF42A5F5)
                            },
                        )
                        Text(
                            text = warning.text.take(80) + if (warning.text.length > 80) "..." else "",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 2,
                        )
                    }
                }
                if (index < warnings.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
