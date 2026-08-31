package com.pranav.dotto.infrastructure.audio

import kotlin.math.PI
import kotlin.math.sin

/**
 * Basic interface for a waveform generator.
 */
interface Oscillator {
    fun nextSample(frequency: Float, sampleRate: Int): Float
}

class SineWaveOscillator : Oscillator {
    private var phase = 0f

    override fun nextSample(frequency: Float, sampleRate: Int): Float {
        val sample = sin(phase).toFloat()
        phase += 2f * PI.toFloat() * frequency / sampleRate
        while (phase > 2f * PI.toFloat()) phase -= 2f * PI.toFloat()
        return sample
    }
}

class TriangleWaveOscillator : Oscillator {
    private var phase = 0f

    override fun nextSample(frequency: Float, sampleRate: Int): Float {
        val t = phase / (2f * PI.toFloat())
        val sample = (2f * Math.abs(2f * (t - Math.floor(t.toDouble() + 0.5).toFloat())) - 1f).toFloat()
        phase += 2f * PI.toFloat() * frequency / sampleRate
        while (phase > 2f * PI.toFloat()) phase -= 2f * PI.toFloat()
        return sample
    }
}

/**
 * A simplified ADSR envelope to shape the volume of a voice.
 */
class AdsrEnvelope {
    var attackTime: Float = 0.1f // seconds
    var releaseTime: Float = 1.0f // seconds
    private var currentLevel = 0f
    private var state = State.IDLE

    enum class State { IDLE, ATTACK, SUSTAIN, RELEASE }

    fun gate(on: Boolean) {
        state = if (on) State.ATTACK else State.RELEASE
    }

    fun nextLevel(sampleRate: Int): Float {
        val attackStep = 1f / (attackTime * sampleRate)
        val releaseStep = 1f / (releaseTime * sampleRate)

        when (state) {
            State.ATTACK -> {
                currentLevel += attackStep
                if (currentLevel >= 1f) {
                    currentLevel = 1f
                    state = State.SUSTAIN
                }
            }
            State.RELEASE -> {
                currentLevel -= releaseStep
                if (currentLevel <= 0f) {
                    currentLevel = 0f
                    state = State.IDLE
                }
            }
            State.IDLE -> currentLevel = 0f
            State.SUSTAIN -> currentLevel = 1f
        }
        return currentLevel
    }

    fun isActive() = state != State.IDLE
}
