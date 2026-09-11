package com.pranav.dotto.presentation.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pranav.dotto.application.state.DottoUiState
import com.pranav.dotto.domain.model.Line

@Composable
fun PvPGameScreen(
    state: DottoUiState.PvPPlaying,
    onLineTapped: (Line) -> Unit,
    onNewGame: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Map PvPPlaying to standard Playing state to re-use GameScreen directly
    val playingState = DottoUiState.Playing(
        gameState = state.gameState,
        isAiThinking = false, // PvP uses remote turn indicator instead of AI thinking
        lastMoveLine = state.lastMoveLine,
        recentlyCompletedBoxes = state.recentlyCompletedBoxes
    )

    GameScreen(
        state = playingState,
        onLineTapped = onLineTapped,
        onNewGame = onNewGame,
        onRestart = onRestart,
        modifier = modifier
    )
}
