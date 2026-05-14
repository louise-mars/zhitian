package com.weathercalendar.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.weathercalendar.MainActivity
import com.weathercalendar.data.model.WeatherCondition
import com.weathercalendar.util.DailyPoetry
import java.time.LocalDate

/**
 * 诗词桌面小组件 — 每日展示一句古诗词。
 *
 * 设计：暖金色衬线文字 + 半透明深色背景，与 App 内诗词卡片风格一致。
 * 每日自动刷新，点击打开 App。
 */
class PoetryWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadPoetryData(context)
        provideContent {
            GlanceTheme {
                PoetryWidgetContent(data)
            }
        }
    }

    private fun loadPoetryData(context: Context): PoetryWidgetData {
        val sp = context.getSharedPreferences("poetry_widget", Context.MODE_PRIVATE)
        val today = LocalDate.now()

        // 读取缓存的天气条件
        val conditionName = sp.getString("weather_condition", null)
        val condition = try {
            conditionName?.let { WeatherCondition.valueOf(it) }
        } catch (_: Exception) { null } ?: WeatherCondition.SUNNY

        // 获取今日诗词
        val poetry = DailyPoetry.getPoetry(today, condition)

        // 缓存成功加载的诗词作为 fallback
        if (poetry.verse.isNotBlank()) {
            sp.edit()
                .putString("cached_verse", poetry.verse)
                .putString("cached_source", poetry.source)
                .putString("cached_date", today.toString())
                .apply()
        }

        val verse = poetry.verse.ifBlank {
            sp.getString("cached_verse", "春风又绿江南岸") ?: "春风又绿江南岸"
        }
        val source = poetry.source.ifBlank {
            sp.getString("cached_source", "泊船瓜洲·王安石") ?: "泊船瓜洲·王安石"
        }

        return PoetryWidgetData(
            verse = verse,
            source = source,
            weatherIcon = condition.icon,
        )
    }
}

data class PoetryWidgetData(
    val verse: String,
    val source: String,
    val weatherIcon: String,
)

@Composable
private fun PoetryWidgetContent(data: PoetryWidgetData) {
    val goldColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF5E6C8))
    val sourceColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFFD4A574))
    val bgColor = androidx.compose.ui.graphics.Color(0xFF1A2030).copy(alpha = 0.85f)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
    ) {
        // 右上角天气图标
        Box(
            modifier = GlanceModifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd,
        ) {
            Text(
                text = data.weatherIcon,
                style = TextStyle(fontSize = 14.sp),
            )
        }

        // 诗词内容居中
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "「${data.verse}」",
                style = TextStyle(
                    color = goldColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = "— ${data.source}",
                style = TextStyle(
                    color = sourceColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * 诗词 Widget 广播接收器 — 系统回调入口。
 */
class PoetryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PoetryWidget()
}
