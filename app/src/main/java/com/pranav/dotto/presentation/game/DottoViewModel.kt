package com.pranav.dotto.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.dotto.application.state.DottoUiState
import com.pranav.dotto.application.state.SetupConfig
import com.pranav.dotto.application.usecase.MakeMoveUseCase
import com.pranav.dotto.application.usecase.StartNewGame
import com.pranav.dotto.domain.ai.AiPlayerController
import com.pranav.dotto.domain.engine.GameEngine
import com.pranav.dotto.domain.engine.GameEngineImpl
import com.pranav.dotto.domain.events.GameEvent
import com.pranav.dotto.domain.model.GameMove
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.GameStatus
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerType
import com.pranav.dotto.domain.player.HumanPlayerController
import com.pranav.dotto.presentation.sound.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Orchestrates the Setup -> Playing -> Result flow. Owns the current
 * [HumanPlayerController]/[AiPlayerController] pair and the coroutine that
 * drives the AI's turn, but contains NO game rule logic itself — all of that
 * lives in [GameEngine]. This is deliberate: the ViewModel is "wiring", the
 * domain layer is "rules".
 */
class DottoViewModel(
    private val engine: GameEngine = GameEngineImpl(),
    private val makeMove: MakeMoveUseCase = MakeMoveUseCase(engine),
    private val startNewGame: StartNewGame = StartNewGame(engine),
    private val soundManager: SoundManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<DottoUiState>(DottoUiState.Setup())
    val uiState: StateFlow<DottoUiState> = _uiState.asStateFlow()

    private var humanController: HumanPlayerController? = null
    private var aiController: AiPlayerController? = null
    private var aiJob: Job? = null

    private var lastSetupConfig: SetupConfig = SetupConfig()

    fun updateSetupConfig(transform: (SetupConfig) -> SetupConfig) {
        val current = _uiState.value
        if (current is DottoUiState.Setup) {
            _uiState.update { DottoUiState.Setup(transform(current.config)) }
        }
    }

    fun startGame() {
        val config = (_uiState.value as? DottoUiState.Setup)?.config ?: lastSetupConfig
        lastSetupConfig = config

        // Cancel any stale AI work from a previous game before wiring up the new one.
        cancelAiWork()

        val result = startNewGame(config)
        humanController = result.humanController
        aiController = result.aiController

        _uiState.value = DottoUiState.Playing(gameState = result.gameState)
        logEvent(GameEvent.GameStarted(result.gameState.players.map { it.id }))

        maybeTriggerAiTurn(result.gameState)
    }

    /** Called by the UI when the human taps a line. */
    fun onLineSelected(line: Line) {
        val playing = _uiState.value as? DottoUiState.Playing ?: return
        val state = playing.gameState
        if (playing.isAiThinking) return
        val current = state.currentPlayer ?: return
        if (current.type != PlayerType.HUMAN) return
        if (!engine.validateMove(state, line)) return

        humanController?.provideMove(line)
    }

    fun restart() {
        cancelAiWork()
        _uiState.value = DottoUiState.Setup(lastSetupConfig)
    }

    fun playAgainSameConfig() {
        _uiState.value = DottoUiState.Setup(lastSetupConfig)
        startGame()
    }

    fun startNextLevel() {
        val nextDots = (lastSetupConfig.gridDots + 1).coerceAtMost(7)
        lastSetupConfig = lastSetupConfig.copy(gridDots = nextDots)
        _uiState.value = DottoUiState.Setup(lastSetupConfig)
        startGame()
    }

    override fun onCleared() {
        super.onCleared()
        cancelAiWork()
    }

    private fun cancelAiWork() {
        aiJob?.cancel()
        aiJob = null
        humanController?.cancelPending()
    }

    /**
     * Drives the current player's controller (human or AI) to obtain a move,
     * applies it via the engine, updates state, and recurses for the next
     * player if the game isn't over. Runs in [viewModelScope] so it is
     * automatically cancelled if the ViewModel is cleared, and is explicitly
     * cancelled in [cancelAiWork] whenever a new game starts.
     */
    private fun maybeTriggerAiTurn(state: GameState) {
        if (state.status is GameStatus.Finished) {
            _uiState.value = DottoUiState.Result(state)
            soundManager?.playWin(lastSetupConfig.soundEnabled, lastSetupConfig.hapticEnabled)
            return
        }
        val current = state.currentPlayer ?: return
        if (current.type != PlayerType.HUMAN) {
            val ai = aiController ?: return
            _uiState.update { s -> (s as? DottoUiState.Playing)?.copy(isAiThinking = true) ?: s }
            aiJob = viewModelScope.launch {
                val move = ai.selectMove(state)
                applyMove(move)
            }
        } else {
            // Wait for human input asynchronously via HumanPlayerController.
            val human = humanController ?: return
            aiJob = viewModelScope.launch {
                val move = human.selectMove(state)
                applyMove(move)
            }
        }
    }

    private fun applyMove(move: GameMove) {
        val playing = _uiState.value as? DottoUiState.Playing ?: return
        val result = makeMove(playing.gameState, move)

        if (!result.accepted) {
            Log.w(TAG, "Rejected move from ${move.playerId}: ${move.line}")
            _uiState.update { s -> (s as? DottoUiState.Playing)?.copy(isAiThinking = false) ?: s }
            return
        }

        result.events.forEach(::logEvent)

        // Play sounds based on move result
        if (result.accepted) {
            if (result.completedBoxes.isNotEmpty()) {
                soundManager?.playScore(lastSetupConfig.soundEnabled, lastSetupConfig.hapticEnabled)
            } else {
                soundManager?.playMove(lastSetupConfig.soundEnabled, lastSetupConfig.hapticEnabled)
            }
        }

        _uiState.value = DottoUiState.Playing(
            gameState = result.newState,
            isAiThinking = false,
            lastMoveLine = move.line,
            recentlyCompletedBoxes = result.completedBoxes.toSet()
        )

        maybeTriggerAiTurn(result.newState)
    }

    private fun logEvent(event: GameEvent) {
        Log.d(TAG, event.toString())
    }

    companion object {
        private const val TAG = "DottoViewModel"
    }
}
