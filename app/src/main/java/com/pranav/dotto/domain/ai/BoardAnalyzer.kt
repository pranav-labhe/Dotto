package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.board.BoardGeometry
import com.pranav.dotto.domain.board.BoardState
import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.Line

/**
 * Shared read-only board analysis used by every AI strategy, so each
 * difficulty level reasons about the same primitives (which lines complete a
 * box right now, which lines would hand the opponent a box) instead of
 * re-deriving them ad hoc.
 */
object BoardAnalyzer {

    /** All currently-undrawn lines. */
    fun availableLines(board: BoardState): List<Line> =
        BoardGeometry.allLines(board.config).filterNot { board.isLineDrawn(it) }

    /** Lines that, if drawn, complete at least one box immediately. */
    fun capturingLines(board: BoardState): List<Line> =
        availableLines(board).filter { line -> completesAnyBox(board, line) }

    /**
     * Lines that are "safe": drawing them does not bring any box to 3 drawn
     * sides (which would let the opponent capture it next turn).
     */
    fun safeLines(board: BoardState): List<Line> =
        availableLines(board).filter { line -> !createsThirdSide(board, line) }

    fun completesAnyBox(board: BoardState, line: Line): Boolean =
        BoardGeometry.boxesForLine(board.config, line)
            .filter { it !in board.boxOwners }
            .any { box -> BoardGeometry.sidesDrawn(box, board.drawnLines + line) == 4 }

    /** True if drawing [line] leaves any bordered, unowned box with exactly 3 sides drawn. */
    fun createsThirdSide(board: BoardState, line: Line): Boolean =
        BoardGeometry.boxesForLine(board.config, line)
            .filter { it !in board.boxOwners }
            .any { box -> BoardGeometry.sidesDrawn(box, board.drawnLines + line) == 3 }

    /** All unowned boxes currently sitting at exactly 3 drawn sides (capturable next move). */
    fun boxesWithThreeSides(board: BoardState): List<BoxCoordinate> =
        BoardGeometry.allBoxes(board.config)
            .filter { it !in board.boxOwners }
            .filter { box -> BoardGeometry.sidesDrawn(box, board.drawnLines) == 3 }
}
