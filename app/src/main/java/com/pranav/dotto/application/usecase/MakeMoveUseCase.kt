package com.pranav.dotto.application.usecase

import com.pranav.dotto.domain.engine.GameEngine
import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.MoveResult

/** Thin wrapper so the ViewModel depends on a use case, not the raw engine, for symmetry with other use cases. */
class MakeMoveUseCase(
    private val engine: GameEngine
) {
    operator fun invoke(state: GameState, move: GameMove): MoveResult = engine.makeMove(state, move)
}
