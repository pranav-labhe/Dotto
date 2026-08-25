package com.pranav.dotto.domain.board

import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId

/**
 * Immutable snapshot of the physical board: which lines are drawn and who
 * owns which completed box. All derived queries (valid lines, box adjacency)
 * are computed from [BoardConfig] rather than stored, so the state stays
 * small and trivially comparable/serializable for later persistence/replay.
 */
data class BoardState(
    val config: BoardConfig,
    val drawnLines: Set<Line> = emptySet(),
    val boxOwners: Map<BoxCoordinate, PlayerId> = emptyMap()
) {
    fun isLineDrawn(line: Line): Boolean = line in drawnLines

    fun withLineDrawn(line: Line): BoardState =
        copy(drawnLines = drawnLines + line)

    fun withBoxesOwned(boxes: Map<BoxCoordinate, PlayerId>): BoardState =
        copy(boxOwners = boxOwners + boxes)

    val isFull: Boolean get() = drawnLines.size >= config.totalLines
}
