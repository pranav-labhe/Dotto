package com.pranav.dotto.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renders a field of randomly positioned stars that twinkle with independent alpha animations.
 */
@Composable
fun StarField(modifier: Modifier = Modifier, starCount: Int = 70) {
    val stars = remember {
        List(starCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = 2f + Random.nextFloat() * 4f, // Slightly larger to see spikes
                alphaPhase = Random.nextFloat() * Math.PI.toFloat() * 2f,
                speed = 0.5f + Random.nextFloat() * 1.5f,
                spikes = Random.nextInt(4, 8) // 4 to 7 spikes
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "star-twinkle")
    val twinkleFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { star ->
            // Use sine wave based on time and individual phase for randomized twinkling
            val alpha = 0.1f + 0.5f * (sin(twinkleFactor * star.speed * Math.PI.toFloat() * 2f + star.alphaPhase) + 1f) / 2f
            
            drawStar(
                center = Offset(star.x * size.width, star.y * size.height),
                spikes = star.spikes,
                outerRadius = star.size,
                innerRadius = star.size * 0.4f,
                color = Color(0xFF5DE1E6).copy(alpha = alpha)
            )
        }
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    spikes: Int,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path()
    val angleStep = Math.PI.toFloat() / spikes
    
    for (i in 0 until 2 * spikes) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = i * angleStep - Math.PI.toFloat() / 2f
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
}

private data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val alphaPhase: Float,
    val speed: Float,
    val spikes: Int
)
