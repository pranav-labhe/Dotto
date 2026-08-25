package com.pranav.dotto.domain.model

/**
 * Logical position of a dot in the grid. Zero-indexed.
 * Completely independent from screen/pixel coordinates.
 */
data class DotCoordinate(
    val row: Int,
    val column: Int
)
