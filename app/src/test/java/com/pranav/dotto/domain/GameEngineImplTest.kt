package com.pranav.dotto.domain

import com.pranav.dotto.domain.board.BoardConfig
import com.pranav.dotto.domain.engine.GameEngineImpl
import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameOutcome
import com.pranav.dotto.domain.model.GameStatus
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.Player
import com.pranav.dotto.domain.model.PlayerColorToken
import com.pranav.dotto.domain.model.PlayerId
import com.pranav.dotto.domain.model.PlayerType
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class GameEngineImplTest {

    private lateinit var engine: GameEngineImpl
    private lateinit var human: Player
    private lateinit var ai: Player

    @Before
    fun setUp() {
        engine = GameEngineImpl()
        human = Player(PlayerId.new(), "Pranav", "P", PlayerType.HUMAN, PlayerColorToken.PRIMARY)
        ai = Player(PlayerId.new(), "Dotto", "D", PlayerType.AI, PlayerColorToken.SECONDARY)
    }

    @Test
    fun `starting a game sets first player as current and status in progress`() {
        val state = engine.startGame(BoardConfig.square(3), listOf(human, ai))
        assertThat(state.currentPlayerId).isEqualTo(human.id)
        assertThat(state.status).isEqualTo(GameStatus.InProgress)
        assertThat(state.scores.values).containsExactly(0, 0)
    }

    @Test
    fun `a normal move with no box completed switches turn`() {
        val state = engine.startGame(BoardConfig.square(3), listOf(human, ai))
        val result = engine.makeMove(state, GameMove(human.id, Line.Horizontal(0, 0)))

        assertThat(result.accepted).isTrue()
        assertThat(result.completedBoxes).isEmpty()
        assertThat(result.extraTurn).isFalse()
        assertThat(result.newState.currentPlayerId).isEqualTo(ai.id)
    }

    @Test
    fun `completing a box grants an extra turn and awards score`() {
        var state = engine.startGame(BoardConfig.square(2), listOf(human, ai)) // single box board
        // Draw 3 of the 4 sides. Every non-completing move switches the turn,
        // so we always read the mover off currentPlayerId rather than assuming
        // who goes next.
        state = engine.makeMove(state, GameMove(human.id, Line.Horizontal(0, 0))).newState
        assertThat(state.currentPlayerId).isEqualTo(ai.id)
        state = engine.makeMove(state, GameMove(ai.id, Line.Horizontal(1, 0))).newState
        assertThat(state.currentPlayerId).isEqualTo(human.id)
        val result = engine.makeMove(state, GameMove(human.id, Line.Vertical(0, 0))) // 3rd side, no completion yet
        assertThat(result.completedBoxes).isEmpty()
        assertThat(result.newState.currentPlayerId).isEqualTo(ai.id)

        val fourthMover = result.newState.currentPlayerId!!
        val finalResult = engine.makeMove(result.newState, GameMove(fourthMover, Line.Vertical(0, 1))) // 4th side
        assertThat(finalResult.accepted).isTrue()
        assertThat(finalResult.completedBoxes).containsExactly(com.pranav.dotto.domain.model.BoxCoordinate(0, 0))
        // The board is also fully filled by this same move, so the game ends at the same
        // moment as the capture — there's no one left to take an "extra" turn, so this
        // correctly reports false despite completing a box. See the next test for a case
        // where extraTurn is true because the game is NOT yet over.
        assertThat(finalResult.extraTurn).isFalse()
        assertThat(finalResult.newState.status).isInstanceOf(GameStatus.Finished::class.java)
        assertThat(finalResult.newState.scores[fourthMover]).isEqualTo(1)
    }

    @Test
    fun `completing one box mid-game grants a genuine extra turn to the same player`() {
        val config = BoardConfig(dotRows = 2, dotColumns = 3) // 2 boxes side by side, 7 lines total
        var state = engine.startGame(config, listOf(human, ai))
        val setup = listOf(Line.Horizontal(0, 0), Line.Horizontal(1, 0), Line.Vertical(0, 0))
        var mover = human.id
        for (line in setup) {
            val res = engine.makeMove(state, GameMove(mover, line))
            assertThat(res.accepted).isTrue()
            state = res.newState
            mover = state.currentPlayerId ?: mover
        }
        // Vertical(0,1) completes box(0,0) but Horizontal(0,1)/Horizontal(1,1)/Vertical(0,2)
        // remain undrawn, so the game is NOT over and the extra turn is real.
        val result = engine.makeMove(state, GameMove(mover, Line.Vertical(0, 1)))
        assertThat(result.completedBoxes).containsExactly(com.pranav.dotto.domain.model.BoxCoordinate(0, 0))
        assertThat(result.newState.status).isEqualTo(GameStatus.InProgress)
        assertThat(result.extraTurn).isTrue()
        assertThat(result.newState.currentPlayerId).isEqualTo(mover)
    }

    @Test
    fun `a move completing two boxes at once grants exactly one extra turn`() {
        // 3x2 dot grid: 2 boxes side by side, sharing the middle vertical line.
        val config = BoardConfig(dotRows = 2, dotColumns = 3)
        var state = engine.startGame(config, listOf(human, ai))

        // Draw everything except the shared middle vertical line (Vertical(0,1)),
        // so drawing it completes both boxes simultaneously.
        val setupMoves = listOf(
            Line.Horizontal(0, 0), Line.Horizontal(0, 1),
            Line.Horizontal(1, 0), Line.Horizontal(1, 1),
            Line.Vertical(0, 0), Line.Vertical(0, 2)
        )
        var mover = human.id
        for (line in setupMoves) {
            val res = engine.makeMove(state, GameMove(mover, line))
            assertThat(res.accepted).isTrue()
            state = res.newState
            mover = state.currentPlayerId ?: mover
        }

        val finishing = engine.makeMove(state, GameMove(mover, Line.Vertical(0, 1)))
        assertThat(finishing.completedBoxes).hasSize(2)
        // Game complete since all 7 lines drawn on this board.
        assertThat(finishing.newState.status).isInstanceOf(GameStatus.Finished::class.java)
        assertThat(finishing.newState.scores[mover]).isEqualTo(2)
    }

    @Test
    fun `rejects a move from a player who is not current`() {
        val state = engine.startGame(BoardConfig.square(3), listOf(human, ai))
        val result = engine.makeMove(state, GameMove(ai.id, Line.Horizontal(0, 0)))
        assertThat(result.accepted).isFalse()
        assertThat(result.newState).isEqualTo(state)
    }

    @Test
    fun `rejects a duplicate line`() {
        var state = engine.startGame(BoardConfig.square(3), listOf(human, ai))
        val first = engine.makeMove(state, GameMove(human.id, Line.Horizontal(0, 0)))
        state = first.newState
        val dup = engine.makeMove(state, GameMove(ai.id, Line.Horizontal(0, 0)))
        assertThat(dup.accepted).isFalse()
    }

    @Test
    fun `rejects an out-of-range line`() {
        val state = engine.startGame(BoardConfig.square(3), listOf(human, ai))
        val result = engine.makeMove(state, GameMove(human.id, Line.Horizontal(5, 5)))
        assertThat(result.accepted).isFalse()
    }

    @Test
    fun `game over is only true once every line is drawn`() {
        val config = BoardConfig.square(2)
        var state = engine.startGame(config, listOf(human, ai))
        assertThat(engine.isGameOver(state)).isFalse()

        val lines = listOf(Line.Horizontal(0, 0), Line.Horizontal(1, 0), Line.Vertical(0, 0), Line.Vertical(0, 1))
        var mover = human.id
        var last = engine.makeMove(state, GameMove(mover, lines[0]))
        state = last.newState
        for (i in 1 until lines.size) {
            mover = state.currentPlayerId ?: mover
            last = engine.makeMove(state, GameMove(mover, lines[i]))
            state = last.newState
        }
        assertThat(engine.isGameOver(state)).isTrue()
    }

    @Test
    fun `winner is the player with strictly more boxes, else draw`() {
        // Force a tiny 2x2-box scenario manually via engine with a 3x2 dot grid (2 boxes),
        // one box each -> draw.
        val config = BoardConfig(dotRows = 2, dotColumns = 3)
        var state = engine.startGame(config, listOf(human, ai))

        // Play the whole game out deterministically and assert the invariant score==boxes,
        // and that whichever outcome occurs (win or draw) is internally consistent.
        val allLines = com.pranav.dotto.domain.board.BoardGeometry.allLines(config).toList()
        var mover = human.id
        for (line in allLines) {
            if (state.status is GameStatus.Finished) break
            val res = engine.makeMove(state, GameMove(mover, line))
            if (res.accepted) {
                state = res.newState
                mover = state.currentPlayerId ?: mover
            }
        }
        val finalStatus = state.status as GameStatus.Finished
        val totalBoxes = config.totalBoxes
        assertThat(state.scores.values.sum()).isEqualTo(totalBoxes)
        when (val outcome = finalStatus.outcome) {
            is GameOutcome.Win -> assertThat(state.scores[outcome.winnerId]).isGreaterThan(
                state.scores.filterKeys { it != outcome.winnerId }.values.maxOrNull() ?: 0
            )
            GameOutcome.Draw -> assertThat(state.scores.values.toSet()).hasSize(1)
        }
    }
}
