package com.pranav.dotto.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pranav.dotto.presentation.theme.DottoPrimary
import com.pranav.dotto.presentation.theme.DottoSecondary

@Composable
fun TurnIndicator(text: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    AnimatedContent(
        targetState = text,
        label = "turn-indicator",
        transitionSpec = {
            fadeIn(tween(300)) + slideInVertically(tween(300)) togetherWith
            fadeOut(tween(300)) + slideOutVertically(tween(300))
        }
    ) { label ->
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DottoPrimary.copy(alpha = 0.2f * alpha),
                            DottoSecondary.copy(alpha = 0.2f * alpha)
                        )
                    )
                )
                .border(1.dp, DottoPrimary.copy(alpha = 0.5f * alpha), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )
        }
    }
}
