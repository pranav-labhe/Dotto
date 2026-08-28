package com.pranav.dotto.presentation.components

import com.pranav.dotto.domain.board.BoardConfig
import com.pranav.dotto.domain.model.Line

/**
 * Converts between logical board coordinates and screen pixel space, and
 * performs touch hit-testing. This is the ONLY place pixel math exists —
 * the domain layer never sees it, and the renderer/touch-handling Composable
 * just delegates here.
 */
class BoardGeometryMapper(
    private val config: BoardConfig,
    private val canvasWidthPx: Float,
    private val canvasHeightPx: Float,
    private val paddingPx: Float,
    private val isLargeLevel: Boolean = false
) {
    private val usableWidth = canvasWidthPx - paddingPx * 2
    private val usableHeight = canvasHeightPx - paddingPx * 2

    // For levels <= 4, board fits screen. 
    // For levels >= 5, cell size is locked to show approx 6 cells with a bleed hint.
    val cellWidth: Float = if (isLargeLevel) {
        usableWidth / 6.2f 
    } else {
        usableWidth / (config.dotColumns - 1)
    }
    
    val cellHeight: Float = if (isLargeLevel) {
        usableHeight / 6.2f
    } else {
        usableHeight / (config.dotRows - 1)
    }

    fun dotX(column: Int): Float = paddingPx + column * cellWidth
    fun dotY(row: Int): Float = paddingPx + row * cellHeight

    /**
     * Finds the nearest line to a tap, within [toleranceFraction] of a cell's
     * length, so users don't need pixel-perfect precision.
     */
    fun hitTestLine(tapX: Float, tapY: Float, toleranceFraction: Float = 0.32f): Line? {
        val tolerance = minOf(cellWidth, cellHeight) * toleranceFraction
        var best: Line? = null
        var bestDistance = Float.MAX_VALUE

        // Horizontal lines: midpoint between two adjacent dots on the same row.
        for (row in 0 until config.dotRows) {
            for (col in 0 until config.dotColumns - 1) {
                val midX = (dotX(col) + dotX(col + 1)) / 2f
                val midY = dotY(row)
                val dx = tapX - midX
                val alongAxisOk = tapX in (dotX(col) - tolerance)..(dotX(col + 1) + tolerance)
                val dy = tapY - midY
                if (alongAxisOk && kotlin.math.abs(dy) <= tolerance) {
                    val dist = dx * dx + dy * dy
                    if (dist < bestDistance) {
                        bestDistance = dist
                        best = Line.Horizontal(row, col)
                    }
                }
            }
        }

        // Vertical lines: midpoint between two adjacent dots on the same column.
        for (row in 0 until config.dotRows - 1) {
            for (col in 0 until config.dotColumns) {
                val midX = dotX(col)
                val midY = (dotY(row) + dotY(row + 1)) / 2f
                val dx = tapX - midX
                val alongAxisOk = tapY in (dotY(row) - tolerance)..(dotY(row + 1) + tolerance)
                val dy = tapY - midY
                if (alongAxisOk && kotlin.math.abs(dx) <= tolerance) {
                    val dist = dx * dx + dy * dy
                    if (dist < bestDistance) {
                        bestDistance = dist
                        best = Line.Vertical(row, col)
                    }
                }
            }
        }
        return best
    }
}
