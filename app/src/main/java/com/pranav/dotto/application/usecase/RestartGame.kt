package com.pranav.dotto.application.usecase

import com.pranav.dotto.application.state.SetupConfig

/**
 * Marker use case for "go back to Setup with the previous config pre-filled".
 * Kept explicit (rather than the ViewModel just resetting a flag) so the
 * intent is discoverable and testable on its own.
 */
class RestartGame {
    operator fun invoke(previousConfig: SetupConfig): SetupConfig = previousConfig
}
