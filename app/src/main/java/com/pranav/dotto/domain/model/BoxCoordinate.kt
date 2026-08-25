package com.pranav.dotto.domain.model

/**
 * Logical identity of a box, identified by its top-left dot.
 * For an N x M dot grid, valid boxes range row in [0, N-2], column in [0, M-2].
 */
data class BoxCoordinate(
    val row: Int,
    val column: Int
)
