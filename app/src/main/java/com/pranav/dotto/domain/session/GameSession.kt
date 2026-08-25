package com.pranav.dotto.domain.session

import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameState
import java.util.UUID

/**
 * Wraps a single game's lifecycle and history, separate from [com.pranav.dotto.domain.engine.GameEngine]
 * (which is stateless rule logic). A session is what future features
 * (replay, match history, resuming, multiplayer room state) will hang off of
 * — the engine itself never needs to know sessions exist.
 */
data class GameSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val currentState: GameState,
    val moveHistory: List<GameMove> = emptyList()
) {
    fun withNewState(state: GameState, appliedMove: GameMove?): GameSession =
        copy(
            currentState = state,
            moveHistory = if (appliedMove != null) moveHistory + appliedMove else moveHistory
        )
}
