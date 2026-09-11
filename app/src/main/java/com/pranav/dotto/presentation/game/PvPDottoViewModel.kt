package com.pranav.dotto.presentation.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.dotto.application.state.DottoUiState
import com.pranav.dotto.application.state.SetupConfig
import com.pranav.dotto.application.transport.MoveTransport
import com.pranav.dotto.application.usecase.MakeMoveUseCase
import com.pranav.dotto.domain.engine.GameEngine
import com.pranav.dotto.domain.engine.GameEngineImpl
import com.pranav.dotto.domain.model.*
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

    private val _isLevelReceived = MutableStateFlow(false)
    val isLevelReceived: StateFlow<Boolean> = _isLevelReceived.asStateFlow()

    private var localConfig: SetupConfig = SetupConfig()
    private var isHostDevice: Boolean = true
    var localPlayerId = PlayerId("local_player")
        private set
    private var remotePlayerId = PlayerId("remote_player")
    private var remotePlayerName: String = "Opponent"

    init {
        transport.onLevelReceived { remoteLevel ->
            Log.d(TAG, "Received level from challenger: $remoteLevel")
            localConfig = localConfig.copy(
                levelNumber = remoteLevel,
                gridDots = remoteLevel + 2
            )
            _isLevelReceived.value = true
            _uiState.update { current ->
                if (current is DottoUiState.PvPSetup) current.copy(config = localConfig) else current
            }
        }

        transport.onStartGameReceived {
            Log.d(TAG, "Start game signal received")
            startPvPGame()
        }

        transport.onRestartGameReceived {
            Log.d(TAG, "Restart game signal received")
            startPvPGame()
        }

        transport.onQuitGameReceived {
            Log.d(TAG, "Quit game signal received")
            restart()
        }

        transport.onBackToRoomReceived {
            Log.d(TAG, "Back to room signal received")
            backToRoomLocal()
        }

        transport.onMoveReceived { type, row, col ->
            Log.d(TAG, "Received remote move: type=$type, r=$row, c=$col")
            val line = if (type == 1) Line.Horizontal(row, col) else Line.Vertical(row, col)
            applyMove(GameMove(remotePlayerId, line))
        }

        viewModelScope.launch {
            transport.connectionState.collect { state ->
                if (state is MoveTransport.ConnectionState.Connected) {
                    remotePlayerName = state.remoteName
                    if (isHostDevice) {
                        transport.sendLevel(localConfig.levelNumber)
                    }
                }
            }
        }
    }

    fun initSetup(config: SetupConfig, isHost: Boolean) {
        val currentState = _uiState.value
        if (currentState is DottoUiState.PvPPlaying || currentState is DottoUiState.Result) return
        
        val connState = transport.connectionState.value
        if (connState is MoveTransport.ConnectionState.Connected || connState is MoveTransport.ConnectionState.Connecting) return

        this.localConfig = config
        _isLevelReceived.value = isHost
        _uiState.update { DottoUiState.PvPSetup(config = localConfig, isHost = isHost) }
    }

    fun startAdvertising() {
        transport.disconnect()
        this.isHostDevice = true
        this.localPlayerId = PlayerId("host_player")
        this.remotePlayerId = PlayerId("joiner_player")
        _uiState.update { current -> if (current is DottoUiState.PvPSetup) current.copy(isHost = true) else current }
        transport.startAdvertising(localConfig.humanName.ifBlank { "Host" })
    }

    fun startDiscovery() {
        transport.disconnect()
        this.isHostDevice = false
        this.localPlayerId = PlayerId("joiner_player")
        this.remotePlayerId = PlayerId("host_player")
        _uiState.update { current -> if (current is DottoUiState.PvPSetup) current.copy(isHost = false) else current }
        transport.startDiscovery()
    }

    fun onEndpointSelected(endpointId: String) {
        transport.connectTo(endpointId, localConfig.humanName.ifBlank { "Opponent" })
    }

    fun onEnterGame() {
        transport.sendStartGame()
        startPvPGame()
    }

    private fun startPvPGame() {
        val hostName = if (isHostDevice) localConfig.humanName else remotePlayerName
        val joinerName = if (!isHostDevice) localConfig.humanName else remotePlayerName

        val player1 = Player(
            id = PlayerId("host_player"),
            name = hostName.ifBlank { "Host" },
            initial = hostName.trim().take(1).ifBlank { "H" }.uppercase(),
            type = if (isHostDevice) PlayerType.HUMAN else PlayerType.REMOTE,
            colorToken = PlayerColorToken.PRIMARY
        )
        val player2 = Player(
            id = PlayerId("joiner_player"),
            name = joinerName.ifBlank { "Joiner" },
            initial = joinerName.trim().take(1).ifBlank { "J" }.uppercase(),
            type = if (!isHostDevice) PlayerType.HUMAN else PlayerType.REMOTE,
            colorToken = PlayerColorToken.SECONDARY
        )

        val players = listOf(player1, player2)
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

        val type = if (line is Line.Horizontal) 1 else 2
        val row = when (line) { is Line.Horizontal -> line.row; is Line.Vertical -> line.row }
        val col = when (line) { is Line.Horizontal -> line.column; is Line.Vertical -> line.column }
        
        transport.sendMove(type, row, col)
        applyMove(GameMove(localPlayerId, line))
    }

    private fun applyMove(move: GameMove) {
        val playing = _uiState.value as? DottoUiState.PvPPlaying ?: return
        val result = makeMove(playing.gameState, move)

        if (!result.accepted) return

        viewModelScope.launch {
            progressDao?.insertMove(
                SavedMoveEntity(
                    levelNumber = localConfig.levelNumber,
                    playerId = move.playerId.value,
                    lineType = if (move.line is Line.Horizontal) "Horizontal" else "Vertical",
                    row = when (val l = move.line) { is Line.Horizontal -> l.row; is Line.Vertical -> l.row },
                    column = when (val l = move.line) { is Line.Horizontal -> l.column; is Line.Vertical -> l.column }
                )
            )
        }

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
        transport.sendQuitGame()
        transport.disconnect()
        _uiState.value = DottoUiState.Setup(config = localConfig)
    }

    fun onBackToRoom() {
        transport.sendBackToRoom()
        backToRoomLocal()
    }

    private fun backToRoomLocal() {
        _uiState.update { DottoUiState.PvPSetup(config = localConfig, isHost = isHostDevice) }
    }

    fun playAgainSameConfig() {
        viewModelScope.launch {
            progressDao?.clearAllMoves()
        }
        transport.sendRestartGame()
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
