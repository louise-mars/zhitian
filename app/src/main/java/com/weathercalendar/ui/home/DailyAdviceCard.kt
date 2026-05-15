package com.weathercalendar.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.domain.advice.DailyAdviceEngine
import com.weathercalendar.ui.components.GlassCard

/**
 * 今日宜忌卡片 — 基于天气数据生成的生活建议。
 */
@Composable
fun DailyAdviceCard(
    advice: DailyAdviceEngine.DailyAdvice,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        alpha = 0.12f,
        cornerRadius = 16.dp,
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 宜
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "宜",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF66BB6A),
                )
                Spacer(Modifier.height(6.dp))
                advice.suitable.forEach { item ->
                    Text(
                        item,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 20.sp,
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // 忌
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "忌",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF5350),
                )
                Spacer(Modifier.height(6.dp))
                advice.unsuitable.forEach { item ->
                    Text(
                        item,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}
