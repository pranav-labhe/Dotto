package com.pranav.dotto.application.state

import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line

/**
 * Top-level UI state for the whole app, mirroring the Setup -> Game -> Result
 * flow. The ViewModel exposes exactly one of these at a time via StateFlow;
 * Composables just render whichever case is current.
 */
sealed interface DottoUiState {

    data class Setup(
        val config: SetupConfig = SetupConfig()
    ) : DottoUiState

    data class Playing(
        val gameState: GameState,
        val isAiThinking: Boolean = false,
        val lastMoveLine: Line? = null,
        val recentlyCompletedBoxes: Set<com.pranav.dotto.domain.model.BoxCoordinate> = emptySet()
    ) : DottoUiState

    data class Result(
        val gameState: GameState
    ) : DottoUiState
}
