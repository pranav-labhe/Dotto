package com.pranav.dotto.domain.model

import com.pranav.dotto.domain.events.GameEvent

/**
 * The observable outcome of attempting a move. Making this explicit (rather
 * than just returning a new GameState) is what makes moves testable,
 * animatable, loggable, and — eventually — network-syncable: every caller
 * gets a full account of what happened, not just the end state.
 */
data class MoveResult(
    val accepted: Boolean,
    val previousState: GameState,
    val newState: GameState,
    val completedBoxes: List<BoxCoordinate>,
    val extraTurn: Boolean,
    val events: List<GameEvent>
) {
    companion object {
        fun rejected(state: GameState, reason: GameEvent.InvalidMove): MoveResult =
            MoveResult(
                accepted = false,
                previousState = state,
                newState = state,
                completedBoxes = emptyList(),
                extraTurn = false,
                events = listOf(reason)
            )
    }
}
