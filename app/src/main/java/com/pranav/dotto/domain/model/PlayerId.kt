package com.pranav.dotto.domain.model

import java.util.UUID

/**
 * Stable, unique identity for a player. Never derived from a display name,
 * so renaming a player never changes identity (important once persistence,
 * stats, and multiplayer identity come into play).
 */
@JvmInline
value class PlayerId(val value: String) {
    companion object {
        fun new(): PlayerId = PlayerId(UUID.randomUUID().toString())
    }
}
