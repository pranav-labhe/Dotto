package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId
import kotlin.random.Random

/**
 * Priority order:
 *  1. Complete a box immediately if any move allows it.
 *  2. Otherwise play a "safe" line that doesn't give the opponent a free box.
 *  3. Otherwise (no safe lines exist) play any remaining legal line.
 */
class MediumAiStrategy : AiStrategy {
    override fun chooseLine(gameState: GameState, playerId: PlayerId, random: Random): Line {
        val board = gameState.board

        val capturing = BoardAnalyzer.capturingLines(board)
        if (capturing.isNotEmpty()) {
            return capturing[random.nextInt(capturing.size)]
        }

        val safe = BoardAnalyzer.safeLines(board)
        if (safe.isNotEmpty()) {
            return safe[random.nextInt(safe.size)]
        }

        val available = BoardAnalyzer.availableLines(board)
        check(available.isNotEmpty()) { "MediumAiStrategy asked to move with no available lines" }
        return available[random.nextInt(available.size)]
    }
}
