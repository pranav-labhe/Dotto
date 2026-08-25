package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId
import kotlin.random.Random

/**
 * Chooses a line to play. Implementations are pure/deterministic given a
 * [Random] source, which keeps them unit-testable with a fixed seed while
 * still behaving randomly in production play.
 */
interface AiStrategy {
    fun chooseLine(gameState: GameState, playerId: PlayerId, random: Random): Line
}
