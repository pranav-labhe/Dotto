package com.pranav.dotto.presentation.sound

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import com.pranav.dotto.infrastructure.audio.DottoMusicEngine
import com.pranav.dotto.infrastructure.audio.DottoMusicParser
import com.pranav.dotto.R

/**
 * Low-latency audio and haptic feedback for immediate "eye-catching" interaction.
 * Now includes the Dotto procedural music engine.
 */
class SoundManager(context: Context) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // Procedural Music Engine
    private val musicParser = DottoMusicParser(context)
    private val musicLibrary = musicParser.parse(R.xml.dotto_music)
    private val musicEngine = DottoMusicEngine(musicLibrary)
    
    private var lastTrackId: String? = null
    private var lastLevel: Int = 1

    fun startMusic(trackId: String, level: Int, soundEnabled: Boolean) {
        lastTrackId = trackId
        lastLevel = level
        if (soundEnabled) {
            musicEngine.play(trackId, level)
        } else {
            musicEngine.stop()
        }
    }

    fun stopMusic() {
        musicEngine.stop()
    }
    
    fun resumeMusic(soundEnabled: Boolean) {
        val tid = lastTrackId ?: return
        if (soundEnabled) {
            musicEngine.play(tid, lastLevel)
        }
    }

    fun playMove(isHuman: Boolean, soundEnabled: Boolean, hapticEnabled: Boolean) {
        if (soundEnabled) {
            musicEngine.triggerTap(880f, isHuman) // High A
        }
        if (hapticEnabled) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun playScore(isHuman: Boolean, soundEnabled: Boolean, hapticEnabled: Boolean) {
        if (soundEnabled) {
            musicEngine.triggerTap(440.0f, isHuman, isScore = true) // Lower A4 Chime
        }
        if (hapticEnabled) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun playWin(isHuman: Boolean, soundEnabled: Boolean, hapticEnabled: Boolean) {
        if (soundEnabled) {
            musicEngine.triggerTap(659.25f, isHuman, isScore = true) // E5 Chime
        }
        if (hapticEnabled) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 200), -1))
        }
    }

    /** Modern digital 'ack' for map interactions */
    fun playGridSelect(soundEnabled: Boolean) {
        if (soundEnabled) {
            musicEngine.triggerTap(1046.5f) // C6
        }
    }

    /** Modern digital chirp for UI settings */
    fun playUISelect(soundEnabled: Boolean) {
        if (soundEnabled) {
            musicEngine.triggerTap(1174.6f) // D6
        }
    }

    fun release() {
        musicEngine.release()
    }
}
