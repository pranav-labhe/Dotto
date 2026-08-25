package com.pranav.dotto.domain.model

import com.pranav.dotto.domain.board.BoardState

/**
 * The complete, immutable state of a Dotto game at a point in time.
 * Suitable as-is for UI rendering, AI evaluation, logging, and — later —
 * persistence/replay/network sync, since it contains nothing Android- or
 * transport-specific.
 */
data class GameState(
    val board: BoardState,
    val players: List<Player>,
    val currentPlayerId: PlayerId?,
    val scores: Map<PlayerId, Int>,
    val completedBoxes: Map<BoxCoordinate, PlayerId>,
    val status: GameStatus,
    val moveNumber: Int
) {
    val currentPlayer: Player?
        get() = players.firstOrNull { it.id == currentPlayerId }

    fun player(id: PlayerId): Player? = players.firstOrNull { it.id == id }

    fun scoreOf(id: PlayerId): Int = scores[id] ?: 0

    companion object {
        fun newGame(board: BoardState, players: List<Player>, firstPlayerId: PlayerId): GameState =
            GameState(
                board = board,
                players = players,
                currentPlayerId = firstPlayerId,
                scores = players.associate { it.id to 0 },
                completedBoxes = emptyMap(),
                status = GameStatus.InProgress,
                moveNumber = 0
            )
    }
}
