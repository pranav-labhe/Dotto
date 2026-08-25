package com.pranav.dotto.presentation.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Low-latency audio and haptic feedback for immediate "eye-catching" interaction.
 */
class SoundManager(context: Context) {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

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

    fun release() {
        toneGenerator.release()
    }
}
