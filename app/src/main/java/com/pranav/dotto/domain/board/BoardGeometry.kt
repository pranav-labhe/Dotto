package com.pranav.dotto.domain.board

import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.Line

/**
 * Pure, stateless geometry calculations over a [BoardConfig]: what lines
 * exist, which lines bound a box, which boxes border a line. This is the
 * single source of truth for board topology so the engine and the AI never
 * duplicate adjacency logic.
 */
object BoardGeometry {

    fun allLines(config: BoardConfig): Set<Line> {
        val lines = LinkedHashSet<Line>(config.totalLines)
        for (row in 0 until config.dotRows) {
            for (col in 0 until config.dotColumns - 1) {
                lines += Line.Horizontal(row, col)
            }
        }
        for (row in 0 until config.dotRows - 1) {
            for (col in 0 until config.dotColumns) {
                lines += Line.Vertical(row, col)
            }
        }
        return lines
    }

    fun allBoxes(config: BoardConfig): List<BoxCoordinate> =
        (0 until config.boxRows).flatMap { r ->
            (0 until config.boxColumns).map { c -> BoxCoordinate(r, c) }
        }

    fun isLineWithinBounds(config: BoardConfig, line: Line): Boolean = when (line) {
        is Line.Horizontal ->
            line.row in 0 until config.dotRows && line.column in 0 until config.dotColumns - 1
        is Line.Vertical ->
            line.row in 0 until config.dotRows - 1 && line.column in 0 until config.dotColumns
    }

    /** The (up to 4) lines that bound a given box. */
    fun linesForBox(box: BoxCoordinate): Set<Line> = setOf(
        Line.Horizontal(box.row, box.column),          // top
        Line.Horizontal(box.row + 1, box.column),      // bottom
        Line.Vertical(box.row, box.column),             // left
        Line.Vertical(box.row, box.column + 1)          // right
    )

    /**
     * The (up to 2) boxes that a given line borders. A horizontal line on the
     * top/bottom edge of the grid borders only one box; same for vertical
     * lines on the left/right edge.
     */
    fun boxesForLine(config: BoardConfig, line: Line): List<BoxCoordinate> = when (line) {
        is Line.Horizontal -> buildList {
            // box above this line (row-1, column) and box below (row, column)
            if (line.row - 1 in 0 until config.boxRows && line.column in 0 until config.boxColumns) {
                add(BoxCoordinate(line.row - 1, line.column))
            }
            if (line.row in 0 until config.boxRows && line.column in 0 until config.boxColumns) {
                add(BoxCoordinate(line.row, line.column))
            }
        }
        is Line.Vertical -> buildList {
            // box to the left (row, column-1) and box to the right (row, column)
            if (line.row in 0 until config.boxRows && line.column - 1 in 0 until config.boxColumns) {
                add(BoxCoordinate(line.row, line.column - 1))
            }
            if (line.row in 0 until config.boxRows && line.column in 0 until config.boxColumns) {
                add(BoxCoordinate(line.row, line.column))
            }
        }
    }

    fun isBoxComplete(box: BoxCoordinate, drawnLines: Set<com.pranav.dotto.domain.model.Line>): Boolean =
        linesForBox(box).all { it in drawnLines }

    /** Number of the box's 4 sides already drawn (0..4). Used heavily by AI heuristics. */
    fun sidesDrawn(box: BoxCoordinate, drawnLines: Set<Line>): Int =
        linesForBox(box).count { it in drawnLines }
}
