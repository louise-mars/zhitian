package com.weathercalendar.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.util.DailyPoetry
import java.time.LocalDate

/**
 * 每日诗词卡片 — 点击展开完整诗词。
 */
@Composable
fun DailyPoetryCard(
    date: LocalDate,
    condition: WeatherCondition,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val poetry = DailyPoetry.getPoetry(date, condition)
    var expanded by remember { mutableStateOf(false) }
    val hasFullText = poetry.fullText.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (hasFullText) Modifier.clickable { expanded = !expanded } else Modifier)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 精华一句
        Text(
            text = "「${poetry.verse}」",
            fontSize = 18.sp,
            color = Color(0xFFF5E6C8),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            lineHeight = 30.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "— ${poetry.source}",
            fontSize = 14.sp,
            color = Color(0xFFD4A574),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
        )

        // 展开提示
        if (hasFullText && !expanded) {
            Text(
                text = "点击查看全文 ▾",
                fontSize = 12.sp,
                color = Color(0xFFD4A574).copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // 完整诗词（展开时显示）
        AnimatedVisibility(
            visible = expanded && hasFullText,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalDivider(
                    color = Color(0xFFD4A574).copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
                Spacer(Modifier.height(12.dp))

                // 诗名
                Text(
                    text = poetry.source.substringBefore("·"),
                    fontSize = 16.sp,
                    color = Color(0xFFF5E6C8),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                )
                // 作者
                Text(
                    text = poetry.source.substringAfter("·", ""),
                    fontSize = 13.sp,
                    color = Color(0xFFD4A574),
                    fontFamily = FontFamily.Serif,
                )
                Spacer(Modifier.height(10.dp))

                // 全文
                Text(
                    text = poetry.fullText,
                    fontSize = 16.sp,
                    color = Color(0xFFF5E6C8).copy(alpha = 0.95f),
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                    letterSpacing = 0.6.sp,
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "▴ 收起",
                    fontSize = 12.sp,
                    color = Color(0xFFD4A574).copy(alpha = 0.6f),
                )
            }
        }
    }
}
