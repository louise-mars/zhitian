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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.weathercalendar.data.model.WeatherCondition
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 宫崎骏风格天气动画叠加层 — 柔和自然的粒子动画。
 *
 * 设计理念：模仿吉卜力动画中的自然元素
 * - 蓬松积云（多圆组合）
 * - 丁达尔光束
 * - 飘落花瓣/树叶
 * - 萤火虫光点（夜间）
 * - 手绘感雨滴
 */
@Composable
fun WeatherAnimationOverlay(
    condition: WeatherCondition,
    isDay: Boolean = true,
    modifier: Modifier = Modifier,
) {
    when (condition) {
        WeatherCondition.RAINY, WeatherCondition.DRIZZLE ->
            GhibliRainEffect(
                modifier = modifier,
                intensity = if (condition == WeatherCondition.DRIZZLE) 25 else 50,
            )
        WeatherCondition.SNOWY ->
            GhibliSnowEffect(modifier = modifier)
        WeatherCondition.CLOUDY, WeatherCondition.PARTLY_CLOUDY ->
            GhibliCloudEffect(modifier = modifier)
        WeatherCondition.STORMY ->
            GhibliStormEffect(modifier = modifier)
        WeatherCondition.SUNNY ->
            if (isDay) GhibliSunEffect(modifier = modifier)
            else GhibliNightEffect(modifier = modifier)
        WeatherCondition.FOGGY ->
            GhibliFogEffect(modifier = modifier)
    }

    // 季节性飘落物（所有天气都有，增加氛围）
    if (isDay) {
        SeasonalParticles(modifier = modifier)
    } else {
        GhibliNightEffect(modifier = modifier)
    }
}

// ═════════════════════════════════════════════
// ☀️ 晴天：丁达尔光束 + 柔和光晕
// ═════════════════════════════════════════════

@Composable
private fun GhibliSunEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sun")

    val glowAlpha by transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val beamAngle by transition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "beam",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 柔和大光晕（水彩感）
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF8E1).copy(alpha = glowAlpha),
                    Color(0xFFFFE082).copy(alpha = glowAlpha * 0.4f),
                    Color.Transparent,
                ),
                center = Offset(w * 0.6f, h * 0.08f),
                radius = w * 0.7f,
            ),
            radius = w * 0.7f,
            center = Offset(w * 0.6f, h * 0.08f),
        )

        // 丁达尔光束（从右上角斜射）
        val beamCount = 5
        for (i in 0 until beamCount) {
            val baseAngle = 35f + i * 8f + beamAngle
            val beamAlpha = glowAlpha * (0.15f + i * 0.03f)
            rotate(degrees = baseAngle, pivot = Offset(w * 0.85f, 0f)) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF176).copy(alpha = beamAlpha),
                            Color.Transparent,
                        ),
                    ),
                    topLeft = Offset(w * 0.85f, 0f),
                    size = Size(12f + i * 4f, h * 0.7f),
                )
            }
        }
    }
}

// ═════════════════════════════════════════════
// 🌙 夜间：萤火虫光点
// ═════════════════════════════════════════════

private data class Firefly(
    var x: Float,
    var y: Float,
    val speed: Float,
    val radius: Float,
    var alpha: Float,
    val pulseSpeed: Float,
    val driftX: Float,
)

@Composable
private fun GhibliNightEffect(modifier: Modifier = Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }

    val fireflies = remember {
        List(20) {
            Firefly(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.7f + 0.1f,
                speed = Random.nextFloat() * 0.0005f + 0.0002f,
                radius = Random.nextFloat() * 4f + 2f,
                alpha = Random.nextFloat(),
                pulseSpeed = Random.nextFloat() * 0.05f + 0.02f,
                driftX = Random.nextFloat() * 0.001f - 0.0005f,
            )
        }.toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            fireflies.forEach { f ->
                f.y -= f.speed
                f.x += f.driftX + sin(frameTime * 0.02f) * 0.0003f
                f.alpha = (sin(frameTime * f.pulseSpeed) * 0.5f + 0.5f).coerceIn(0.1f, 0.9f)
                if (f.y < -0.05f) { f.y = 1.05f; f.x = Random.nextFloat() }
                if (f.x < -0.05f) f.x = 1.05f
                if (f.x > 1.05f) f.x = -0.05f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        fireflies.forEach { f ->
            // 外层光晕
            drawCircle(
                color = Color(0xFFFFE082).copy(alpha = f.alpha * 0.3f),
                radius = f.radius * 3f,
                center = Offset(f.x * w, f.y * h),
            )
            // 核心亮点
            drawCircle(
                color = Color(0xFFFFF8E1).copy(alpha = f.alpha * 0.8f),
                radius = f.radius,
                center = Offset(f.x * w, f.y * h),
            )
        }
    }
}

// ═════════════════════════════════════════════
// ☁️ 多云：蓬松积云（宫崎骏式多圆组合）
// ═════════════════════════════════════════════

private data class GhibliCloud(
    var x: Float,
    val y: Float,
    val speed: Float,
    val scale: Float,
    val alpha: Float,
)

@Composable
private fun GhibliCloudEffect(modifier: Modifier = Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }

    val clouds = remember {
        listOf(
            GhibliCloud(x = -0.1f, y = 0.05f, speed = 0.00012f, scale = 1.2f, alpha = 0.25f),
            GhibliCloud(x = 0.4f, y = 0.12f, speed = 0.00018f, scale = 0.9f, alpha = 0.30f),
            GhibliCloud(x = 0.8f, y = 0.08f, speed = 0.00015f, scale = 1.0f, alpha = 0.20f),
            GhibliCloud(x = 0.2f, y = 0.22f, speed = 0.00025f, scale = 0.7f, alpha = 0.35f),
            GhibliCloud(x = 0.6f, y = 0.28f, speed = 0.00030f, scale = 0.6f, alpha = 0.30f),
        ).toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            clouds.forEach { c ->
                c.x += c.speed
                if (c.x > 1.3f) c.x = -0.4f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        clouds.forEach { cloud ->
            drawGhibliCloud(
                center = Offset(cloud.x * w, cloud.y * h),
                scale = cloud.scale * w * 0.15f,
                alpha = cloud.alpha,
            )
        }
    }
}

/** 绘制蓬松积云：多个圆形组合成自然的云朵形状 */
private fun DrawScope.drawGhibliCloud(center: Offset, scale: Float, alpha: Float) {
    val color = Color.White.copy(alpha = alpha)
    // 底部大圆
    drawCircle(color, radius = scale, center = center)
    // 上方隆起
    drawCircle(color, radius = scale * 0.75f, center = Offset(center.x - scale * 0.5f, center.y - scale * 0.3f))
    drawCircle(color, radius = scale * 0.85f, center = Offset(center.x + scale * 0.2f, center.y - scale * 0.45f))
    drawCircle(color, radius = scale * 0.6f, center = Offset(center.x + scale * 0.7f, center.y - scale * 0.15f))
    // 左右延伸
    drawCircle(color, radius = scale * 0.65f, center = Offset(center.x - scale * 0.9f, center.y + scale * 0.1f))
    drawCircle(color, radius = scale * 0.55f, center = Offset(center.x + scale * 1.0f, center.y + scale * 0.1f))
}

// ═════════════════════════════════════════════
// 🌧 雨天：手绘感雨滴（带弧度和不规则）
// ═════════════════════════════════════════════

private data class GhibliRainDrop(
    var x: Float,
    var y: Float,
    val speed: Float,
    val length: Float,
    val alpha: Float,
    val curve: Float,  // 弧度，让雨滴有手绘感
    val thickness: Float,
)

@Composable
private fun GhibliRainEffect(modifier: Modifier = Modifier, intensity: Int = 80) {
    var frameTime by remember { mutableLongStateOf(0L) }

    val drops = remember {
        List(intensity) {
            GhibliRainDrop(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 1.2f - 0.1f,
                speed = Random.nextFloat() * 0.015f + 0.012f,
                length = Random.nextFloat() * 0.04f + 0.02f,
                alpha = Random.nextFloat() * 0.4f + 0.2f,
                curve = Random.nextFloat() * 0.008f - 0.004f,
                thickness = Random.nextFloat() * 1.5f + 1f,
            )
        }.toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            drops.forEach { drop ->
                drop.y += drop.speed
                drop.x += 0.001f  // 微微斜向
                if (drop.y > 1.1f) {
                    drop.y = Random.nextFloat() * -0.15f
                    drop.x = Random.nextFloat()
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drops.forEach { drop ->
            val startX = drop.x * w
            val startY = drop.y * h
            val endY = startY + drop.length * h
            // 带弧度的雨滴（贝塞尔曲线感）
            val path = Path().apply {
                moveTo(startX, startY)
                quadraticTo(
                    startX + drop.curve * w, (startY + endY) / 2,
                    startX + drop.curve * w * 2, endY,
                )
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = drop.alpha),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = drop.thickness,
                    cap = StrokeCap.Round,
                ),
            )
        }
    }
}

// ═════════════════════════════════════════════
// 🌨 雪天：大小不一的雪花 + 轻柔飘落
// ═════════════════════════════════════════════

private data class GhibliSnowflake(
    var x: Float,
    var y: Float,
    val speed: Float,
    val radius: Float,
    val alpha: Float,
    val swayPhase: Float,
    val swayAmp: Float,
)

@Composable
private fun GhibliSnowEffect(modifier: Modifier = Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }

    val flakes = remember {
        List(45) {
            GhibliSnowflake(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.003f + 0.001f,
                radius = Random.nextFloat() * 5f + 2f,
                alpha = Random.nextFloat() * 0.5f + 0.3f,
                swayPhase = Random.nextFloat() * PI.toFloat() * 2,
                swayAmp = Random.nextFloat() * 0.015f + 0.005f,
            )
        }.toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            flakes.forEach { f ->
                f.y += f.speed
                f.x += sin(frameTime * 0.015f + f.swayPhase) * f.swayAmp * 0.05f
                if (f.y > 1.05f) { f.y = -0.05f; f.x = Random.nextFloat() }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        flakes.forEach { f ->
            // 柔和光晕
            drawCircle(
                color = Color.White.copy(alpha = f.alpha * 0.3f),
                radius = f.radius * 2f,
                center = Offset(f.x * w, f.y * h),
            )
            // 雪花核心
            drawCircle(
                color = Color.White.copy(alpha = f.alpha),
                radius = f.radius,
                center = Offset(f.x * w, f.y * h),
            )
        }
    }
}

// ═════════════════════════════════════════════
// ⛈ 雷暴：密集雨 + 闪电
// ═════════════════════════════════════════════

@Composable
private fun GhibliStormEffect(modifier: Modifier = Modifier) {
    GhibliRainEffect(modifier = modifier, intensity = 60)

    val flashAlpha = remember { mutableListOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3000, 7000))
            flashAlpha[0] = 0.3f
            delay(60)
            flashAlpha[0] = 0f
            delay(100)
            flashAlpha[0] = 0.15f
            delay(40)
            flashAlpha[0] = 0f
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (flashAlpha[0] > 0f) {
            drawRect(color = Color(0xFFFFF8E1).copy(alpha = flashAlpha[0]))
        }
    }
}

// ═════════════════════════════════════════════
// 🌫 雾天：层叠薄雾缓慢流动
// ═════════════════════════════════════════════

private data class GhibliFogLayer(
    var x: Float,
    val y: Float,
    val speed: Float,
    val width: Float,
    val height: Float,
    val alpha: Float,
)

@Composable
private fun GhibliFogEffect(modifier: Modifier = Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }

    val layers = remember {
        List(8) {
            GhibliFogLayer(
                x = Random.nextFloat() * 1.5f - 0.5f,
                y = Random.nextFloat() * 0.6f + 0.2f,
                speed = Random.nextFloat() * 0.0002f + 0.00005f,
                width = Random.nextFloat() * 0.6f + 0.4f,
                height = Random.nextFloat() * 0.08f + 0.04f,
                alpha = Random.nextFloat() * 0.12f + 0.05f,
            )
        }.toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            layers.forEach { l ->
                l.x += l.speed
                if (l.x > 1.3f) l.x = -l.width - 0.1f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        layers.forEach { l ->
            drawOval(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = l.alpha),
                        Color.White.copy(alpha = l.alpha),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(l.x * w, l.y * h),
                size = Size(l.width * w, l.height * h),
            )
        }
    }
}

// ═════════════════════════════════════════════
// 🌸 季节性飘落物（花瓣/树叶）
// ═════════════════════════════════════════════

private data class FloatingPetal(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val alpha: Float,
    val color: Color,
)

@Composable
private fun SeasonalParticles(modifier: Modifier = Modifier) {
    var frameTime by remember { mutableLongStateOf(0L) }

    // 根据月份选择颜色（春=粉色花瓣，夏=绿叶，秋=红叶，冬=无）
    val month = java.time.LocalDate.now().monthValue
    val particleColor = when (month) {
        in 3..5 -> Color(0xFFFFB7C5)   // 春：樱花粉
        in 6..8 -> Color(0xFFA5D6A7)   // 夏：嫩绿
        in 9..11 -> Color(0xFFFFCC80)  // 秋：金黄
        else -> return  // 冬天不飘落
    }

    val petals = remember {
        List(12) {
            FloatingPetal(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.5f - 0.1f,
                speed = Random.nextFloat() * 0.002f + 0.0008f,
                size = Random.nextFloat() * 8f + 4f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 2f - 1f,
                alpha = Random.nextFloat() * 0.4f + 0.2f,
                color = particleColor,
            )
        }.toMutableList()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            frameTime++
            petals.forEach { p ->
                p.y += p.speed
                p.x += sin(frameTime * 0.01f + p.rotation) * 0.001f
                if (p.y > 1.1f) {
                    p.y = -0.1f
                    p.x = Random.nextFloat()
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        petals.forEach { p ->
            val cx = p.x * w
            val cy = p.y * h
            val angle = (frameTime * p.rotationSpeed + p.rotation) % 360f

            rotate(degrees = angle, pivot = Offset(cx, cy)) {
                // 花瓣/叶子形状（椭圆）
                drawOval(
                    color = p.color.copy(alpha = p.alpha),
                    topLeft = Offset(cx - p.size, cy - p.size * 0.5f),
                    size = Size(p.size * 2f, p.size),
                )
            }
        }
    }
}
