package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.board.BoardState
import com.pranav.dotto.domain.model.PlayerId

/**
 * Scores a board position from [playerId]'s perspective. Kept as its own
 * interface so search strategies (minimax, future MCTS, etc.) can be mixed
 * and matched with different evaluation heuristics.
 */
interface MoveEvaluator {
    fun evaluate(board: BoardState, playerId: PlayerId, opponentId: PlayerId): Int
}

/**
 * score = (my captured boxes - opponent captured boxes) * 10,
 * minus a small penalty per box currently sitting at 3 sides (since those
 * are up for grabs by whoever moves next), which nudges the search away from
 * positions that hand over easy captures.
 */
class HeuristicMoveEvaluator : MoveEvaluator {
    override fun evaluate(board: BoardState, playerId: PlayerId, opponentId: PlayerId): Int {
        val myBoxes = board.boxOwners.values.count { it == playerId }
        val opponentBoxes = board.boxOwners.values.count { it == opponentId }
        val scoreDiff = (myBoxes - opponentBoxes) * 10
        val exposedBoxes = BoardAnalyzer.boxesWithThreeSides(board).size
        return scoreDiff - exposedBoxes
    }
}
