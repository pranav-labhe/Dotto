package com.pranav.dotto.domain.rules

import com.pranav.dotto.domain.board.BoardConfig
import com.pranav.dotto.domain.board.BoardGeometry
import com.pranav.dotto.domain.board.BoardState
import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.Line

/**
 * The standard Dots-and-Boxes rules: a move is legal if the line is within
 * the grid and not already drawn; completing one or more boxes on a move
 * grants exactly one extra turn (regardless of how many boxes were
 * completed); the game ends when every line has been drawn.
 */
class ClassicDottoRules(
    private val config: BoardConfig
) : GameRules {

    override fun isMoveLegal(board: BoardState, line: Line): Boolean =
        BoardGeometry.isLineWithinBounds(config, line) && !board.isLineDrawn(line)

    override fun boxesCompletedBy(board: BoardState, line: Line): List<BoxCoordinate> {
        val linesAfterMove = board.drawnLines + line
        return BoardGeometry.boxesForLine(config, line)
            .filter { box -> box !in board.boxOwners }
            .filter { box -> BoardGeometry.isBoxComplete(box, linesAfterMove) }
    }

    override fun grantsExtraTurn(completedBoxCount: Int): Boolean = completedBoxCount > 0

    override fun isGameComplete(config: BoardConfig, board: BoardState): Boolean =
        board.drawnLines.size >= config.totalLines
}
