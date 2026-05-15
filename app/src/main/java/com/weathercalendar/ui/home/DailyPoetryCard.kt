package com.weathercalendar.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * 每日诗词卡片 — 支持左右滑动探索多首诗词，点击展开完整诗词，支持收藏。
 */
@Composable
fun DailyPoetryCard(
    date: LocalDate,
    condition: WeatherCondition,
    textColor: Color,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
) {
    val pageCount = 5
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val poetryDate = date.plusDays(page.toLong())
            val poetry = DailyPoetry.getPoetry(poetryDate, condition)
            PoetryPageContent(
                poetry = poetry,
                isFavorite = isFavorite && page == 0,
                onFavoriteToggle = if (page == 0) onFavoriteToggle else null,
            )
        }

        // Page indicator dots
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == pagerState.currentPage) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) Color(0xFFF5E6C8)
                            else Color(0xFFF5E6C8).copy(alpha = 0.3f)
                        ),
                )
            }
        }
    }
}

@Composable
private fun PoetryPageContent(
    poetry: DailyPoetry.Poetry,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val hasFullText = poetry.fullText.isNotBlank()

    Column(
        modifier = Modifier
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

        // 收藏按钮
        if (onFavoriteToggle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isFavorite) "❤️ 已收藏" else "🤍 收藏",
                fontSize = 11.sp,
                color = if (isFavorite) Color(0xFFE57373) else Color(0xFFD4A574).copy(alpha = 0.6f),
                modifier = Modifier.clickable { onFavoriteToggle() },
            )
        }

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
