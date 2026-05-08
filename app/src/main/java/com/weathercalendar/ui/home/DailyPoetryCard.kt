package com.weathercalendar.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * 每日诗词卡片 — 根据天气和季节展示一句古诗词。
 */
@Composable
fun DailyPoetryCard(
    date: LocalDate,
    condition: WeatherCondition,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val poetry = DailyPoetry.getPoetry(date, condition)

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "「${poetry.verse}」",
            fontSize = 16.sp,
            color = Color(0xFFE8D5B7),  // 暖金色，古典感
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Serif,
            lineHeight = 26.sp,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "— ${poetry.source}",
            fontSize = 13.sp,
            color = Color(0xFFE8D5B7).copy(alpha = 0.6f),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Light,
        )
    }
}
