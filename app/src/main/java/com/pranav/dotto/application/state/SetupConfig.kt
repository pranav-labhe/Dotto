package com.pranav.dotto.application.state

import com.pranav.dotto.domain.ai.AiDifficulty
import com.pranav.dotto.domain.board.BoardConfig

/** User-chosen configuration from the Setup screen. */
data class SetupConfig(
    val humanName: String = "Player",
    val gridDots: Int = 5,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val levelNumber: Int = 3 // Defaults to Level 3 (5x5)
) {
    val boardConfig: BoardConfig get() = BoardConfig.square(gridDots)

    companion object {
        val AVAILABLE_GRID_SIZES = listOf(3, 4, 5, 6, 7)
        
        fun forLevel(level: Int): SetupConfig {
            val dots = level + 2
            return SetupConfig(gridDots = dots, levelNumber = level)
        }
    }
}
