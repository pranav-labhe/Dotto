package com.pranav.dotto.domain.player

import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameState

/**
 * Supplies the next move for a player. The engine and application layer only
 * ever talk to this abstraction, never to "a human" or "an AI" directly —
 * that's what lets NearbyPlayerController / OnlinePlayerController slot in
 * later with zero changes to the game loop.
 */
interface PlayerController {
    suspend fun selectMove(gameState: GameState): GameMove
}
