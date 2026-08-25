package com.pranav.dotto.domain.board

import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerId

/**
 * Immutable snapshot of the physical board: which lines are drawn and who
 * owns which completed box.
 */
data class BoardState(
    val config: BoardConfig,
    val lineOwners: Map<Line, PlayerId> = emptyMap(),
    val boxOwners: Map<BoxCoordinate, PlayerId> = emptyMap()
) {
    /** Helper for logic that only cares if a line is drawn, not by whom. */
    val drawnLines: Set<Line> get() = lineOwners.keys

    fun isLineDrawn(line: Line): Boolean = line in lineOwners

    fun withLineDrawn(line: Line, playerId: PlayerId): BoardState =
        copy(lineOwners = lineOwners + (line to playerId))

    fun withBoxesOwned(boxes: Map<BoxCoordinate, PlayerId>): BoardState =
        copy(boxOwners = boxOwners + boxes)

    val isFull: Boolean get() = lineOwners.size >= config.totalLines
}
