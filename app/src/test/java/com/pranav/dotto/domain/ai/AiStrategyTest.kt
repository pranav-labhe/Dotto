package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.board.BoardConfig
import com.pranav.dotto.domain.engine.GameEngineImpl
import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.Player
import com.pranav.dotto.domain.model.PlayerColorToken
import com.pranav.dotto.domain.model.PlayerId
import com.pranav.dotto.domain.model.PlayerType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class AiStrategyTest {

    private val engine = GameEngineImpl()
    private val human = Player(PlayerId.new(), "P1", "1", PlayerType.HUMAN, PlayerColorToken.PRIMARY)
    private val ai = Player(PlayerId.new(), "Dotto", "D", PlayerType.AI, PlayerColorToken.SECONDARY)

    @Test
    fun `easy AI always returns a valid, undrawn line`() {
        val strategy = EasyAiStrategy()
        val state = engine.startGame(BoardConfig.square(4), listOf(human, ai))
        val random = Random(1)
        repeat(20) {
            val line = strategy.chooseLine(state, ai.id, random)
            assertThat(engine.validateMove(state, line)).isTrue()
        }
    }

    @Test
    fun `easy AI is deterministic for a fixed seed`() {
        val strategy = EasyAiStrategy()
        val state = engine.startGame(BoardConfig.square(4), listOf(human, ai))
        val lineA = strategy.chooseLine(state, ai.id, Random(99))
        val lineB = strategy.chooseLine(state, ai.id, Random(99))
        assertThat(lineA).isEqualTo(lineB)
    }

    @Test
    fun `medium AI takes an immediate box completion when available`() {
        val strategy = MediumAiStrategy()
        val config = BoardConfig.square(2) // single box
        var state = engine.startGame(config, listOf(ai, human))
        // Draw 3 of the 4 sides, leaving exactly one capturing move.
        val setup = listOf(Line.Horizontal(0, 0), Line.Horizontal(1, 0), Line.Vertical(0, 0))
        var mover = ai.id
        for (line in setup) {
            val res = engine.makeMove(state, GameMove(mover, line))
            state = res.newState
            mover = state.currentPlayerId ?: mover
        }
        val chosen = strategy.chooseLine(state, mover, Random(7))
        assertThat(chosen).isEqualTo(Line.Vertical(0, 1))
    }

    @Test
    fun `medium AI never picks an occupied line`() {
        val strategy = MediumAiStrategy()
        var state = engine.startGame(BoardConfig.square(3), listOf(ai, human))
        val random = Random(5)
        repeat(15) {
            if (state.status is com.pranav.dotto.domain.model.GameStatus.Finished) return@repeat
            val mover = state.currentPlayerId ?: return@repeat
            val line = strategy.chooseLine(state, mover, random)
            assertThat(state.board.isLineDrawn(line)).isFalse()
            val result = engine.makeMove(state, GameMove(mover, line))
            assertThat(result.accepted).isTrue()
            state = result.newState
        }
    }

    @Test
    fun `hard AI takes immediate box completion when available`() {
        val strategy = HardAiStrategy()
        val config = BoardConfig.square(2)
        var state = engine.startGame(config, listOf(ai, human))
        val setup = listOf(Line.Horizontal(0, 0), Line.Horizontal(1, 0), Line.Vertical(0, 0))
        var mover = ai.id
        for (line in setup) {
            val res = engine.makeMove(state, GameMove(mover, line))
            state = res.newState
            mover = state.currentPlayerId ?: mover
        }
        val chosen = strategy.chooseLine(state, mover, Random(3))
        assertThat(chosen).isEqualTo(Line.Vertical(0, 1))
    }

    @Test
    fun `hard AI avoids handing away a free box when a safe move exists`() {
        val strategy = HardAiStrategy()
        // 4x4 dot board (3x3 boxes): draw only 2 of box(0,0)'s 4 sides, so no box
        // is capturable yet, but playing either of its remaining 2 sides would put
        // it at 3-sides (risky). Plenty of fully untouched, safe lines remain
        // elsewhere on this larger board.
        val config = BoardConfig.square(4)
        var state = engine.startGame(config, listOf(ai, human))
        val dangerSetup = listOf(Line.Horizontal(0, 0), Line.Vertical(0, 0))
        var mover = ai.id
        for (line in dangerSetup) {
            val res = engine.makeMove(state, GameMove(mover, line))
            state = res.newState
            mover = state.currentPlayerId ?: mover
        }
        assertThat(BoardAnalyzer.capturingLines(state.board)).isEmpty()
        assertThat(BoardAnalyzer.safeLines(state.board)).isNotEmpty()

        val chosen = strategy.chooseLine(state, mover, Random(11))
        assertThat(BoardAnalyzer.createsThirdSide(state.board, chosen)).isFalse()
    }

    @Test
    fun `all AI difficulties always produce a legal move across a full random game`() {
        AiDifficulty.entries.forEach { difficulty ->
            val strategy = AiStrategyFactory.create(difficulty)
            var state = engine.startGame(BoardConfig.square(3), listOf(ai, human))
            val random = Random(difficulty.ordinal + 100)
            while (state.status !is com.pranav.dotto.domain.model.GameStatus.Finished) {
                val mover = state.currentPlayerId!!
                val line = strategy.chooseLine(state, mover, random)
                val result = engine.makeMove(state, GameMove(mover, line))
                assertThat(result.accepted).isTrue()
                state = result.newState
            }
        }
    }
}
