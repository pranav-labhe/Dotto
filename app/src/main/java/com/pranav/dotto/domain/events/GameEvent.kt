package com.pranav.dotto.domain.events

import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.GameOutcome
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId

/**
 * Explicit domain events emitted by the engine. Consumers (UI animation,
 * logging, future analytics/replay/network sync) subscribe to these instead
 * of diffing GameState snapshots by hand.
 */
sealed interface GameEvent {

    data class GameStarted(val playerIds: List<PlayerId>) : GameEvent

    data class MoveMade(val playerId: PlayerId, val line: Line, val moveNumber: Int) : GameEvent

    data class BoxCompleted(val box: BoxCoordinate, val ownerId: PlayerId) : GameEvent

    data class ScoreChanged(val playerId: PlayerId, val newScore: Int) : GameEvent

    data class TurnChanged(val nextPlayerId: PlayerId) : GameEvent

    data class ExtraTurnGranted(val playerId: PlayerId) : GameEvent

    data class GameFinished(val outcome: GameOutcome) : GameEvent

    data class InvalidMove(val playerId: PlayerId, val line: Line, val reason: String) : GameEvent
}
