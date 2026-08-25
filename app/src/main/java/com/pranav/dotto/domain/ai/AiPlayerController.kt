package com.pranav.dotto.domain.ai

import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.PlayerId
import com.pranav.dotto.domain.player.PlayerController
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Wraps an [AiStrategy] as a [PlayerController]. Adds a small artificial
 * "thinking" delay so AI moves never feel instant, and is fully suspend-based
 * so the caller can cancel it (e.g. new game started while AI is thinking)
 * via normal coroutine cancellation — no extra plumbing needed.
 */
class AiPlayerController(
    private val playerId: PlayerId,
    private val strategy: AiStrategy,
    private val random: Random = Random.Default,
    private val minThinkMillis: Long = 300,
    private val maxThinkMillis: Long = 800
) : PlayerController {

    override suspend fun selectMove(gameState: GameState): GameMove {
        val thinkTime = random.nextLong(minThinkMillis, maxThinkMillis + 1)
        delay(thinkTime)
        val line = strategy.chooseLine(gameState, playerId, random)
        return GameMove(playerId = playerId, line = line)
    }
}
