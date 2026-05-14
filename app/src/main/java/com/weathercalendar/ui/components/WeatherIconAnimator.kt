package com.weathercalendar.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.weathercalendar.data.model.WeatherCondition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 天气图标微动画 — 64dp Canvas 动画，轻量无限循环。
 *
 * 设计原则：
 * - 仅用于 CurrentWeatherCard 的主图标
 * - 粒子数严格限制（≤5）
 * - 自动跟随 Compose 生命周期暂停/恢复
 * - 尊重系统 Reduced Motion 设置
 */
@Composable
fun WeatherIconAnimator(
    condition: WeatherCondition,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // 检查系统 Reduced Motion 设置
    val reducedMotion = remember {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    val shouldAnimate = enabled && !reducedMotion

    if (!shouldAnimate) {
        // 静态图标
        StaticWeatherIcon(condition = condition, modifier = modifier.size(64.dp))
        return
    }

    when (condition) {
        WeatherCondition.SUNNY -> SunnyIconAnimation(modifier.size(64.dp))
        WeatherCondition.PARTLY_CLOUDY, WeatherCondition.CLOUDY -> CloudyIconAnimation(modifier.size(64.dp))
        WeatherCondition.RAINY, WeatherCondition.DRIZZLE -> RainyIconAnimation(modifier.size(64.dp))
        WeatherCondition.SNOWY -> SnowyIconAnimation(modifier.size(64.dp))
        WeatherCondition.STORMY -> StormyIconAnimation(modifier.size(64.dp))
        WeatherCondition.FOGGY -> FoggyIconAnimation(modifier.size(64.dp))
    }
}

// ─────────────────────────────────────────────
// 静态图标（降级/Reduced Motion 时使用）
// ─────────────────────────────────────────────

@Composable
private fun StaticWeatherIcon(condition: WeatherCondition, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        when (condition) {
            WeatherCondition.SUNNY -> drawSunStatic(cx, cy)
            WeatherCondition.PARTLY_CLOUDY, WeatherCondition.CLOUDY -> drawCloudStatic(cx, cy)
            WeatherCondition.RAINY, WeatherCondition.DRIZZLE -> drawRainStatic(cx, cy)
            WeatherCondition.SNOWY -> drawSnowStatic(cx, cy)
            WeatherCondition.STORMY -> drawStormStatic(cx, cy)
            WeatherCondition.FOGGY -> drawFogStatic(cx, cy)
        }
    }
}

// ─────────────────────────────────────────────
// ☀️ 晴天：光芒脉冲
// ─────────────────────────────────────────────

@Composable
private fun SunnyIconAnimation(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "sun_icon")
    val rayAlpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ray_alpha",
    )
    val rayScale by transition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse),
        label = "ray_scale",
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.width * 0.18f

        // 光芒
        val rayLength = size.width * 0.15f * rayScale
        for (i in 0 until 8) {
            val angle = (i * 45f) * (PI / 180f).toFloat()
            val startR = radius + 4f
            val endR = startR + rayLength
            drawLine(
                color = Color(0xFFFFD54F).copy(alpha = rayAlpha),
                start = Offset(cx + cos(angle) * startR, cy + sin(angle) * startR),
                end = Offset(cx + cos(angle) * endR, cy + sin(angle) * endR),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }

        // 太阳核心
        drawCircle(Color(0xFFFFD54F), radius = radius, center = Offset(cx, cy))
        drawCircle(Color(0xFFFFF176).copy(alpha = 0.6f), radius = radius * 0.6f, center = Offset(cx, cy))
    }
}

// ─────────────────────────────────────────────
// ☁️ 多云：云朵水平漂移
// ─────────────────────────────────────────────

@Composable
private fun CloudyIconAnimation(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "cloud_icon")
    val drift by transition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        // 主云
        drawCircle(Color.White.copy(alpha = 0.85f), radius = 14f, center = Offset(cx + drift, cy))
        drawCircle(Color.White.copy(alpha = 0.85f), radius = 11f, center = Offset(cx - 10f + drift, cy + 3f))
        drawCircle(Color.White.copy(alpha = 0.85f), radius = 10f, center = Offset(cx + 10f + drift, cy + 3f))
        // 次云（反向漂移）
        drawCircle(Color.White.copy(alpha = 0.5f), radius = 9f, center = Offset(cx - 5f - drift * 0.5f, cy - 10f))
        drawCircle(Color.White.copy(alpha = 0.5f), radius = 7f, center = Offset(cx + 8f - drift * 0.5f, cy - 8f))
    }
}

// ─────────────────────────────────────────────
// 🌧 雨天：雨滴下落
// ─────────────────────────────────────────────

@Composable
private fun RainyIconAnimation(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "rain_icon")
    val drop1Y by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "d1",
    )
    val drop2Y by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "d2",
    )
    val drop3Y by transition.animateFloat(
        initialValue = 0.6f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "d3",
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        // 云
        drawCircle(Color(0xFFB0BEC5), radius = 12f, center = Offset(cx, cy - 8f))
        drawCircle(Color(0xFFB0BEC5), radius = 9f, center = Offset(cx - 10f, cy - 5f))
        drawCircle(Color(0xFFB0BEC5), radius = 9f, center = Offset(cx + 10f, cy - 5f))

        // 雨滴
        val dropZone = size.height * 0.35f
        val startY = cy + 5f
        listOf(
            Pair(cx - 8f, drop1Y),
            Pair(cx, drop2Y),
            Pair(cx + 8f, drop3Y),
        ).forEach { (x, progress) ->
            val y = startY + (progress % 1f) * dropZone
            val alpha = 1f - (progress % 1f)
            drawLine(
                color = Color(0xFF4FC3F7).copy(alpha = alpha * 0.8f),
                start = Offset(x, y),
                end = Offset(x, y + 6f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

// ─────────────────────────────────────────────
// 🌨 雪天：雪花飘落 + 横摆
// ─────────────────────────────────────────────

@Composable
private fun SnowyIconAnimation(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "snow_icon")
    val fall by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "fall",
    )
    val sway by transition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "sway",
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        // 云
        drawCircle(Color(0xFFCFD8DC), radius = 12f, center = Offset(cx, cy - 10f))
        drawCircle(Color(0xFFCFD8DC), radius = 9f, center = Offset(cx - 10f, cy - 7f))
        drawCircle(Color(0xFFCFD8DC), radius = 9f, center = Offset(cx + 10f, cy - 7f))

        // 雪花
        val dropZone = size.height * 0.35f
        val startY = cy + 2f
        val offsets = listOf(-10f, -2f, 6f, 12f)
        offsets.forEachIndexed { i, xOff ->
            val phase = (fall + i * 0.25f) % 1f
            val y = startY + phase * dropZone
            val x = cx + xOff + sway * (if (i % 2 == 0) 1f else -1f)
            val alpha = 1f - phase * 0.5f
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.8f),
                radius = 2.5f,
                center = Offset(x, y),
            )
        }
    }
}

// ─────────────────────────────────────────────
// ⛈ 雷暴：闪电闪烁
// ─────────────────────────────────────────────

@Composable
private fun StormyIconAnimation(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "storm_icon")
    val flash by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "flash",
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        // 暗云
        drawCircle(Color(0xFF546E7A), radius = 13f, center = Offset(cx, cy - 8f))
        drawCircle(Color(0xFF546E7A), radius = 10f, center = Offset(cx - 10f, cy - 5f))
        drawCircle(Color(0xFF546E7A), radius = 10f, center = Offset(cx + 10f, cy - 5f))

        // 闪电（在特定时间段闪烁）
        val flashPhase = flash % 1f
        val showFlash = flashPhase in 0.6f..0.65f || flashPhase in 0.7f..0.73f
        if (showFlash) {
            val path = Path().apply {
                moveTo(cx + 2f, cy + 2f)
                lineTo(cx - 3f, cy + 12f)
                lineTo(cx + 1f, cy + 12f)
                lineTo(cx - 4f, cy + 24f)
            }
            drawPath(
                path = path,
                color = Color(0xFFFFD54F),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round),
            )
        }
    }
}

// ─────────────────────────────────────────────
// 🌫 雾天：薄雾层流动
// ─────────────────────────────────────────────

@Composable
private fun FoggyIconAnimation(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "fog_icon")
    val drift by transition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "fog_drift",
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        // 三层雾
        listOf(
            Triple(cy - 6f, 22f, 0.4f),
            Triple(cy + 2f, 18f, 0.3f),
            Triple(cy + 10f, 20f, 0.25f),
        ).forEachIndexed { i, (y, width, alpha) ->
            val xOff = drift * (if (i % 2 == 0) 1f else -0.7f)
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(cx - width + xOff, y),
                end = Offset(cx + width + xOff, y),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
}

// ─────────────────────────────────────────────
// 静态绘制辅助
// ─────────────────────────────────────────────

private fun DrawScope.drawSunStatic(cx: Float, cy: Float) {
    val radius = size.width * 0.18f
    for (i in 0 until 8) {
        val angle = (i * 45f) * (PI / 180f).toFloat()
        val startR = radius + 4f
        val endR = startR + size.width * 0.12f
        drawLine(
            color = Color(0xFFFFD54F).copy(alpha = 0.7f),
            start = Offset(cx + cos(angle) * startR, cy + sin(angle) * startR),
            end = Offset(cx + cos(angle) * endR, cy + sin(angle) * endR),
            strokeWidth = 3f, cap = StrokeCap.Round,
        )
    }
    drawCircle(Color(0xFFFFD54F), radius = radius, center = Offset(cx, cy))
}

private fun DrawScope.drawCloudStatic(cx: Float, cy: Float) {
    drawCircle(Color.White.copy(alpha = 0.85f), radius = 14f, center = Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = 11f, center = Offset(cx - 10f, cy + 3f))
    drawCircle(Color.White.copy(alpha = 0.85f), radius = 10f, center = Offset(cx + 10f, cy + 3f))
}

private fun DrawScope.drawRainStatic(cx: Float, cy: Float) {
    drawCircle(Color(0xFFB0BEC5), radius = 12f, center = Offset(cx, cy - 8f))
    drawCircle(Color(0xFFB0BEC5), radius = 9f, center = Offset(cx - 10f, cy - 5f))
    drawCircle(Color(0xFFB0BEC5), radius = 9f, center = Offset(cx + 10f, cy - 5f))
    listOf(-8f, 0f, 8f).forEach { xOff ->
        drawLine(Color(0xFF4FC3F7).copy(alpha = 0.6f), Offset(cx + xOff, cy + 5f), Offset(cx + xOff, cy + 11f), 2f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawSnowStatic(cx: Float, cy: Float) {
    drawCircle(Color(0xFFCFD8DC), radius = 12f, center = Offset(cx, cy - 10f))
    drawCircle(Color(0xFFCFD8DC), radius = 9f, center = Offset(cx - 10f, cy - 7f))
    drawCircle(Color(0xFFCFD8DC), radius = 9f, center = Offset(cx + 10f, cy - 7f))
    listOf(-8f, 0f, 8f).forEach { xOff ->
        drawCircle(Color.White.copy(alpha = 0.7f), radius = 2.5f, center = Offset(cx + xOff, cy + 8f))
    }
}

private fun DrawScope.drawStormStatic(cx: Float, cy: Float) {
    drawCircle(Color(0xFF546E7A), radius = 13f, center = Offset(cx, cy - 8f))
    drawCircle(Color(0xFF546E7A), radius = 10f, center = Offset(cx - 10f, cy - 5f))
    drawCircle(Color(0xFF546E7A), radius = 10f, center = Offset(cx + 10f, cy - 5f))
    val path = Path().apply {
        moveTo(cx + 2f, cy + 2f); lineTo(cx - 3f, cy + 12f)
        lineTo(cx + 1f, cy + 12f); lineTo(cx - 4f, cy + 24f)
    }
    drawPath(path, Color(0xFFFFD54F), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
}

private fun DrawScope.drawFogStatic(cx: Float, cy: Float) {
    listOf(cy - 6f, cy + 2f, cy + 10f).forEachIndexed { i, y ->
        val width = listOf(22f, 18f, 20f)[i]
        val alpha = listOf(0.4f, 0.3f, 0.25f)[i]
        drawLine(Color.White.copy(alpha = alpha), Offset(cx - width, y), Offset(cx + width, y), 4f, cap = StrokeCap.Round)
    }
}
