package com.pranav.dotto.presentation.theme

import androidx.compose.ui.graphics.Color
import com.pranav.dotto.domain.model.PlayerColorToken

/**
 * Maps the domain's abstract [PlayerColorToken] to actual Compose colors.
 * This is the ONLY place player colors are hard-coded, per the "no hard-coded
 * colors everywhere" requirement — everything else asks this object.
 */
object PlayerPresentation {
    fun colorFor(token: PlayerColorToken): Color = when (token) {
        PlayerColorToken.PRIMARY -> DottoPrimary
        PlayerColorToken.SECONDARY -> DottoSecondary
        PlayerColorToken.TERTIARY -> DottoTertiary
        PlayerColorToken.QUATERNARY -> DottoQuaternary
    }
}
