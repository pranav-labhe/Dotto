package com.pranav.dotto.domain.model

sealed interface GameStatus {
    data object NotStarted : GameStatus
    data object InProgress : GameStatus
    data class Finished(val outcome: GameOutcome) : GameStatus
}

sealed interface GameOutcome {
    data class Win(val winnerId: PlayerId) : GameOutcome
    data object Draw : GameOutcome
}
