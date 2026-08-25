package com.pranav.dotto.domain.engine

import com.pranav.dotto.domain.board.BoardConfig
import com.pranav.dotto.domain.board.BoardGeometry
import com.pranav.dotto.domain.board.BoardState
import com.pranav.dotto.domain.events.GameEvent
import com.pranav.dotto.domain.model.BoxCoordinate
import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameOutcome
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.GameStatus
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.MoveResult
import com.pranav.dotto.domain.model.Player
import com.pranav.dotto.domain.model.PlayerId
import com.pranav.dotto.domain.rules.GameRules

/**
 * Default [GameEngine] implementation. Rules are injected via [rulesFactory]
 * rather than hard-coded, so future board/rule variants only require a new
 * [GameRules], never a change here.
 *
 * Turn order is round-robin over [GameState.players] in list order, which is
 * also what will let a future "N human players" mode work with zero engine
 * changes beyond building a longer player list.
 */
class GameEngineImpl(
    private val rulesFactory: (BoardConfig) -> GameRules = { com.pranav.dotto.domain.rules.ClassicDottoRules(it) }
) : GameEngine {

    override fun startGame(config: BoardConfig, players: List<Player>): GameState {
        require(players.isNotEmpty()) { "A game requires at least one player" }
        val board = BoardState(config = config)
        return GameState.newGame(board = board, players = players, firstPlayerId = players.first().id)
    }

    override fun validateMove(state: GameState, line: Line): Boolean {
        val rules = rulesFactory(state.board.config)
        return rules.isMoveLegal(state.board, line)
    }

    override fun getValidMoves(state: GameState): List<Line> =
        BoardGeometry.allLines(state.board.config).filterNot { state.board.isLineDrawn(it) }.toList()

    override fun getAffectedBoxes(state: GameState, line: Line): List<BoxCoordinate> =
        BoardGeometry.boxesForLine(state.board.config, line)

    override fun getCompletedBoxes(state: GameState, line: Line): List<BoxCoordinate> {
        val rules = rulesFactory(state.board.config)
        return rules.boxesCompletedBy(state.board, line)
    }

    override fun isGameOver(state: GameState): Boolean = state.status is GameStatus.Finished

    override fun getWinner(state: GameState): PlayerId? {
        val outcome = (state.status as? GameStatus.Finished)?.outcome ?: return null
        return (outcome as? GameOutcome.Win)?.winnerId
    }

    override fun makeMove(state: GameState, move: GameMove): MoveResult {
        val rules = rulesFactory(state.board.config)

        if (state.status is GameStatus.Finished) {
            return MoveResult.rejected(
                state,
                GameEvent.InvalidMove(move.playerId, move.line, "Game is already finished")
            )
        }
        if (state.currentPlayerId != move.playerId) {
            return MoveResult.rejected(
                state,
                GameEvent.InvalidMove(move.playerId, move.line, "It is not this player's turn")
            )
        }
        if (!rules.isMoveLegal(state.board, move.line)) {
            return MoveResult.rejected(
                state,
                GameEvent.InvalidMove(move.playerId, move.line, "Line is out of bounds or already drawn")
            )
        }

        val events = mutableListOf<GameEvent>()

        val completed = rules.boxesCompletedBy(state.board, move.line)
        val boardAfterLine = state.board.withLineDrawn(move.line, move.playerId)
        val boardAfterBoxes = if (completed.isNotEmpty()) {
            boardAfterLine.withBoxesOwned(completed.associateWith { move.playerId })
        } else {
            boardAfterLine
        }

        val newScores = state.scores.toMutableMap()
        if (completed.isNotEmpty()) {
            newScores[move.playerId] = (newScores[move.playerId] ?: 0) + completed.size
        }

        val newCompletedBoxes = state.completedBoxes + completed.associateWith { move.playerId }

        events += GameEvent.MoveMade(move.playerId, move.line, state.moveNumber + 1)
        completed.forEach { box ->
            events += GameEvent.BoxCompleted(box, move.playerId)
        }
        if (completed.isNotEmpty()) {
            events += GameEvent.ScoreChanged(move.playerId, newScores[move.playerId] ?: 0)
        }

        val gameComplete = rules.isGameComplete(state.board.config, boardAfterBoxes)
        val extraTurn = !gameComplete && rules.grantsExtraTurn(completed.size)

        val nextPlayerId: PlayerId? = when {
            gameComplete -> null
            extraTurn -> {
                events += GameEvent.ExtraTurnGranted(move.playerId)
                move.playerId
            }
            else -> nextPlayerAfter(state.players, move.playerId)
        }

        if (!gameComplete && !extraTurn && nextPlayerId != null) {
            events += GameEvent.TurnChanged(nextPlayerId)
        }

        val status: GameStatus = if (gameComplete) {
            val outcome = determineOutcome(newScores)
            events += GameEvent.GameFinished(outcome)
            GameStatus.Finished(outcome)
        } else {
            GameStatus.InProgress
        }

        val newState = state.copy(
            board = boardAfterBoxes,
            currentPlayerId = nextPlayerId,
            scores = newScores,
            completedBoxes = newCompletedBoxes,
            status = status,
            moveNumber = state.moveNumber + 1
        )

        return MoveResult(
            accepted = true,
            previousState = state,
            newState = newState,
            completedBoxes = completed,
            extraTurn = extraTurn,
            events = events
        )
    }

    private fun nextPlayerAfter(players: List<Player>, currentId: PlayerId): PlayerId {
        val index = players.indexOfFirst { it.id == currentId }
        val nextIndex = (index + 1) % players.size
        return players[nextIndex].id
    }

    private fun determineOutcome(scores: Map<PlayerId, Int>): GameOutcome {
        val maxScore = scores.values.maxOrNull() ?: 0
        val leaders = scores.filterValues { it == maxScore }.keys
        return if (leaders.size == 1) {
            GameOutcome.Win(leaders.first())
        } else {
            GameOutcome.Draw
        }
    }
}
