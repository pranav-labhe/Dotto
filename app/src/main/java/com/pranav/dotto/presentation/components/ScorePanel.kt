package com.pranav.dotto.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pranav.dotto.domain.model.Player
import com.pranav.dotto.domain.util.GameUtils

@Composable
fun ScorePanel(
    players: List<Player>,
    scores: Map<com.pranav.dotto.domain.model.PlayerId, Int>,
    currentPlayerId: com.pranav.dotto.domain.model.PlayerId?,
    colorFor: (com.pranav.dotto.domain.model.PlayerColorToken) -> Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        players.forEach { player ->
            val isTurn = player.id == currentPlayerId
            val playerColor = colorFor(player.colorToken)
            
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (isTurn) Modifier.border(2.dp, playerColor.copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
                        else Modifier
                    ),
                color = if (isTurn) playerColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayerDot(color = playerColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = (scores[player.id] ?: 0).toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isTurn) playerColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerDot(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.padding(2.dp)) {
        drawCircle(color = color, radius = 7f)
    }
}
