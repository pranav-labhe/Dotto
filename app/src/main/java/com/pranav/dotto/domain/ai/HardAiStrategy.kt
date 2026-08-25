package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.board.BoardState
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId
import kotlin.random.Random

/**
 * Strategic AI. Considers, in order:
 *  1. Immediate captures — if multiple exist, prefer ones on boxes that don't
 *     open a further chain reaction advantage for the opponent (approximated
 *     via the shared evaluator through search).
 *  2. Safe moves (don't hand the opponent a free box) — chosen via a bounded
 *     minimax/alpha-beta search so "safe" also accounts for who wins the
 *     resulting chains, not just the very next move.
 *  3. If no safe move exists, the least-bad "sacrifice": search picks the
 *     line that gives away the smallest/least valuable chain.
 *
 * Search depth scales down as the board grows so play never blocks the UI
 * thread (see [depthFor]).
 */
class HardAiStrategy(
    private val search: SearchStrategy = MinimaxSearchStrategy()
) : AiStrategy {

    override fun chooseLine(gameState: GameState, playerId: PlayerId, random: Random): Line {
        val board = gameState.board
        val depth = depthFor(board)

        val capturing = BoardAnalyzer.capturingLines(board)
        if (capturing.isNotEmpty()) {
            return if (capturing.size == 1) {
                capturing.first()
            } else {
                search.bestMove(gameState, playerId, capturing, depth)
            }
        }

        val safe = BoardAnalyzer.safeLines(board)
        val candidates = safe.ifEmpty { BoardAnalyzer.availableLines(board) }
        if (candidates.size == 1) return candidates.first()

        return search.bestMove(gameState, playerId, candidates, depth)
    }

    /** Smaller boards can afford deeper search without noticeable UI delay. */
    private fun depthFor(board: BoardState): Int {
        val totalBoxes = board.config.totalBoxes
        return when {
            totalBoxes <= 9 -> 4
            totalBoxes <= 16 -> 3
            totalBoxes <= 25 -> 2
            else -> 1
        }
    }
}
