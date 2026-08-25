package com.pranav.dotto.domain.rules

import com.pranav.dotto.domain.board.BoardConfig
import com.pranav.dotto.domain.board.BoardState
import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.Line

/**
 * Encapsulates "what the rules say" so the engine's control flow doesn't
 * hard-code Dotto-specific decisions inline. Only the classic Dots-and-Boxes
 * ruleset is implemented, but future variants (different scoring, different
 * extra-turn conditions) can be added as new [GameRules] implementations
 * without touching [com.pranav.dotto.domain.engine.GameEngine].
 */
interface GameRules {

    fun isMoveLegal(board: BoardState, line: Line): Boolean

    /** Boxes that become complete if [line] were drawn on top of [board]. */
    fun boxesCompletedBy(board: BoardState, line: Line): List<BoxCoordinate>

    /** Whether completing [completedBoxCount] boxes on one move earns another turn. */
    fun grantsExtraTurn(completedBoxCount: Int): Boolean

    fun isGameComplete(config: BoardConfig, board: BoardState): Boolean
}
