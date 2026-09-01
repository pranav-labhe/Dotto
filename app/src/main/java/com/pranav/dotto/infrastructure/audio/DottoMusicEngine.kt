package com.pranav.dotto.infrastructure.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.pranav.dotto.domain.audio.*
import kotlinx.coroutines.*
import kotlin.math.pow

/**
 * The core procedural synthesizer engine.
 * Generates PCM data in real-time based on the provided MusicLibrary.
 */
class DottoMusicEngine(private val library: MusicLibrary) {

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_FLOAT
    )

    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var currentTrack: TrackConfig? = null
    private var currentLevel: Int = 1
    
    // Voice states
    private var bassOsc: Oscillator = SineWaveOscillator() 
    private var currentBassWaveform = "sine"
    private var padOsc: Oscillator = SineWaveOscillator()
    private var currentPadWaveform = "sine"
    private val padEnv = AdsrEnvelope()
    private var arpOsc: Oscillator = SineWaveOscillator()
    private val arpEnv = AdsrEnvelope()

    // High-pitched interaction voice (Cyberpunk style)
    private var interactionOsc: Oscillator = SineWaveOscillator()
    private val interactionEnv = AdsrEnvelope()
    private var interactionBaseFreq = 880f
    private var interactionCurrentFreq = 880f
    private var interactionSampleIdx = 0
    private var interactionIsHuman = true
    
    @Volatile private var pendingTap = false
    @Volatile private var pendingTapFreq = 880f
    @Volatile private var pendingIsHuman = true

    // Effects: Spacious atmosphere (Delay)
    private val delayBuffer = FloatArray(sampleRate) // 1 second delay
    private var delayWriteIdx = 0
    private var feedback = 0.5f

    // DC Blocker / High-pass to prevent "garbage vibration" on old speakers
    private var lastOut = 0f
    private var lastIn = 0f
    private val hpAlpha = 0.97f // More aggressive cut for cleaner bass

    // Sequencer
    private var sampleCount = 0L
    private var cachedBassFreq = 0f
    private var targetBassFreq = 0f
    private var cachedPadFreq = 0f
    private var targetPadFreq = 0f
    private var lastArpIdx = -1
    private var cachedArpFreq = 0f
    private var targetArpFreq = 0f
    
    // LFO for "Lofi Wobble"
    private var lfoPhase = 0f
    private val lfoFreq = 0.4f // Slow 0.4Hz wobble

    init {
        ensureAudioTrack()
    }

    private fun ensureAudioTrack() {
        if (audioTrack == null || audioTrack?.state == AudioTrack.STATE_UNINITIALIZED) {
            val trackBufferSize = bufferSize * 2
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(trackBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }
    }

    fun play(trackId: String, level: Int = 1) {
        val config = library.tracks[trackId] ?: return
        currentTrack = config
        currentLevel = level
        
        ensureAudioTrack()

        // Update oscillators based on personality
        if (config.bass.waveform != currentBassWaveform) {
            bassOsc = createOscillator(config.bass.waveform)
            currentBassWaveform = config.bass.waveform
        }
        if (config.pad.waveform != currentPadWaveform) {
            padOsc = createOscillator(config.pad.waveform)
            currentPadWaveform = config.pad.waveform
        }
        arpOsc = createOscillator("sine") // Arps stay clean sine for now

        // Update envelopes based on personality
        padEnv.attackTime = config.pad.attack
        padEnv.releaseTime = config.pad.release
        padEnv.gate(true) // Pads are usually ambient

        // Arpeggio envelope needs to be fast to prevent "tp" clicks while staying smooth
        arpEnv.attackTime = 0.01f 
        arpEnv.releaseTime = 0.1f
        
        // Interaction Envelope: Softer attack and longer release to be less harsh
        interactionEnv.attackTime = 0.015f
        interactionEnv.releaseTime = 0.2f

        // Clear targets and buffers to prevent "garbage noise" on transition
        targetBassFreq = 0f
        targetPadFreq = 0f
        targetArpFreq = 0f
        
        sampleCount = 0L // Reset sequencer timing
        lastOut = 0f
        lastIn = 0f
        delayBuffer.fill(0f)
        delayWriteIdx = 0
        
        if (job == null || job?.isActive == false) {
            startLoop()
        }
    }

    private fun createOscillator(waveform: String): Oscillator {
        return when (waveform.lowercase()) {
            "triangle" -> TriangleWaveOscillator()
            "square" -> SquareWaveOscillator()
            else -> SineWaveOscillator() // Default to Sine (handles "cosine" etc)
        }
    }

    private fun startLoop() {
        job = scope.launch {
            audioTrack?.play()
            val floatBuffer = FloatArray(bufferSize / 4)
            
            while (isActive) {
                for (i in floatBuffer.indices) {
                    floatBuffer[i] = generateSample()
                }
                audioTrack?.write(floatBuffer, 0, floatBuffer.size, AudioTrack.WRITE_BLOCKING)
            }
        }
    }

    private fun generateSample(): Float {
        val config = currentTrack ?: return 0f
        
        // Dynamic "Infinite Level" Modifiers
        val levelFactor = (currentLevel - 1)
        val levelTempo = config.tempo + (levelFactor * 0.5f).toInt()
        val levelWobble = config.pad.wobble + (levelFactor * 0.02f)
        
        // Shift root key every 5 levels (Infinite Evolution)
        val rootShift = (levelFactor / 5)
        val levelRoot = shiftRoot(config.root, rootShift)

        // Update target frequencies. Recalculate if rootShift changes or initial.
        val baseBassFreq = noteToFreq(levelRoot, config.bass.octave + 1)
        val basePadFreq = noteToFreq(levelRoot, config.bass.octave + 2)
        
        if (targetBassFreq != baseBassFreq) {
            targetBassFreq = baseBassFreq
            targetPadFreq = basePadFreq
            if (cachedBassFreq == 0f) cachedBassFreq = targetBassFreq
            if (cachedPadFreq == 0f) cachedPadFreq = targetPadFreq
        }

        // SMOOTHING: Slew ALL frequencies towards targets (prevents clicking)
        // Increased speed (0.001 -> 0.05) to make level transitions audible
        cachedBassFreq += (targetBassFreq - cachedBassFreq) * 0.05f 
        cachedPadFreq += (targetPadFreq - cachedPadFreq) * 0.05f
        
        // LFO Update
        lfoPhase += 2f * Math.PI.toFloat() * lfoFreq / sampleRate
        if (lfoPhase > 2f * Math.PI.toFloat()) lfoPhase -= 2f * Math.PI.toFloat()
        val wobbleOffset = kotlin.math.sin(lfoPhase) * levelWobble * 2.5f

        val samplesPerBeat = (sampleRate * 60) / levelTempo
        val samplesPerArp = when (config.arpeggio.noteLength) {
            "sixteenth" -> samplesPerBeat / 4
            "eighth" -> samplesPerBeat / 2
            "fifth" -> samplesPerBeat * 4 / 5
            "sixth" -> samplesPerBeat * 4 / 6
            "seventh" -> (samplesPerBeat  * 4) / 7
            "half" -> samplesPerBeat * 2
            else -> samplesPerBeat // quarter
        }

        // Bass Frequency (Sub-bass)
        val bass = bassOsc.nextSample(cachedBassFreq, sampleRate) * config.bass.volume

        // Pad Frequency (Atmospheric) with Lofi Wobble
        val pad = padOsc.nextSample(cachedPadFreq + wobbleOffset, sampleRate) * padEnv.nextLevel(sampleRate) * config.pad.volume

        // Arpeggio logic
        val arpStep = (sampleCount / samplesPerArp).toInt()
        val shouldPlay = (arpStep % 4 != 0) || (levelFactor > 10) 
        val arpIdx = arpStep % config.arpeggio.pattern.size
        
        if (arpIdx != lastArpIdx) {
            val arpInterval = config.arpeggio.pattern[arpIdx]
            targetArpFreq = noteToFreq(levelRoot, config.bass.octave + 2, arpInterval)
            if (cachedArpFreq == 0f) cachedArpFreq = targetArpFreq // Instant first note
            lastArpIdx = arpIdx
        }

        // SMOOTHING: Slew the frequency towards the target (prevents clicking)
        cachedArpFreq += (targetArpFreq - cachedArpFreq) * 0.05f 

        // Trigger arp envelope at start of each note
        if (sampleCount % samplesPerArp == 0L) {
            if (shouldPlay) {
                arpEnv.gate(true)
            } else {
                arpEnv.gate(false) // Ensure it's off if we are skipping
            }
        } else if (sampleCount % samplesPerArp == (samplesPerArp * 0.8f).toLong()) {
            arpEnv.gate(false)
        }

        // NO hard if(shouldPlay) cut here. The envelope handles the silence smoothly.
        val arp = arpOsc.nextSample(cachedArpFreq + wobbleOffset, sampleRate) * 
                  arpEnv.nextLevel(sampleRate) * 
                  config.arpeggio.volume
                  
        // Trigger interaction sounds on quantization grid
        if (sampleCount % samplesPerArp == 0L) {
            if (pendingTap) {
                interactionBaseFreq = pendingTapFreq
                interactionIsHuman = pendingIsHuman
                
                // Different character for Player vs Dotto
                interactionOsc = SineWaveOscillator() // Use Sine for both to be less harsh
                
                if (interactionIsHuman) {
                    interactionCurrentFreq = interactionBaseFreq * 0.9f // Subtler sweep
                } else {
                    interactionCurrentFreq = interactionBaseFreq * 1.1f // Subtler sweep
                }
                
                interactionSampleIdx = 0
                interactionEnv.gate(on = true, retrigger = false)
                pendingTap = false
            }
        }
        
        // Auto-gate off interaction after a short duration
        if (sampleCount % samplesPerArp == (samplesPerArp / 2).toLong()) {
            interactionEnv.gate(on = false)
        }

        // Pitch Sweep Logic: Cyberpunk Character (Softer)
        if (interactionEnv.isActive()) {
            val sweepDurationSamples = (sampleRate * 0.1f).toInt() // 100ms sweep
            if (interactionSampleIdx < sweepDurationSamples) {
                val progress = interactionSampleIdx.toFloat() / sweepDurationSamples
                if (interactionIsHuman) {
                    // Ascending Power-up for Player
                    interactionCurrentFreq = interactionBaseFreq * (0.9f + 0.1f * progress)
                } else {
                    // Descending Power-down for Dotto
                    interactionCurrentFreq = interactionBaseFreq * (1.1f - 0.1f * progress)
                }
            } else {
                interactionCurrentFreq = interactionBaseFreq
            }
            interactionSampleIdx++
        }

        val tap = interactionOsc.nextSample(interactionCurrentFreq, sampleRate) * 
                  interactionEnv.nextLevel(sampleRate)

        sampleCount++

        // Mixer: Keep Bass separate from Effects to prevent "noise mud"
        // Lowered tap volume significantly to 0.15f
        val melodicSum = (pad + arp + tap * 0.15f) * 0.2f
        
        // Effects: Spacious atmosphere (Delay) - ONLY for pads and arps
        val delayReadIdx = (delayWriteIdx + 1) % delayBuffer.size
        val delayedSignal = delayBuffer[delayReadIdx]
        
        val spatialOut = melodicSum + delayedSignal * feedback
        
        // Write melodic content to delay buffer
        delayBuffer[delayWriteIdx] = spatialOut
        delayWriteIdx = (delayWriteIdx + 1) % delayBuffer.size

        // Final Mix: Dry Bass + Spatial Melody
        // Adjusted Bass gain (0.6 -> 0.4) for better balance and headroom
        val mixed = (bass * 0.4f) + spatialOut
        
        // DC Blocker / High-pass Filter
        val out = hpAlpha * (lastOut + mixed - lastIn)
        lastIn = mixed
        lastOut = out
        
        // Final Soft Limiter to prevent "krrrrrrrrrr" (hard digital clipping)
        return softLimit(out)
    }

    private fun softLimit(x: Float): Float {
        // Tanh-like soft clipping to prevent hard edges at 1.0/-1.0
        return if (x > 0.7f) {
            0.7f + (x - 0.7f) * 0.3f / (1f + (x - 0.7f))
        } else if (x < -0.7f) {
            -0.7f + (x + 0.7f) * 0.3f / (1f + (-x - 0.7f))
        } else {
            x
        }.coerceIn(-0.95f, 0.95f)
    }

    private fun noteToFreq(root: String, octave: Int, interval: Int = 0): Float {
        val rootFreq = when (root.uppercase()) {
            "C" -> 16.35f
            "C#" -> 17.32f
            "D" -> 18.35f
            "D#" -> 19.45f
            "E" -> 20.60f
            "F" -> 21.83f
            "F#" -> 23.12f
            "G" -> 24.50f
            "G#" -> 25.96f
            "A" -> 27.50f
            "A#" -> 29.14f
            "B" -> 30.87f
            else -> 18.35f
        }
        return rootFreq * 2f.pow(octave) * 2f.pow(interval / 12f)
    }

    private fun shiftRoot(root: String, semitones: Int): String {
        val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val idx = notes.indexOf(root.uppercase())
        if (idx == -1) return root
        val nextIdx = (idx + semitones) % notes.size
        return notes[nextIdx]
    }

    fun stop() {
        job?.cancel()
        job = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    /** Releases all resources and cancels the synthesis scope */
    fun release() {
        stop()
        scope.cancel()
    }

    /** Triggers a high-pitched ping aligned with the music rhythm */
    fun triggerTap(frequency: Float, isHuman: Boolean = true) {
        pendingTapFreq = frequency
        pendingIsHuman = isHuman
        pendingTap = true
    }
}
