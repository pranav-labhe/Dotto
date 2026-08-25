package com.pranav.dotto.domain

import com.pranav.dotto.domain.board.BoardConfig
import com.pranav.dotto.domain.board.BoardGeometry
import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.Line
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BoardGeometryTest {

    @Test
    fun `3x3 dot grid has 4 boxes`() {
        val config = BoardConfig.square(3)
        assertThat(config.totalBoxes).isEqualTo(4)
        assertThat(BoardGeometry.allBoxes(config)).hasSize(4)
    }

    @Test
    fun `total lines matches formula for a 5x5 dot grid`() {
        val config = BoardConfig.square(5)
        // horizontal: 5 rows * 4 segments = 20; vertical: 4 rows * 5 segments = 20
        assertThat(config.totalLines).isEqualTo(40)
        assertThat(BoardGeometry.allLines(config)).hasSize(40)
    }

    @Test
    fun `line within bounds returns true for valid lines and false outside grid`() {
        val config = BoardConfig.square(3)
        assertThat(BoardGeometry.isLineWithinBounds(config, Line.Horizontal(0, 0))).isTrue()
        assertThat(BoardGeometry.isLineWithinBounds(config, Line.Horizontal(0, 2))).isFalse()
        assertThat(BoardGeometry.isLineWithinBounds(config, Line.Vertical(2, 0))).isFalse()
    }

    @Test
    fun `a corner box has exactly 2 bordering lines shared with neighbors and 4 total`() {
        val box = BoxCoordinate(0, 0)
        val lines = BoardGeometry.linesForBox(box)
        assertThat(lines).hasSize(4)
        assertThat(lines).containsExactly(
            Line.Horizontal(0, 0),
            Line.Horizontal(1, 0),
            Line.Vertical(0, 0),
            Line.Vertical(0, 1)
        )
    }

    @Test
    fun `an interior line borders exactly two boxes`() {
        val config = BoardConfig.square(4)
        val boxes = BoardGeometry.boxesForLine(config, Line.Vertical(1, 1))
        assertThat(boxes).hasSize(2)
    }

    @Test
    fun `an edge line borders exactly one box`() {
        val config = BoardConfig.square(3)
        val boxes = BoardGeometry.boxesForLine(config, Line.Horizontal(0, 0))
        assertThat(boxes).containsExactly(BoxCoordinate(0, 0))
    }

    @Test
    fun `box is complete only when all four sides drawn`() {
        val box = BoxCoordinate(0, 0)
        val threeSides = setOf(
            Line.Horizontal(0, 0), Line.Horizontal(1, 0), Line.Vertical(0, 0)
        )
        assertThat(BoardGeometry.isBoxComplete(box, threeSides)).isFalse()
        val fourSides = threeSides + Line.Vertical(0, 1)
        assertThat(BoardGeometry.isBoxComplete(box, fourSides)).isTrue()
    }
}
