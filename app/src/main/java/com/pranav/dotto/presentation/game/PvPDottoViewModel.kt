package com.pranav.dotto.presentation.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.dotto.application.state.DottoUiState
import com.pranav.dotto.application.state.SetupConfig
import com.pranav.dotto.application.transport.MoveTransport
import com.pranav.dotto.application.usecase.MakeMoveUseCase
import com.pranav.dotto.domain.board.BoardGeometry
import com.pranav.dotto.domain.engine.GameEngine
import com.pranav.dotto.domain.engine.GameEngineImpl
import com.pranav.dotto.domain.events.GameEvent
import com.pranav.dotto.domain.model.*
import com.pranav.dotto.infrastructure.persistence.PlayerProgressEntity
import com.pranav.dotto.infrastructure.persistence.ProgressDao
import com.pranav.dotto.infrastructure.persistence.SavedMoveEntity
import com.pranav.dotto.presentation.sound.SoundManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PvPDottoViewModel(
    private val transport: MoveTransport,
    private val engine: GameEngine = GameEngineImpl(),
    private val makeMove: MakeMoveUseCase = MakeMoveUseCase(engine),
    private val soundManager: SoundManager? = null,
    private val progressDao: ProgressDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<DottoUiState>(DottoUiState.PvPSetup(SetupConfig(), isHost = true))
    val uiState: StateFlow<DottoUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<MoveTransport.ConnectionState> = transport.connectionState

    private var localConfig: SetupConfig = SetupConfig()
    private var isHostDevice: Boolean = true
    private var localPlayerId = PlayerId("local_player")
    private var remotePlayerId = PlayerId("remote_player")

    init {
        transport.onLevelReceived { remoteLevel ->
            Log.d(TAG, "Received level from challenger: $remoteLevel")
            localConfig = localConfig.copy(
                levelNumber = remoteLevel,
                gridDots = remoteLevel + 2
            )
            // Opponent updates state with the challenger's selected level
            _uiState.update { current ->
                if (current is DottoUiState.PvPSetup) {
                    current.copy(config = localConfig)
                } else current
            }
        }

        transport.onMoveReceived { lineIndex ->
            Log.d(TAG, "Received remote move index: $lineIndex")
            val playing = _uiState.value as? DottoUiState.PvPPlaying ?: return@onMoveReceived
            val line = BoardGeometry.indexToLine(playing.gameState.board.config, lineIndex)
            applyMove(GameMove(remotePlayerId, line), isRemoteMove = true)
        }
    }

    fun initSetup(config: SetupConfig, isHost: Boolean) {
        this.localConfig = config
        // Keep it in Setup, but we won't automatically start transport advertising/discovery here.
        // We let the user choose dynamically on the screen.
        _uiState.value = DottoUiState.PvPSetup(config = localConfig, isHost = isHost)
    }

    fun startAdvertising() {
        transport.disconnect()
        this.isHostDevice = true
        this.localPlayerId = PlayerId("host_player")
        this.remotePlayerId = PlayerId("joiner_player")
        _uiState.update { current ->
            if (current is DottoUiState.PvPSetup) current.copy(isHost = true) else current
        }
        transport.startAdvertising(localConfig.humanName.ifBlank { "Host" })
    }

    fun startDiscovery() {
        transport.disconnect()
        this.isHostDevice = false
        this.localPlayerId = PlayerId("joiner_player")
        this.remotePlayerId = PlayerId("host_player")
        _uiState.update { current ->
            if (current is DottoUiState.PvPSetup) current.copy(isHost = false) else current
        }
        transport.startDiscovery()
    }

    fun onEnterGame() {
        if (isHostDevice) {
            transport.sendLevel(localConfig.levelNumber)
        }
        startPvPGame()
    }

    private fun startPvPGame() {
        val player1 = Player(
            id = if (isHostDevice) localPlayerId else remotePlayerId,
            name = if (isHostDevice) localConfig.humanName.ifBlank { "Player 1" } else "Opponent",
            initial = if (isHostDevice) localConfig.humanName.trim().take(1).ifBlank { "P" }.uppercase() else "O",
            type = if (isHostDevice) PlayerType.HUMAN else PlayerType.REMOTE,
            colorToken = PlayerColorToken.PRIMARY
        )
        val player2 = Player(
            id = if (isHostDevice) remotePlayerId else localPlayerId,
            name = if (isHostDevice) "Opponent" else localConfig.humanName.ifBlank { "Player 2" },
            initial = if (isHostDevice) "O" else localConfig.humanName.trim().take(1).ifBlank { "P" }.uppercase(),
            type = if (isHostDevice) PlayerType.REMOTE else PlayerType.HUMAN,
            colorToken = PlayerColorToken.SECONDARY
        )

        val players = if (isHostDevice) listOf(player1, player2) else listOf(player1, player2)
        val initialGameState = engine.startGame(localConfig.boardConfig, players)

        _uiState.value = DottoUiState.PvPPlaying(
            gameState = initialGameState,
            isRemoteTurn = initialGameState.currentPlayerId != localPlayerId
        )
    }

    fun onLineSelected(line: Line) {
        val playing = _uiState.value as? DottoUiState.PvPPlaying ?: return
        val state = playing.gameState
        if (state.currentPlayerId != localPlayerId) return
        if (!engine.validateMove(state, line)) return

        val lineIndex = BoardGeometry.lineToIndex(state.board.config, line)
        transport.sendMove(lineIndex)
        applyMove(GameMove(localPlayerId, line), isRemoteMove = false)
    }

    private fun applyMove(move: GameMove, isRemoteMove: Boolean) {
        val playing = _uiState.value as? DottoUiState.PvPPlaying ?: return
        val result = makeMove(playing.gameState, move)

        if (!result.accepted) {
            Log.w(TAG, "Rejected move from ${move.playerId}: ${move.line}")
            return
        }

        // Save move locally in database
        viewModelScope.launch {
            progressDao?.insertMove(
                SavedMoveEntity(
                    levelNumber = localConfig.levelNumber,
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
        }

        // Play sounds
        val isLocalHuman = move.playerId == localPlayerId
        if (result.completedBoxes.isNotEmpty()) {
            soundManager?.playScore(isLocalHuman, localConfig.soundEnabled, localConfig.hapticEnabled)
        } else {
            soundManager?.playMove(isLocalHuman, localConfig.soundEnabled, localConfig.hapticEnabled)
        }

        if (result.newState.status is GameStatus.Finished) {
            val outcome = (result.newState.status as? GameStatus.Finished)?.outcome
            val isLocalWinner = (outcome as? GameOutcome.Win)?.winnerId == localPlayerId
            soundManager?.playWin(isLocalWinner, localConfig.soundEnabled, localConfig.hapticEnabled)
            _uiState.value = DottoUiState.Result(result.newState)
        } else {
            _uiState.value = DottoUiState.PvPPlaying(
                gameState = result.newState,
                isRemoteTurn = result.newState.currentPlayerId != localPlayerId,
                lastMoveLine = move.line,
                recentlyCompletedBoxes = result.completedBoxes.toSet()
            )
        }
    }

    fun restart() {
        transport.disconnect()
        _uiState.value = DottoUiState.Setup(config = localConfig)
    }

    fun playAgainSameConfig() {
        startPvPGame()
    }

    override fun onCleared() {
        super.onCleared()
        transport.disconnect()
    }

    companion object {
        private const val TAG = "PvPDottoViewModel"
    }
}
