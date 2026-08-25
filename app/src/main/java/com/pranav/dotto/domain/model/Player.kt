package com.pranav.dotto.domain.model

/**
 * Domain-level player. Deliberately free of anything Android/UI related.
 *
 * [initial] is what is rendered inside a captured box (e.g. "P", "D").
 * [colorToken] is an abstract presentation key (not a Compose Color) so the
 * domain never depends on UI color types; the presentation layer maps it.
 */
data class Player(
    val id: PlayerId,
    val name: String,
    val initial: String,
    val type: PlayerType,
    val colorToken: PlayerColorToken
)

/**
 * Abstract identifier for "which visual identity" a player uses.
 * The presentation layer decides what PRIMARY/SECONDARY actually look like.
 */
enum class PlayerColorToken {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    QUATERNARY
}
