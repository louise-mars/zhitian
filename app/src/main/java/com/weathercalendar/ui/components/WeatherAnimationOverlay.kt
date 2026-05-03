package com.weathercalendar.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.weathercalendar.data.model.WeatherCondition
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 天气动态背景叠加层 — Canvas 粒子动画（iOS 级）。
 *
 * 架构：WeatherCondition → 对应的 Effect 组合
 * 叠在渐变背景之上、内容之下。
 */
@Composable
fun WeatherAnimationOverlay(
    condition: WeatherCondition,
    isDay: Boolean = true,
    modifier: Modifier = Modifier,
) {
    when (condition) {
        WeatherCondition.RAINY, WeatherCondition.DRIZZLE ->
            RainEffect(
                modifier = modifier,
                intensity = if (condition == WeatherCondition.DRIZZLE) 50 else 100,
            )
        WeatherCondition.SNOWY ->
            SnowEffect(modifier = modifier)
        WeatherCondition.CLOUDY, WeatherCondition.PARTLY_CLOUDY ->
            CloudEffect(modifier = modifier)
        WeatherCondition.STORMY ->
            StormEffect(modifier = modifier)
        WeatherCondition.SUNNY ->
            if (isDay) SunLightEffect(modifier = modifier)
        WeatherCondition.FOGGY ->
            FogEffect(modifier = modifier)
    }
}

// ═════════════════════════════════════════════
// ☀️ 晴天：双层光晕呼吸 + 光线旋转
// ═════════════════════════════════════════════

@Composable
private fun SunLightEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sun")

    // 主光晕呼吸
    val glowAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mainGlow",
    )
    val glowRadius by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mainRadius",
    )

    // 次光晕（偏移，更柔和）
    val secondAlpha by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "secondGlow",
    )

    // 光线旋转角度
    val rayAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
        ),
        label = "rayRotation",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.12f

        // 主光晕
        val r1 = w * glowRadius
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFD54F).copy(alpha = glowAlpha),
                    Color(0xFFFFF176).copy(alpha = glowAlpha * 0.3f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = r1,
            ),
            radius = r1,
            center = Offset(cx, cy),
        )

        // 次光晕（偏下偏大）
        val r2 = w * 0.7f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = secondAlpha),
                    Color.Transparent,
                ),
                center = Offset(cx, cy + h * 0.05f),
                radius = r2,
            ),
            radius = r2,
            center = Offset(cx, cy + h * 0.05f),
        )

        // 旋转光线（8 条，更粗更亮）
        val rayCount = 8
        val rayLength = w * 0.5f
        for (i in 0 until rayCount) {
            val angle = Math.toRadians((rayAngle + i * 360.0 / rayCount).toDouble())
            val endX = cx + (rayLength * kotlin.math.cos(angle)).toFloat()
            val endY = cy + (rayLength * kotlin.math.sin(angle)).toFloat()
            drawLine(
                color = Color(0xFFFFD54F).copy(alpha = glowAlpha * 0.5f),
                start = Offset(cx, cy),
                end = Offset(endX, endY),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
}

// ═════════════════════════════════════════════
// ☁️ 多云：多层云视差漂移
// ═════════════════════════════════════════════

private data class CloudLayer(
    var x: Float,
    val y: Float,
    val speed: Float,       // 不同层速度不同 → 视差
    val width: Float,
    val height: Float,
    val alpha: Float,
    val cornerRatio: Float, // 圆角比例
)

@Composable
private fun CloudEffect(modifier: Modifier = Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }

    // 3 层云，速度不同产生视差
    val clouds = remember {
        listOf(
            // 远景层
            CloudLayer(x = -0.1f, y = 0.04f, speed = 0.00018f, width = 0.50f, height = 0.07f, alpha = 0.25f, cornerRatio = 0.5f),
            CloudLayer(x = 0.5f, y = 0.08f, speed = 0.00015f, width = 0.60f, height = 0.08f, alpha = 0.20f, cornerRatio = 0.5f),
            // 中景层
            CloudLayer(x = 0.1f, y = 0.16f, speed = 0.00035f, width = 0.40f, height = 0.06f, alpha = 0.35f, cornerRatio = 0.5f),
            CloudLayer(x = 0.7f, y = 0.22f, speed = 0.00030f, width = 0.35f, height = 0.055f, alpha = 0.30f, cornerRatio = 0.5f),
            // 近景层
            CloudLayer(x = -0.2f, y = 0.30f, speed = 0.00055f, width = 0.30f, height = 0.05f, alpha = 0.40f, cornerRatio = 0.5f),
            CloudLayer(x = 0.4f, y = 0.35f, speed = 0.00050f, width = 0.25f, height = 0.045f, alpha = 0.35f, cornerRatio = 0.5f),
            CloudLayer(x = 0.9f, y = 0.13f, speed = 0.00045f, width = 0.32f, height = 0.05f, alpha = 0.28f, cornerRatio = 0.5f),
        ).toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            clouds.forEach { cloud ->
                cloud.x += cloud.speed
                if (cloud.x > 1.2f) cloud.x = -cloud.width - 0.1f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        clouds.forEach { cloud ->
            drawOval(
                color = Color.White.copy(alpha = cloud.alpha),
                topLeft = Offset(cloud.x * w, cloud.y * h),
                size = Size(cloud.width * w, cloud.height * h),
            )
        }
    }
}

// ═════════════════════════════════════════════
// 🌧 雨天：多层雨滴 + 速度/透明度变化
// ═════════════════════════════════════════════

private data class RainDrop(
    var x: Float,
    var y: Float,
    val speed: Float,
    val length: Float,
    val alpha: Float,
    val angle: Float,
    val strokeWidth: Float,
)

@Composable
private fun RainEffect(modifier: Modifier = Modifier, intensity: Int = 100) {
    var frameTime by remember { mutableLongStateOf(0L) }

    val drops = remember {
        List(intensity) {
            createRainDrop()
        }.toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            drops.forEach { drop ->
                drop.y += drop.speed
                drop.x += drop.angle * drop.speed * 0.5f
                if (drop.y > 1.15f) {
                    drop.y = Random.nextFloat() * -0.2f - 0.05f
                    drop.x = Random.nextFloat()
                }
                if (drop.x > 1.1f) drop.x = -0.05f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drops.forEach { drop ->
            val startX = drop.x * w
            val startY = drop.y * h
            val endX = startX + drop.angle * drop.length * h
            val endY = startY + drop.length * h
            drawLine(
                color = Color.White.copy(alpha = drop.alpha),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = drop.strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun createRainDrop() = RainDrop(
    x = Random.nextFloat(),
    y = Random.nextFloat() * 1.2f - 0.1f,
    speed = Random.nextFloat() * 0.018f + 0.012f,
    length = Random.nextFloat() * 0.06f + 0.025f,
    alpha = Random.nextFloat() * 0.4f + 0.3f,
    angle = Random.nextFloat() * 0.12f + 0.04f,
    strokeWidth = Random.nextFloat() * 1.5f + 1.2f,
)

// ═════════════════════════════════════════════
// 🌨 雪天：多层雪花 + 左右摆动
// ═════════════════════════════════════════════

private data class Snowflake(
    var x: Float,
    var y: Float,
    val speed: Float,
    val radius: Float,
    val alpha: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val phase: Float,
)

@Composable
private fun SnowEffect(modifier: Modifier = Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }

    val flakes = remember {
        List(100) {
            Snowflake(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.004f + 0.001f,
                radius = Random.nextFloat() * 5f + 2f,
                alpha = Random.nextFloat() * 0.5f + 0.4f,
                swayAmplitude = Random.nextFloat() * 0.02f + 0.005f,
                swayFrequency = Random.nextFloat() * 0.04f + 0.015f,
                phase = Random.nextFloat() * 2f * PI.toFloat(),
            )
        }.toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            flakes.forEach { flake ->
                flake.y += flake.speed
                flake.x += sin(frameTime * flake.swayFrequency + flake.phase) * flake.swayAmplitude * 0.08f
                if (flake.y > 1.05f) {
                    flake.y = -0.03f
                    flake.x = Random.nextFloat()
                }
                if (flake.x < -0.05f) flake.x = 1.05f
                if (flake.x > 1.05f) flake.x = -0.05f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        flakes.forEach { flake ->
            drawCircle(
                color = Color.White.copy(alpha = flake.alpha),
                radius = flake.radius,
                center = Offset(flake.x * w, flake.y * h),
            )
        }
    }
}

// ═════════════════════════════════════════════
// ⛈ 雷暴：密集雨 + 闪电
// ═════════════════════════════════════════════

@Composable
private fun StormEffect(modifier: Modifier = Modifier) {
    // 密集雨层
    RainEffect(modifier = modifier, intensity = 120)

    // 闪电层
    val flashAlpha = remember { mutableListOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2000, 5000))
            // 双闪
            flashAlpha[0] = 0.35f
            delay(70)
            flashAlpha[0] = 0f
            delay(90)
            flashAlpha[0] = 0.18f
            delay(50)
            flashAlpha[0] = 0f
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (flashAlpha[0] > 0f) {
            drawRect(color = Color.White.copy(alpha = flashAlpha[0]))
        }
    }
}

// ═════════════════════════════════════════════
// 🌫 雾天：多层雾带缓慢漂移
// ═════════════════════════════════════════════

private data class FogBand(
    var x: Float,
    val y: Float,
    val speed: Float,
    val width: Float,
    val height: Float,
    val alpha: Float,
)

@Composable
private fun FogEffect(modifier: Modifier = Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }

    val bands = remember {
        List(10) {
            FogBand(
                x = Random.nextFloat() * 1.5f - 0.5f,
                y = Random.nextFloat() * 0.7f + 0.15f,
                speed = Random.nextFloat() * 0.0003f + 0.0001f,
                width = Random.nextFloat() * 0.7f + 0.5f,
                height = Random.nextFloat() * 0.12f + 0.05f,
                alpha = Random.nextFloat() * 0.15f + 0.1f,
            )
        }.toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            bands.forEach { band ->
                band.x += band.speed
                if (band.x > 1.2f) band.x = -band.width - 0.1f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        bands.forEach { band ->
            drawOval(
                color = Color.White.copy(alpha = band.alpha),
                topLeft = Offset(band.x * w, band.y * h),
                size = Size(band.width * w, band.height * h),
            )
        }
    }
}
