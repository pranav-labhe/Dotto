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
        // Ensure phase is wrapped BEFORE calculation to prevent out-of-bounds jumps
        while (phase >= 2f * PI.toFloat()) phase -= 2f * PI.toFloat()
        while (phase < 0f) phase += 2f * PI.toFloat()
        
        val x = phase / PI.toFloat() // Range 0..2
        val sample = 1f - 2f * Math.abs(1f - x)
        
        phase += 2f * PI.toFloat() * frequency / sampleRate
        return sample
    }
}

class SquareWaveOscillator : Oscillator {
    private var phase = 0f

    override fun nextSample(frequency: Float, sampleRate: Int): Float {
        while (phase >= 2f * PI.toFloat()) phase -= 2f * PI.toFloat()
        while (phase < 0f) phase += 2f * PI.toFloat()

        val sample = if (phase < PI.toFloat()) 1f else -1f
        
        phase += 2f * PI.toFloat() * frequency / sampleRate
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

    /**
     * @param on Whether the gate is on or off
     * @param retrigger If true, resets the level to 0 immediately when gate is turned on.
     */
    fun gate(on: Boolean, retrigger: Boolean = false) {
        if (on) {
            if (retrigger) currentLevel = 0f
            state = State.ATTACK
        } else {
            state = State.RELEASE
        }
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
