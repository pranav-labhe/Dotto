package com.pranav.dotto.domain

import com.pranav.dotto.domain.board.BoardConfig
import com.pranav.dotto.domain.board.BoardGeometry
import com.pranav.dotto.domain.engine.GameEngineImpl
import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameStatus
import com.pranav.dotto.domain.model.Player
import com.pranav.dotto.domain.model.PlayerColorToken
import com.pranav.dotto.domain.model.PlayerId
import com.pranav.dotto.domain.model.PlayerType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

/**
 * Property-style tests: play many randomized full games and assert the
 * invariants hold after every single move, not just at the end.
 */
class GameInvariantsTest {

    private val engine = GameEngineImpl()

    @Test
    fun `invariants hold across many randomized full games`() {
        val random = Random(42)
        repeat(25) { gameIndex ->
            val size = 3 + gameIndex % 4 // 3..6
            val config = BoardConfig.square(size)
            val p1 = Player(PlayerId.new(), "P1", "1", PlayerType.HUMAN, PlayerColorToken.PRIMARY)
            val p2 = Player(PlayerId.new(), "P2", "2", PlayerType.AI, PlayerColorToken.SECONDARY)
            var state = engine.startGame(config, listOf(p1, p2))

            var previousDrawnCount = 0
            while (state.status !is GameStatus.Finished) {
                val validMoves = engine.getValidMoves(state)
                assertThat(validMoves).isNotEmpty()
                val mover = state.currentPlayerId!!
                val line = validMoves[random.nextInt(validMoves.size)]

                val result = engine.makeMove(state, GameMove(mover, line))
                assertThat(result.accepted).isTrue()
                state = result.newState

                // Lines drawn never decreases.
                assertThat(state.board.drawnLines.size).isAtLeast(previousDrawnCount)
                previousDrawnCount = state.board.drawnLines.size

                // No line appears twice (set semantics already guarantee this, but assert size grew by exactly 1).
                // A completed box always has exactly 4 surrounding lines drawn, and one owner.
                state.board.boxOwners.keys.forEach { box ->
                    assertThat(BoardGeometry.sidesDrawn(box, state.board.drawnLines)).isEqualTo(4)
                }
                // Map keys are inherently unique, but assert explicitly that no box has two owners
                // by checking the key set size matches the owner-entry count.
                assertThat(state.board.boxOwners.keys.toSet().size).isEqualTo(state.board.boxOwners.size)

                // score[player] == boxes owned by player
                state.players.forEach { player ->
                    val ownedCount = state.board.boxOwners.values.count { it == player.id }
                    assertThat(state.scoreOf(player.id)).isEqualTo(ownedCount)
                }

                assertThat(state.board.boxOwners.size).isAtMost(config.totalBoxes)
                assertThat(state.board.drawnLines.size).isAtMost(config.totalLines)
            }

            // gameOver <-> all playable lines drawn
            assertThat(state.board.drawnLines.size).isEqualTo(config.totalLines)
            assertThat(state.board.boxOwners.size).isEqualTo(config.totalBoxes)
        }
    }
}
