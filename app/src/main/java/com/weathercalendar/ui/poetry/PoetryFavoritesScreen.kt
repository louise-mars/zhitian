package com.weathercalendar.ui.poetry

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathercalendar.data.repository.FavoritePoetry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 我的诗集 — 诗词收藏夹页面。
 *
 * 设计：深色背景 + 暖金色文字，与诗词卡片风格一致。
 * 点击展开全文，长按/点击删除图标取消收藏。
 */
@Composable
fun PoetryFavoritesScreen(
    favorites: List<FavoritePoetry>,
    onBack: () -> Unit = {},
    onDelete: (Long) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A2030), Color(0xFF0F1520))
                )
            )
            .statusBarsPadding(),
    ) {
        // 顶部导航
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "我的诗集",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF5E6C8),
                fontFamily = FontFamily.Serif,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${favorites.size} 首",
                fontSize = 14.sp,
                color = Color(0xFFD4A574),
            )
            Spacer(Modifier.width(16.dp))
        }

        if (favorites.isEmpty()) {
            // 空状态
            Column(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(80.dp))
                Text("📖", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "还没有收藏的诗词",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "在首页点击诗词旁的 🤍 即可收藏",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.3f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            ) {
                items(favorites, key = { it.id }) { poetry ->
                    PoetryFavoriteItem(
                        poetry = poetry,
                        onDelete = { onDelete(poetry.id) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                // 底部留白
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun PoetryFavoriteItem(
    poetry: FavoritePoetry,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val dateStr = try {
        val date = Instant.ofEpochMilli(poetry.collectedAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        date.format(DateTimeFormatter.ofPattern("M月d日收藏"))
    } catch (_: Exception) { "" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable { if (poetry.fullText.isNotBlank()) expanded = !expanded }
            .padding(16.dp)
            .animateContentSize(),
    ) {
        // 诗句
        Text(
            text = "「${poetry.verse}」",
            fontSize = 16.sp,
            color = Color(0xFFF5E6C8),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))

        // 出处 + 删除按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "— ${poetry.source}",
                    fontSize = 13.sp,
                    color = Color(0xFFD4A574),
                    fontFamily = FontFamily.Serif,
                )
                if (dateStr.isNotEmpty()) {
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.3f),
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "取消收藏",
                    tint = Color.White.copy(alpha = 0.4f),
                )
            }
        }

        // 展开全文
        if (expanded && poetry.fullText.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFD4A574).copy(alpha = 0.2f))
            Spacer(Modifier.height(10.dp))
            Text(
                text = poetry.fullText,
                fontSize = 15.sp,
                color = Color(0xFFF5E6C8).copy(alpha = 0.9f),
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
