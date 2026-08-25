package com.pranav.dotto.domain.board

/**
 * Configurable grid size, expressed in DOTS (not boxes).
 * boxRows = dotRows - 1, boxColumns = dotColumns - 1.
 *
 * Kept as a small immutable value so future variants (non-square boards,
 * different rule sets keyed by size) can branch on it without the engine
 * caring about UI concerns.
 */
data class BoardConfig(
    val dotRows: Int,
    val dotColumns: Int
) {
    init {
        require(dotRows >= 2) { "dotRows must be >= 2, was $dotRows" }
        require(dotColumns >= 2) { "dotColumns must be >= 2, was $dotColumns" }
    }

    val boxRows: Int get() = dotRows - 1
    val boxColumns: Int get() = dotColumns - 1
    val totalBoxes: Int get() = boxRows * boxColumns

    /** Total number of distinct horizontal + vertical lines possible on this grid. */
    val totalLines: Int get() {
        val horizontal = dotRows * (dotColumns - 1)
        val vertical = (dotRows - 1) * dotColumns
        return horizontal + vertical
    }

    companion object {
        fun square(dots: Int): BoardConfig = BoardConfig(dots, dots)
    }
}
