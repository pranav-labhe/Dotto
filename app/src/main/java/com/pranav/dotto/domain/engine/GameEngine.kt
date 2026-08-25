package com.pranav.dotto.domain.engine

import com.pranav.dotto.domain.board.BoardConfig
import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.MoveResult
import com.pranav.dotto.domain.model.Player
import com.pranav.dotto.domain.model.PlayerId

/**
 * The authoritative rulebook of Dotto. Pure domain logic: no Android,
 * no coroutines-for-UI, no I/O. Given a GameState and a Move it deterministically
 * produces the next GameState — the same property that will let future replay
 * and network sync simply replay a move list through this same engine.
 */
interface GameEngine {

    fun startGame(config: BoardConfig, players: List<Player>): GameState

    /** Validates and applies [move] against [state], returning a full [MoveResult]. */
    fun makeMove(state: GameState, move: GameMove): MoveResult

    fun validateMove(state: GameState, line: Line): Boolean

    fun getValidMoves(state: GameState): List<Line>

    /** Boxes that would be affected (bordered) by drawing [line]. */
    fun getAffectedBoxes(state: GameState, line: Line): List<BoxCoordinate>

    /** Boxes that WOULD become newly complete if [line] were drawn right now. */
    fun getCompletedBoxes(state: GameState, line: Line): List<BoxCoordinate>

    fun isGameOver(state: GameState): Boolean

    fun getWinner(state: GameState): PlayerId?
}
