package com.pranav.dotto

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.dotto.application.state.DottoUiState
import com.pranav.dotto.presentation.game.DottoViewModel
import com.pranav.dotto.presentation.game.GameScreen
import com.pranav.dotto.presentation.result.ResultScreen
import com.pranav.dotto.presentation.setup.SetupScreen
import com.pranav.dotto.presentation.sound.SoundManager
import com.pranav.dotto.presentation.theme.DottoTheme

/**
 * Root composable. Deliberately not using androidx.navigation — the whole
 * app is three linear screens driven by one sealed [DottoUiState], so a
 * simple `when` gives the same behavior with far less ceremony. A real
 * NavHost can be introduced later if screen count grows (e.g. history,
 * replay, settings) without touching the ViewModel contract.
 */
@Composable
fun DottoApp(
    soundManager: SoundManager? = null,
    viewModel: DottoViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DottoViewModel(soundManager = soundManager) as T
            }
        }
    )
) {
    val state by viewModel.uiState.collectAsState()

    DottoTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state,
                label = "dotto-navigation",
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }
            ) { currentState ->
                when (currentState) {
                    is DottoUiState.Setup -> SetupScreen(
                        config = currentState.config,
                        onConfigChange = { newConfig -> viewModel.updateSetupConfig { newConfig } },
                        onStartGame = { viewModel.startGame() }
                    )
                    is DottoUiState.Playing -> GameScreen(
                        state = currentState,
                        onLineTapped = viewModel::onLineSelected,
                        onNewGame = viewModel::restart
                    )
                    is DottoUiState.Result -> ResultScreen(
                        gameState = currentState.gameState,
                        onPlayAgain = viewModel::playAgainSameConfig,
                        onNewSetup = viewModel::restart
                    )
                }
            }
        }
    }
}
