package com.pranav.dotto.presentation.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pranav.dotto.application.state.DottoUiState
import com.pranav.dotto.presentation.components.DottoBoard
import com.pranav.dotto.presentation.components.ScorePanel
import com.pranav.dotto.presentation.components.TurnIndicator
import com.pranav.dotto.presentation.theme.PlayerPresentation
import com.pranav.dotto.domain.model.PlayerType

@Composable
fun GameScreen(
    state: DottoUiState.Playing,
    onLineTapped: (com.pranav.dotto.domain.model.Line) -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState = state.gameState

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DOTTO", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onNewGame) { Text("New Game") }
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
        TurnIndicator(text = turnLabel)

        DottoBoard(
            gameState = gameState,
            recentlyCompletedBoxes = state.recentlyCompletedBoxes,
            lastMoveLine = state.lastMoveLine,
            enabled = !state.isAiThinking && gameState.currentPlayer?.type == PlayerType.HUMAN,
            onLineTapped = onLineTapped,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
