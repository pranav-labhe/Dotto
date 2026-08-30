package com.pranav.dotto.presentation.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.dotto.application.state.DottoUiState
import com.pranav.dotto.presentation.components.DottoBoard
import com.pranav.dotto.presentation.components.ScorePanel
import com.pranav.dotto.presentation.components.StarField
import com.pranav.dotto.presentation.components.TurnIndicator
import com.pranav.dotto.presentation.components.DottoAdView
import com.pranav.dotto.presentation.theme.*
import com.pranav.dotto.domain.model.PlayerType

@Composable
fun GameScreen(
    state: DottoUiState.Playing,
    onLineTapped: (com.pranav.dotto.domain.model.Line) -> Unit,
    onNewGame: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState = state.gameState
    var isAdVisible by remember { mutableStateOf(false) }
    var isAdClosedByUser by remember { mutableStateOf(false) }
    
    // Zoom/Pan State
    var scale by remember(gameState.board.config) { mutableStateOf(1f) }
    var offset by remember(gameState.board.config) { mutableStateOf(Offset.Zero) }
    var isPanningMode by remember { mutableStateOf(false) }

    // Title Animation (Shared with SetupScreen)
    val infiniteTransition = rememberInfiniteTransition(label = "neon-title")
    val titleGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val breathingGlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DottoBackground)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Glassmorphism Header Bar with Orbiting Star
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                color = DottoSurface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Far Left: Quit Button
                    IconButton(
                        onClick = onNewGame,
                        modifier = Modifier
                            .size(40.dp)
                            .background(DottoTertiary.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, DottoTertiary.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.PowerSettingsNew,
                            contentDescription = "Quit",
                            tint = DottoTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Center: Title and Move Badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "DOTTO",
                                style = MaterialTheme.typography.titleLarge,
                                color = DottoPrimary.copy(alpha = 0.3f * titleGlow),
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.offset(y = 1.dp)
                            )
                            Text(
                                "DOTTO",
                                style = MaterialTheme.typography.titleLarge,
                                color = DottoPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "MOVE ${gameState.moveNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Far Right: Restart Button
                    IconButton(
                        onClick = onRestart,
                        modifier = Modifier
                            .size(40.dp)
                            .background(DottoSecondary.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, DottoSecondary.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.Replay,
                            contentDescription = "Restart",
                            tint = DottoSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Orbiting Star Effect for the Header
            HeaderBorderStar(modifier = Modifier.matchParentSize())
        }

        ScorePanel(
            players = gameState.players,
            scores = gameState.scores,
            currentPlayerId = gameState.currentPlayerId,
            colorFor = PlayerPresentation::colorFor
        )

        val turnLabel = when {
            state.isAiThinking -> "${gameState.currentPlayer?.name ?: "Dotto"} is thinking…"
            gameState.currentPlayer?.type == PlayerType.HUMAN -> "YOUR TURN"
            else -> "${gameState.currentPlayer?.name ?: "Opponent"}'s turn"
        }
        
        val isHumanTurn = gameState.currentPlayer?.type == PlayerType.HUMAN && !state.isAiThinking
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (isHumanTurn) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            TurnIndicator(text = turnLabel, isHumanTurn = isHumanTurn)
        }

        // Consolidated Board and Controls Container to remove gaps
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DottoBackground.copy(alpha = 0.1f * breathingGlow),
                                Color.Transparent,
                                DottoBackground.copy(alpha = 0.05f * breathingGlow)
                            )
                        )
                    )
                    .padding(top = 2.dp) // Removed horizontal padding to align with Header
            ) {
                // Subtle Glossy Sheen Overlay
                Canvas(modifier = Modifier.matchParentSize()) {
                    val sheenBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFACA95D).copy(alpha = 0.08f * breathingGlow),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                    drawRect(brush = sheenBrush)
                }

                DottoBoard(
                    gameState = gameState,
                    recentlyCompletedBoxes = state.recentlyCompletedBoxes,
                    lastMoveLine = state.lastMoveLine,
                    enabled = !state.isAiThinking && gameState.currentPlayer?.type == PlayerType.HUMAN,
                    scale = scale,
                    offset = offset,
                    isPanningMode = isPanningMode,
                    onOffsetChange = { offset = it },
                    onScaleChange = { scale = it },
                    onLineTapped = onLineTapped,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isHumanTurn) {
                    Text(
                        text = "TAP TO BRIDGE THE DOTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, letterSpacing = 1.sp),
                        color = Color(0xFFAD582F).copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 4.dp)
                    )
                }
            }

            // Stars Holder Block with Overlapping Controls - Touches the board now
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                StarField(starCount = 57)

                // Board Controls Row (Level 6+) - Overlapping with StarField
                if (gameState.board.config.dotRows - 2 >= 6) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { scale = (scale * 1.2f).coerceAtMost(4.0f) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ZoomIn,
                                    contentDescription = "Zoom In",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { isPanningMode = !isPanningMode },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.OpenWith,
                                    contentDescription = "Move",
                                    tint = if (isPanningMode) DottoPrimary else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    scale = (scale / 1.2f).coerceAtLeast(0.3f)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ZoomOut,
                                    contentDescription = "Zoom Out",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Consolidated Ad Container
        if (!isAdClosedByUser) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.End
            ) {
                if (isAdVisible) {
                    // Close Button for Ads - Back to original small size
                    IconButton(
                        onClick = { isAdClosedByUser = true },
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hide Ad",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DottoAdView(
                        modifier = Modifier.fillMaxWidth().then(
                            if (!isAdVisible) Modifier.height(50.dp) else Modifier.wrapContentHeight()
                        ),
                        onAdLoaded = { isAdVisible = true },
                        onAdFailed = { isAdVisible = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.HeaderBorderStar(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "header-orbit")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val perimeter = 2 * (w + h)
        val currentDist = progress * perimeter

        val center = when {
            currentDist < w -> Offset(currentDist, 0f)
            currentDist < w + h -> Offset(w, currentDist - w)
            currentDist < 2 * w + h -> Offset(w - (currentDist - (w + h)), h)
            else -> Offset(0f, h - (currentDist - (2 * w + h)))
        }

        // Comet Shadow/Trail
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                center = center,
                radius = 40f
            ),
            center = center,
            radius = 40f
        )
        
        // Bright Star Core
        drawCircle(
            color = Color.White,
            radius = 4f,
            center = center
        )
    }
}
