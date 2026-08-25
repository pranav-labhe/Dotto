package com.pranav.dotto.presentation.result

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.dotto.domain.model.GameOutcome
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.GameStatus
import com.pranav.dotto.domain.model.PlayerType
import com.pranav.dotto.domain.util.GameUtils
import com.pranav.dotto.presentation.theme.DottoBackground
import com.pranav.dotto.presentation.theme.DottoPrimary
import com.pranav.dotto.presentation.theme.DottoSecondary
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ResultScreen(
    gameState: GameState,
    onPlayAgain: () -> Unit,
    onNewSetup: () -> Unit,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outcome = (gameState.status as? GameStatus.Finished)?.outcome
    var visible by remember { mutableStateOf(false) }
    val isHumanWinner = remember(outcome) {
        if (outcome is GameOutcome.Win) {
            gameState.player(outcome.winnerId)?.type == PlayerType.HUMAN
        } else false
    }
    
    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DottoBackground)
            .safeDrawingPadding()
    ) {
        if (isHumanWinner) {
            ConfettiEffect()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val headline = when (outcome) {
                is GameOutcome.Win -> {
                    val winner = gameState.player(outcome.winnerId)
                    GameUtils.getWinMessage(winner?.name ?: "Dotto")
                }
                GameOutcome.Draw -> "IT'S A DRAW!"
                null -> "GAME OVER"
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000)) + scaleIn(spring(Spring.DampingRatioMediumBouncy))
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isHumanWinner) DottoPrimary else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }

            Card(
                modifier = Modifier
                    .padding(vertical = 40.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    gameState.players.forEach { player ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = gameState.scoreOf(player.id).toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (outcome is GameOutcome.Win && outcome.winnerId == player.id) DottoPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Button(
                onClick = if (isHumanWinner) onNextLevel else onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isHumanWinner) "START NEXT LEVEL" else "PLAY AGAIN",
                    fontWeight = FontWeight.ExtraBold
                )
            }

            OutlinedButton(
                onClick = onNewSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
            ) {
                Text("BACK TO SETUP", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConfettiEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val colors = listOf(DottoPrimary, DottoSecondary, Color.Yellow, Color.Cyan, Color.Magenta)
    
    val particles = remember {
        List(50) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f,
                speed = 0.005f + Random.nextFloat() * 0.01f,
                size = 5f + Random.nextFloat() * 10f,
                color = colors.random(),
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val currentY = ((p.y + progress * (p.speed * 400f)) % 1.2f) * size.height
            val currentX = p.x * size.width
            
            rotate(p.rotation + progress * 360f, Offset(currentX, currentY)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(currentX, currentY),
                    size = Size(p.size, p.size * 1.5f)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val rotation: Float
)
