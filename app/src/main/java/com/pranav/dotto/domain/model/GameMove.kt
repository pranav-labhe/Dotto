package com.pranav.dotto.domain.model

/** A single, atomic player action: draw one line. */
data class GameMove(
    val playerId: PlayerId,
    val line: Line
)
