package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId
import kotlin.random.Random

/** Picks uniformly among all legal moves. No lookahead at all. */
class EasyAiStrategy : AiStrategy {
    override fun chooseLine(gameState: GameState, playerId: PlayerId, random: Random): Line {
        val available = BoardAnalyzer.availableLines(gameState.board)
        check(available.isNotEmpty()) { "EasyAiStrategy asked to move with no available lines" }
        return available[random.nextInt(available.size)]
    }
}
