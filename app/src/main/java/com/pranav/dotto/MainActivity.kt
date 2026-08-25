package com.pranav.dotto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pranav.dotto.presentation.sound.SoundManager

class MainActivity : ComponentActivity() {
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = SoundManager(this)
        enableEdgeToEdge()
        setContent {
            DottoApp(soundManager = soundManager)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
