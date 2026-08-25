package com.pranav.dotto.domain.ai

/** Central place mapping difficulty -> strategy implementation. */
object AiStrategyFactory {
    fun create(difficulty: AiDifficulty): AiStrategy = when (difficulty) {
        AiDifficulty.EASY -> EasyAiStrategy()
        AiDifficulty.MEDIUM -> MediumAiStrategy()
        AiDifficulty.HARD -> HardAiStrategy()
    }
}
