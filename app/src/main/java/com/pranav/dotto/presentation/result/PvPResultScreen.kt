package com.pranav.dotto.presentation.result

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.dotto.domain.model.GameOutcome
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.GameStatus
import com.pranav.dotto.domain.model.PlayerId
import com.pranav.dotto.presentation.theme.*

@Composable
fun PvPResultScreen(
    gameState: GameState,
    localPlayerId: PlayerId,
    onPlayAgain: () -> Unit,
    onBackToRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outcome = (gameState.status as? GameStatus.Finished)?.outcome
    var visible by remember { mutableStateOf(false) }
    
    val isWinner = remember(outcome) {
        (outcome as? GameOutcome.Win)?.winnerId == localPlayerId
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
        if (isWinner) {
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
                    if (outcome.winnerId == localPlayerId) "YOU WON!"
                    else "${winner?.name?.uppercase() ?: "OPPONENT"} WON!"
                }
                GameOutcome.Draw -> "IT'S A DRAW!"
                else -> "MATCH OVER"
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000)) + scaleIn(spring(Spring.DampingRatioMediumBouncy))
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isWinner) DottoPrimary else DottoSecondary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            Card(
                modifier = Modifier
                    .padding(vertical = 40.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DottoSurface.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    gameState.players.forEach { player ->
                        val isLocal = player.id == localPlayerId
                        val playerColor = if (player.id == PlayerId("host_player")) DottoPrimary else DottoSecondary
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = player.name.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isLocal) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = if (isLocal) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isLocal) {
                                    Text("(YOU)", style = MaterialTheme.typography.labelSmall, color = playerColor.copy(alpha = 0.8f))
                                }
                            }
                            Text(
                                text = gameState.scoreOf(player.id).toString(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = if (outcome is GameOutcome.Win && outcome.winnerId == player.id) playerColor else Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DottoPrimary)
            ) {
                Text("REMATCH", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = DottoBackground)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBackToRoom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DottoSecondary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DottoSecondary)
            ) {
                Text("BACK TO ROOM", fontWeight = FontWeight.Bold)
            }
        }
    }
}
