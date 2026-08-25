package com.pranav.dotto.domain.model

/**
 * A line connects exactly two adjacent dots, either horizontally or
 * vertically. The (row, column) anchor is always the top-left / lesser dot
 * of the pair, which guarantees a single canonical representation per line
 * and therefore deterministic equality/hashing with zero extra work — two
 * logically identical lines can never be represented two different ways.
 *
 * Horizontal(row, column): connects dot(row, column) to dot(row, column+1)
 * Vertical(row, column):   connects dot(row, column) to dot(row+1, column)
 */
sealed interface Line {

    data class Horizontal(
        val row: Int,
        val column: Int
    ) : Line

    data class Vertical(
        val row: Int,
        val column: Int
    ) : Line
}
