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
    
    // Voice states
    private val bassOsc = TriangleWaveOscillator()
    private val padOsc = SineWaveOscillator()
    private val padEnv = AdsrEnvelope()
    private val arpOsc = SineWaveOscillator()
    private val arpEnv = AdsrEnvelope()

    // Effects: Spacious atmosphere (Delay)
    private val delayBuffer = FloatArray(sampleRate) // 1 second delay
    private var delayWriteIdx = 0
    private var feedback = 0.5f

    // Sequencer
    private var sampleCount = 0L

    init {
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
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    fun play(trackId: String) {
        val config = library.tracks[trackId] ?: return
        currentTrack = config
        
        // Update envelopes based on personality
        padEnv.attackTime = config.pad.attack
        padEnv.releaseTime = config.pad.release
        padEnv.gate(true) // Pads are usually ambient
        
        if (job == null || job?.isActive == false) {
            startLoop()
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
        
        val samplesPerBeat = (sampleRate * 60) / config.tempo
        val samplesPerArp = when (config.arpeggio.noteLength) {
            "sixteenth" -> samplesPerBeat / 4
            "eighth" -> samplesPerBeat / 2
            "half" -> samplesPerBeat * 2
            else -> samplesPerBeat // quarter
        }

        // Bass Frequency (Sub-bass)
        val bassFreq = noteToFreq(config.root, config.bass.octave)
        val bass = bassOsc.nextSample(bassFreq, sampleRate) * config.bass.volume

        // Pad Frequency (Atmospheric)
        val padFreq = noteToFreq(config.root, config.bass.octave + 1)
        val pad = padOsc.nextSample(padFreq, sampleRate) * padEnv.nextLevel(sampleRate) * config.pad.volume

        // Arpeggio logic
        val arpStep = (sampleCount / samplesPerArp).toInt()
        val arpIdx = arpStep % config.arpeggio.pattern.size
        
        // Trigger arp envelope at start of each note
        if (sampleCount % samplesPerArp == 0L) {
            arpEnv.gate(true)
        } else if (sampleCount % samplesPerArp == (samplesPerArp * 0.8f).toLong()) {
            arpEnv.gate(false)
        }

        val arpInterval = config.arpeggio.pattern[arpIdx]
        val arpFreq = noteToFreq(config.root, config.bass.octave + 2, arpInterval)
        val arp = arpOsc.nextSample(arpFreq, sampleRate) * arpEnv.nextLevel(sampleRate) * config.arpeggio.volume

        sampleCount++

        // Mixer
        val dry = bass + pad + arp
        
        // Effects: Spacious atmosphere (Delay)
        val delayReadIdx = (delayWriteIdx + 1) % delayBuffer.size
        val delayedSignal = delayBuffer[delayReadIdx]
        
        val out = dry + delayedSignal * feedback
        
        // Write to delay buffer
        delayBuffer[delayWriteIdx] = dry + delayedSignal * feedback
        delayWriteIdx = (delayWriteIdx + 1) % delayBuffer.size

        // Soft limit
        return out.coerceIn(-0.9f, 0.9f)
    }

    private fun noteToFreq(root: String, octave: Int, interval: Int = 0): Float {
        val rootFreq = when (root.uppercase()) {
            "C" -> 16.35f
            "D" -> 18.35f
            "E" -> 20.60f
            "F" -> 21.83f
            "G" -> 24.50f
            "A" -> 27.50f
            "B" -> 30.87f
            else -> 18.35f
        }
        return rootFreq * 2f.pow(octave) * 2f.pow(interval / 12f)
    }

    fun stop() {
        job?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
