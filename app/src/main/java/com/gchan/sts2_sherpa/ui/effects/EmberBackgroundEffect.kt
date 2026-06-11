package com.gchan.sts2_sherpa.ui.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.sin

private const val ASH_COUNT = 66
private const val EMBER_DOT_COUNT = 86
private const val EFFECT_ALPHA_MULTIPLIER = 1.22f
private const val ASH_SPEED_MULTIPLIER = 1.65f
private const val EMBER_SPEED_MULTIPLIER = 1.85f
private const val GLOW_MID_ALPHA = 0.18f
private const val GLOW_BOTTOM_ALPHA = 0.42f

private val bottomGlowDeepRed = Color(0xFF5E1008)
private val bottomGlowRed = Color(0xFF8B1F0D)
private val bottomGlowOrange = Color(0xFFD24A18)
private val particleHotCore = Color(0xFFFFD88A)
private val particleWarmGold = Color(0xFFFFB858)
private val particleEmber = Color(0xFFFF8432)
private val particleCopper = Color(0xFFE35B24)
private val ashColor = Color(0xFFFFB36A)

private data class AshParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val drift: Float,
    val speed: Float,
    val phase: Float,
    val swayFrequency: Float
)

private data class EmberDot(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val drift: Float,
    val speed: Float,
    val warm: Float,
    val phase: Float,
    val swayFrequency: Float,
    val twinkleSpeed: Float
)

private val ashParticles = List(ASH_COUNT) { index ->
    AshParticle(
        x = unit(index, 0.31f),
        y = unit(index, 0.47f),
        radius = 0.55f + unit(index, 0.71f) * 2.05f,
        alpha = 0.07f + unit(index, 0.93f) * 0.22f,
        drift = (-0.0035f + unit(index, 1.13f) * 0.0075f) * ASH_SPEED_MULTIPLIER,
        speed = (0.010f + unit(index, 1.37f) * 0.020f) * ASH_SPEED_MULTIPLIER,
        phase = unit(index, 1.59f) * 120f,
        swayFrequency = 0.75f + unit(index, 1.81f) * 1.55f
    )
}

private val emberDots = List(EMBER_DOT_COUNT) { index ->
    EmberDot(
        x = unit(index, 1.61f),
        y = unit(index, 1.83f),
        radius = 0.85f + unit(index, 2.07f) * 3.45f,
        alpha = 0.20f + unit(index, 2.31f) * 0.52f,
        drift = (-0.004f + unit(index, 2.57f) * 0.013f) * EMBER_SPEED_MULTIPLIER,
        speed = (0.018f + unit(index, 2.79f) * 0.040f) * EMBER_SPEED_MULTIPLIER,
        warm = unit(index, 3.01f),
        phase = unit(index, 3.23f) * 120f,
        swayFrequency = 1.0f + unit(index, 3.47f) * 2.6f,
        twinkleSpeed = 1.6f + unit(index, 3.71f) * 5.2f
    )
}

@Composable
fun EmberBackgroundEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ember_background")
    val ashProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 132000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ash_time"
    )
    val dotProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 97000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ember_dot_time"
    )

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        drawBottomGlow()
        ashParticles.forEach { drawAshParticle(it, ashProgress) }
        emberDots.forEach { drawEmberDot(it, dotProgress) }
    }
}

private fun DrawScope.drawBottomGlow() {
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.Transparent,
                0.16f to Color.Transparent,
                0.38f to bottomGlowDeepRed.copy(alpha = GLOW_MID_ALPHA * 0.30f),
                0.58f to bottomGlowRed.copy(alpha = GLOW_MID_ALPHA * 0.72f),
                0.78f to bottomGlowRed.copy(alpha = GLOW_MID_ALPHA),
                1.00f to bottomGlowOrange.copy(alpha = GLOW_BOTTOM_ALPHA)
            )
        )
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                bottomGlowOrange.copy(alpha = 0.32f),
                bottomGlowRed.copy(alpha = 0.20f),
                bottomGlowDeepRed.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.50f, size.height * 1.02f),
            radius = size.width * 1.06f
        ),
        center = Offset(size.width * 0.50f, size.height * 1.02f),
        radius = size.width * 1.06f
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                bottomGlowRed.copy(alpha = 0.14f),
                bottomGlowDeepRed.copy(alpha = 0.07f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.18f, size.height * 0.88f),
            radius = size.width * 0.58f
        ),
        center = Offset(size.width * 0.18f, size.height * 0.88f),
        radius = size.width * 0.58f
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                bottomGlowOrange.copy(alpha = 0.12f),
                bottomGlowRed.copy(alpha = 0.07f),
                Color.Transparent
            ),
            center = Offset(size.width * 0.82f, size.height * 0.86f),
            radius = size.width * 0.52f
        ),
        center = Offset(size.width * 0.82f, size.height * 0.86f),
        radius = size.width * 0.52f
    )
}

private fun DrawScope.drawAshParticle(particle: AshParticle, time: Float) {
    val particleTime = time + particle.phase
    val loop = wrap(particle.y - particleTime * particle.speed)
    val wobble = sin(particleTime * particle.swayFrequency + particle.x * 9.3f) * 0.018f
    val x = wrap(particle.x + particle.drift * particleTime + wobble) * size.width
    val y = loop * size.height
    val mask = readabilityMask(x / size.width, y / size.height)
    val verticalFade = upwardFade(y / size.height)

    drawCircle(
        color = ashColor.copy(alpha = particle.alpha * mask * verticalFade * EFFECT_ALPHA_MULTIPLIER),
        radius = particle.radius,
        center = Offset(x, y)
    )
}

private fun DrawScope.drawEmberDot(particle: EmberDot, time: Float) {
    val particleTime = time + particle.phase
    val loop = wrap(particle.y - particleTime * particle.speed)
    val wobble = sin(particleTime * particle.swayFrequency + particle.y * 12.0f) * 0.018f
    val x = wrap(particle.x + particle.drift * particleTime + wobble) * size.width
    val y = loop * size.height
    val pulse = 0.54f + 0.46f * abs(sin(particleTime * particle.twinkleSpeed + particle.x * 14.7f))
    val mask = readabilityMask(x / size.width, y / size.height)
    val verticalFade = upwardFade(y / size.height)
    val color = when {
        particle.warm > 0.80f -> particleHotCore
        particle.warm > 0.56f -> particleWarmGold
        particle.warm > 0.24f -> particleEmber
        else -> particleCopper
    }
    val alpha = particle.alpha * pulse * mask * verticalFade * EFFECT_ALPHA_MULTIPLIER

    drawCircle(
        color = color.copy(alpha = alpha * 0.24f),
        radius = particle.radius * 4.8f,
        center = Offset(x, y)
    )
    drawCircle(
        color = color.copy(alpha = alpha * 0.66f),
        radius = particle.radius * 1.90f,
        center = Offset(x, y)
    )
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = particle.radius * 0.72f,
        center = Offset(x, y)
    )
}

private fun readabilityMask(x: Float, y: Float): Float {
    val inMainContentBand = x in 0.10f..0.90f && y in 0.22f..0.86f
    val inRecommendationBand = x in 0.12f..0.88f && y in 0.52f..0.84f
    return when {
        inRecommendationBand -> 0.48f
        inMainContentBand -> 0.66f
        else -> 1.0f
    }
}

private fun upwardFade(y: Float): Float {
    return when {
        y < 0.08f -> 0.08f
        y < 0.42f -> 0.08f + ((y - 0.08f) / 0.34f) * 0.62f
        y < 0.72f -> 0.70f + ((y - 0.42f) / 0.30f) * 0.30f
        else -> 1.0f
    }
}

private fun wrap(value: Float): Float {
    val mod = value % 1f
    return if (mod < 0f) mod + 1f else mod
}

private fun unit(index: Int, salt: Float): Float {
    val value = sin((index + 1) * salt * 12.9898f) * 43758.5453f
    return abs(value % 1f)
}
