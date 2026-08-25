package com.pranav.dotto.domain.model

/**
 * Who/what is behind a player. Kept open-ended so future controller types
 * (NEARBY, ONLINE) can be added without touching the engine.
 */
enum class PlayerType {
    HUMAN,
    AI
}
