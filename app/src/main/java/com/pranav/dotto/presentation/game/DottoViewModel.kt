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
import com.pranav.dotto.domain.model.GameOutcome
import com.pranav.dotto.domain.model.GameState
import com.pranav.dotto.domain.model.GameStatus
import com.pranav.dotto.domain.model.Line
import com.pranav.dotto.domain.model.PlayerType
import com.pranav.dotto.domain.player.HumanPlayerController
import com.pranav.dotto.infrastructure.persistence.PlayerProgressEntity
import com.pranav.dotto.infrastructure.persistence.ProgressDao
import com.pranav.dotto.infrastructure.persistence.SavedMoveEntity
import com.pranav.dotto.presentation.sound.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val soundManager: SoundManager? = null,
    private val progressDao: ProgressDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<DottoUiState>(DottoUiState.Setup())
    val uiState: StateFlow<DottoUiState> = _uiState.asStateFlow()

    private var humanController: HumanPlayerController? = null
    private var aiController: AiPlayerController? = null
    private var aiJob: Job? = null

    private var lastSetupConfig: SetupConfig = SetupConfig()
    private var currentTotalScore: Int = 0
    private var highestLevelReached: Int = 1
    private var nextPlayerIdToResume: String = "human_player"

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            progressDao?.getProgress()?.first()?.let { progress ->
                lastSetupConfig = lastSetupConfig.copy(
                    humanName = progress.playerName,
                    levelNumber = progress.currentLevel,
                    gridDots = (progress.currentLevel + 2).coerceIn(3, 10)
                )
                currentTotalScore = progress.totalScore
                highestLevelReached = progress.highestLevelReached
                nextPlayerIdToResume = progress.nextPlayerId
                
                // Update initial state with loaded config and stats
                if (_uiState.value is DottoUiState.Setup) {
                    _uiState.update { 
                        DottoUiState.Setup(
                            config = lastSetupConfig,
                            totalScore = currentTotalScore,
                            highestLevel = highestLevelReached
                        ) 
                    }
                }
            }
        }
    }

    private fun saveProgress(nextMover: String? = null) {
        viewModelScope.launch {
            val progress = PlayerProgressEntity(
                playerName = lastSetupConfig.humanName,
                currentLevel = lastSetupConfig.levelNumber,
                totalScore = currentTotalScore,
                highestLevelReached = highestLevelReached,
                nextPlayerId = nextMover ?: nextPlayerIdToResume
            )
            progressDao?.saveProgress(progress)
        }
    }

    fun updateSetupConfig(transform: (SetupConfig) -> SetupConfig) {
        val current = _uiState.value
        if (current is DottoUiState.Setup) {
            val nextConfig = transform(current.config)
            lastSetupConfig = nextConfig // CRITICAL: Keep local cache sync'd with UI selection
            _uiState.update { 
                DottoUiState.Setup(
                    config = nextConfig,
                    totalScore = currentTotalScore,
                    highestLevel = highestLevelReached
                ) 
            }
        }
    }

    fun startGame() {
        val config = (_uiState.value as? DottoUiState.Setup)?.config ?: lastSetupConfig
        lastSetupConfig = config
        saveProgress()

        // Cancel any stale AI work from a previous game before wiring up the new one.
        cancelAiWork()

        viewModelScope.launch {
            val result = startNewGame(config)
            humanController = result.humanController
            aiController = result.aiController
            
            var currentState = result.gameState
            
            // Replay saved moves if they belong to the current level
            val savedMoves = progressDao?.getAllMoves() ?: emptyList()
            if (savedMoves.isNotEmpty() && savedMoves.first().levelNumber == config.levelNumber) {
                Log.d(TAG, "Replaying ${savedMoves.size} saved moves for Level ${config.levelNumber}")
                savedMoves.forEach { moveEntity ->
                    val line = if (moveEntity.lineType == "Horizontal") {
                        Line.Horizontal(moveEntity.row, moveEntity.column)
                    } else {
                        Line.Vertical(moveEntity.row, moveEntity.column)
                    }
                    val moveResult = makeMove(currentState, GameMove(com.pranav.dotto.domain.model.PlayerId(moveEntity.playerId), line))
                    if (moveResult.accepted) {
                        currentState = moveResult.newState
                    }
                }
            } else if (savedMoves.isNotEmpty()) {
                Log.d(TAG, "Saved moves exist but belong to a different level. Starting fresh.")
                // We don't clear moves here because only Refresh/Close have authority to flush.
            } else {
                logEvent(GameEvent.GameStarted(currentState.players.map { it.id }))
            }

            _uiState.value = DottoUiState.Playing(gameState = currentState)
            
            // If the game is already finished after replay, show result
            if (currentState.status is GameStatus.Finished) {
                _uiState.value = DottoUiState.Result(currentState)
            } else {
                maybeTriggerAiTurn(currentState)
            }
        }
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
        viewModelScope.launch {
            progressDao?.clearAllMoves()
            _uiState.value = DottoUiState.Setup(
                config = lastSetupConfig,
                totalScore = currentTotalScore,
                highestLevel = highestLevelReached
            )
        }
    }

    fun playAgainSameConfig() {
        cancelAiWork()
        viewModelScope.launch {
            progressDao?.clearAllMoves()
            _uiState.value = DottoUiState.Setup(
                config = lastSetupConfig,
                totalScore = currentTotalScore,
                highestLevel = highestLevelReached
            )
            startGame()
        }
    }

    fun startNextLevel() {
        val currentDots = lastSetupConfig.gridDots
        val nextLevel = (currentDots - 2) + 1
        val nextDots = nextLevel + 2 
        
        lastSetupConfig = lastSetupConfig.copy(
            levelNumber = nextLevel,
            gridDots = nextDots
        )
        
        Log.d(TAG, "Starting Next Level: $nextLevel (${nextDots}x${nextDots})")
        
        cancelAiWork()
        viewModelScope.launch {
            progressDao?.clearAllMoves()
            _uiState.value = DottoUiState.Setup(
                config = lastSetupConfig,
                totalScore = currentTotalScore,
                highestLevel = highestLevelReached
            )
            startGame()
        }
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

        // Save move and next turn to DB
        viewModelScope.launch {
            val nextMoverId = result.newState.currentPlayerId?.value ?: move.playerId.value
            nextPlayerIdToResume = nextMoverId
            
            progressDao?.insertMove(
                SavedMoveEntity(
                    levelNumber = lastSetupConfig.levelNumber,
                    playerId = move.playerId.value,
                    lineType = if (move.line is Line.Horizontal) "Horizontal" else "Vertical",
                    row = when (val l = move.line) {
                        is Line.Horizontal -> l.row
                        is Line.Vertical -> l.row
                    },
                    column = when (val l = move.line) {
                        is Line.Horizontal -> l.column
                        is Line.Vertical -> l.column
                    }
                )
            )
            saveProgress(nextMoverId)
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

        if (result.newState.status is GameStatus.Finished) {
            val humanPlayer = result.newState.players.firstOrNull { it.type == PlayerType.HUMAN }
            if (humanPlayer != null) {
                currentTotalScore += result.newState.scoreOf(humanPlayer.id)
                
                // If human won, update highest level
                val outcome = (result.newState.status as? GameStatus.Finished)?.outcome
                if (outcome is GameOutcome.Win && outcome.winnerId == humanPlayer.id) {
                    highestLevelReached = maxOf(highestLevelReached, lastSetupConfig.levelNumber + 1)
                }
            }
            viewModelScope.launch {
                progressDao?.clearAllMoves()
                saveProgress()
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
