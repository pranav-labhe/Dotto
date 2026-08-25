package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.engine.GameEngine
import com.pranav.dotto.domain.engine.GameEngineImpl
import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.GameStatus
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId

/**
 * Minimax with alpha-beta pruning, aware that completing a box grants the
 * SAME player another ply (so the recursion doesn't naively alternate
 * players every level — it alternates only when a move does NOT extend the
 * mover's turn, matching Dotto's actual turn rules).
 *
 * Depth is measured in individual moves (not "rounds"), and is intentionally
 * kept shallow by the caller ([HardAiStrategy]) based on board size so this
 * never blocks the UI thread for long.
 */
class MinimaxSearchStrategy(
    private val engine: GameEngine = GameEngineImpl(),
    private val evaluator: MoveEvaluator = HeuristicMoveEvaluator()
) : SearchStrategy {

    override fun bestMove(
        gameState: GameState,
        playerId: PlayerId,
        candidates: List<Line>,
        maxDepth: Int
    ): Line {
        val opponentId = gameState.players.first { it.id != playerId }.id
        var bestScore = Int.MIN_VALUE
        var bestLine = candidates.first()

        var alpha = Int.MIN_VALUE
        var beta = Int.MAX_VALUE

        for (line in candidates) {
            val result = engine.makeMove(gameState, GameMove(playerId, line))
            if (!result.accepted) continue

            val nextIsSamePlayer = result.extraTurn
            val score = minimax(
                state = result.newState,
                mySide = playerId,
                opponentSide = opponentId,
                depth = maxDepth - 1,
                maximizing = nextIsSamePlayer, // if we go again, we're still maximizing
                alpha = alpha,
                beta = beta
            )
            if (score > bestScore) {
                bestScore = score
                bestLine = line
            }
            alpha = maxOf(alpha, bestScore)
        }
        return bestLine
    }

    private fun minimax(
        state: GameState,
        mySide: PlayerId,
        opponentSide: PlayerId,
        depth: Int,
        maximizing: Boolean,
        alpha: Int,
        beta: Int
    ): Int {
        if (depth <= 0 || state.status is GameStatus.Finished) {
            return evaluator.evaluate(state.board, mySide, opponentSide)
        }
        val mover = state.currentPlayerId ?: return evaluator.evaluate(state.board, mySide, opponentSide)
        val moves = engine.getValidMoves(state)
        if (moves.isEmpty()) {
            return evaluator.evaluate(state.board, mySide, opponentSide)
        }

        var a = alpha
        var b = beta

        return if (maximizing) {
            var value = Int.MIN_VALUE
            for (line in moves) {
                val result = engine.makeMove(state, GameMove(mover, line))
                if (!result.accepted) continue
                val nextMaximizing = result.extraTurn // same player (mySide) continues
                value = maxOf(
                    value,
                    minimax(result.newState, mySide, opponentSide, depth - 1, nextMaximizing, a, b)
                )
                a = maxOf(a, value)
                if (b <= a) break
            }
            if (value == Int.MIN_VALUE) evaluator.evaluate(state.board, mySide, opponentSide) else value
        } else {
            var value = Int.MAX_VALUE
            for (line in moves) {
                val result = engine.makeMove(state, GameMove(mover, line))
                if (!result.accepted) continue
                val nextMaximizing = !result.extraTurn // opponent continues => still minimizing; if opponent's extra turn ends, mySide moves next => maximizing
                value = minOf(
                    value,
                    minimax(result.newState, mySide, opponentSide, depth - 1, nextMaximizing, a, b)
                )
                b = minOf(b, value)
                if (b <= a) break
            }
            if (value == Int.MAX_VALUE) evaluator.evaluate(state.board, mySide, opponentSide) else value
        }
    }
}
