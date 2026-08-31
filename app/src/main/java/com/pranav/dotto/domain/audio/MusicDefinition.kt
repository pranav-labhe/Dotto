package com.pranav.dotto.domain.audio

/**
 * Represents the entire collection of musical personalities defined in XML.
 */
data class MusicLibrary(
    val tracks: Map<String, TrackConfig>
)

/**
 * Configuration for a specific musical state (e.g., Landing, Game).
 */
data class TrackConfig(
    val id: String,
    val tempo: Int,
    val root: String,
    val scale: String,
    val bass: BassConfig,
    val pad: PadConfig,
    val arpeggio: ArpConfig
)

data class BassConfig(
    val waveform: String,
    val octave: Int,
    val volume: Float
)

data class PadConfig(
    val waveform: String,
    val attack: Float,
    val release: Float,
    val volume: Float
)

data class ArpConfig(
    val pattern: List<Int>,
    val noteLength: String,
    val volume: Float
)
