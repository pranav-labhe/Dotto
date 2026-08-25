package com.pranav.dotto.domain.ai

/**
 * Data-only difficulty selector. Adding EXPERT/MASTER/ADAPTIVE later only
 * requires a new enum entry plus a case in [AiStrategyFactory] — nothing in
 * the UI or engine needs to change.
 */
enum class AiDifficulty {
    EASY,
    MEDIUM,
    HARD
}
