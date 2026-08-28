package com.pranav.dotto.presentation.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.dotto.R
import com.pranav.dotto.presentation.components.StarField

/**
 * 512x512 Square Store Icon
 */
@Preview(widthDp = 512, heightDp = 512)
@Composable
fun DottoStoreIcon() {
    DottoTheme {
        Box(
            modifier = Modifier
                .size(512.dp)
                .background(DottoBackground),
            contentAlignment = Alignment.Center
        ) {
            StarField(starCount = 60)
            
            // Using the official Launcher Foreground for the icon branding
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(480.dp) // Adjusted for clear visibility in the 512x512 box
                    .scale(1.2f)
            )
        }
    }
}

/**
 * 1024x500 Play Store Feature Graphic
 * NOTE: When using "Save as PNG" from the Preview tab, ensure the final output
 * is resized to exactly 1024x500 pixels for the Play Console.
 */
@Preview(widthDp = 1024, heightDp = 500)
@Composable
fun DottoFeatureGraphic() {
    DottoTheme {
        val infiniteTransition = rememberInfiniteTransition(label = "banner-glow")
        val breathingGlow by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing"
        )

        Box(
            modifier = Modifier
                .size(width = 1024.dp, height = 500.dp)
                .background(DottoBackground)
        ) {
            StarField(starCount = 100)
            
            // Decorative Big Circles (Neon Orbs)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(DottoPrimary.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.2f),
                        radius = 400f
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = 400f
                )
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(DottoSecondary.copy(alpha = 0.1f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.8f),
                        radius = 500f
                    ),
                    center = Offset(size.width * 0.1f, size.height * 0.8f),
                    radius = 500f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 60.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Neon Title Reconstructed
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "DOTTO",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 140.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = DottoSecondary.copy(alpha = breathingGlow * 0.4f),
                                blurRadius = 20f * breathingGlow
                            )
                        ),
                        color = DottoSecondary.copy(alpha = 0.2f),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "DOTTO",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 140.sp
                        ),
                        color = DottoPrimary,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CONNECT THE DOTS. CAPTURE THE GRID.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Feature pills
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeaturePill(text = "ADAPTIVE AI", color = DottoSecondary)
                    FeaturePill(text = "NEON VIBE", color = DottoPrimary)
                    FeaturePill(text = "OFFLINE PLAY", color = Color.White)
                }
            }
            
            // Stylized board fragment in the corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 100.dp, y = 50.dp)
                    .scale(1.5f)
            ) {
                Canvas(modifier = Modifier.size(300.dp)) {
                    val step = 60f
                    for (i in 0..4) {
                        for (j in 0..4) {
                            drawCircle(Color.White.copy(alpha = 0.2f), radius = 4f, center = Offset(i * step, j * step))
                        }
                    }
                    // Draw a few lines
                    drawLine(DottoPrimary, Offset(0f, 0f), Offset(step, 0f), strokeWidth = 4f)
                    drawLine(DottoSecondary, Offset(step, 0f), Offset(step, step), strokeWidth = 4f)
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(text: String, color: Color) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontWeight = FontWeight.Bold
        )
    }
}
