package com.pranav.dotto.presentation.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
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
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // Procedural Music Engine
    private val musicParser = DottoMusicParser(context)
    private val musicLibrary = musicParser.parse(R.xml.dotto_music)
    private val musicEngine = DottoMusicEngine(musicLibrary)

    fun startMusic(trackId: String, level: Int, soundEnabled: Boolean) {
        if (soundEnabled) {
            musicEngine.play(trackId, level)
        } else {
            musicEngine.stop()
        }
    }

    fun stopMusic() {
        musicEngine.stop()
    }

    fun playMove(soundEnabled: Boolean, hapticEnabled: Boolean) {
        if (soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
        }
        if (hapticEnabled) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun playScore(soundEnabled: Boolean, hapticEnabled: Boolean) {
        if (soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
        }
        if (hapticEnabled) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun playWin(soundEnabled: Boolean, hapticEnabled: Boolean) {
        if (soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 500)
        }
        if (hapticEnabled) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 200), -1))
        }
    }

    /** Modern digital 'ack' for map interactions */
    fun playGridSelect(soundEnabled: Boolean) {
        if (soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 45)
        }
    }

    /** Modern digital chirp for UI settings */
    fun playUISelect(soundEnabled: Boolean) {
        if (soundEnabled) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 40)
        }
    }

    fun release() {
        toneGenerator.release()
        musicEngine.stop()
    }
}
