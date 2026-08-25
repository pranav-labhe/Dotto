package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId

/**
 * Chooses the best line among a candidate set by searching ahead.
 * Replaceable independently from [AiStrategy] so "Hard" can later swap in a
 * different search algorithm without touching the difficulty wiring.
 */
interface SearchStrategy {
    fun bestMove(
        gameState: GameState,
        playerId: PlayerId,
        candidates: List<Line>,
        maxDepth: Int
    ): Line
}
