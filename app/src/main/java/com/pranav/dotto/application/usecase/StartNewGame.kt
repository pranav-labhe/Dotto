package com.pranav.dotto.application.usecase

import com.pranav.dotto.application.state.SetupConfig
import com.pranav.dotto.domain.ai.AiStrategyFactory
import com.pranav.dotto.domain.engine.GameEngine
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.Player
import com.pranav.dotto.domain.model.PlayerColorToken
import com.pranav.dotto.domain.model.PlayerId
import com.pranav.dotto.domain.model.PlayerType
import com.pranav.dotto.domain.player.HumanPlayerController
import com.pranav.dotto.domain.ai.AiPlayerController

/**
 * Builds a fresh [GameState] plus the two [com.pranav.dotto.domain.player.PlayerController]s
 * for a Human-vs-AI game from setup config. Kept as a use case (rather than
 * inline in the ViewModel) so alternate game-start flows (resume, replay,
 * future PvP setup) can reuse the player/engine wiring pieces independently.
 */
class StartNewGame(
    private val engine: GameEngine
) {
    data class Result(
        val gameState: GameState,
        val humanController: HumanPlayerController,
        val aiController: AiPlayerController,
        val humanId: PlayerId,
        val aiId: PlayerId
    )

    operator fun invoke(config: SetupConfig): Result {
        val humanId = PlayerId("human_player")
        val aiId = PlayerId("dotto_ai")

        val human = Player(
            id = humanId,
            name = config.humanName.ifBlank { "Player" },
            initial = config.humanName.trim().take(1).ifBlank { "P" }.uppercase(),
            type = PlayerType.HUMAN,
            colorToken = PlayerColorToken.PRIMARY
        )
        val ai = Player(
            id = aiId,
            name = "Dotto",
            initial = "D",
            type = PlayerType.AI,
            colorToken = PlayerColorToken.SECONDARY
        )

        val gameState = engine.startGame(config.boardConfig, listOf(human, ai))

        val humanController = HumanPlayerController(humanId)
        val aiController = AiPlayerController(
            playerId = aiId,
            strategy = AiStrategyFactory.create(config.aiDifficulty)
        )

        return Result(gameState, humanController, aiController, humanId, aiId)
    }
}
