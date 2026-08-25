package com.pranav.dotto.domain.player

import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId
import kotlinx.coroutines.CompletableDeferred

/**
 * A human's move arrives asynchronously from a UI tap. This controller just
 * bridges that: [provideMove] is called by the presentation layer when the
 * user taps a line, and [selectMove] suspends until that happens.
 */
class HumanPlayerController(
    private val playerId: PlayerId
) : PlayerController {

    private var pending: CompletableDeferred<Line>? = null

    override suspend fun selectMove(gameState: GameState): GameMove {
        val deferred = CompletableDeferred<Line>()
        pending = deferred
        val line = deferred.await()
        pending = null
        return GameMove(playerId = playerId, line = line)
    }

    /** Called from the UI layer when the human taps a line. */
    fun provideMove(line: Line) {
        pending?.complete(line)
    }

    /** Cancels any move currently awaited, e.g. when starting a new game. */
    fun cancelPending() {
        pending?.cancel()
        pending = null
    }
}
