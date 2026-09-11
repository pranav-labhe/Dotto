package com.pranav.dotto.infrastructure.audio

import android.content.Context
import com.pranav.dotto.domain.audio.*
import org.xmlpull.v1.XmlPullParser

/**
 * Parses the musical personality XML into Domain models.
 */
class DottoMusicParser(private val context: Context) {

    fun parse(resourceId: Int): MusicLibrary {
        val tracks = mutableMapOf<String, TrackConfig>()
        val parser = context.resources.getXml(resourceId)
        
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "track") {
                val track = parseTrack(parser)
                tracks[track.id] = track
            }
            eventType = parser.next()
        }
        return MusicLibrary(tracks)
    }

    private fun parseTrack(parser: XmlPullParser): TrackConfig {
        val id = parser.getAttributeValue(null, "id") ?: "unknown"
        val tempo = parser.getAttributeValue(null, "tempo")?.toInt() ?: 72
        val root = parser.getAttributeValue(null, "root") ?: "D"
        val scale = parser.getAttributeValue(null, "scale") ?: "minor"
        
        var bass: BassConfig? = null
        var pad: PadConfig? = null
        var arpeggio: ArpConfig? = null

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "track")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "bass" -> bass = BassConfig(
                        waveform = parser.getAttributeValue(null, "waveform") ?: "triangle",
                        octave = parser.getAttributeValue(null, "octave")?.toInt() ?: 1,
                        volume = parser.getAttributeValue(null, "volume")?.toFloat() ?: 0.5f
                    )
                    "pad" -> pad = PadConfig(
                        waveform = parser.getAttributeValue(null, "waveform") ?: "sine",
                        attack = parser.getAttributeValue(null, "attack")?.toFloat() ?: 2.0f,
                        release = parser.getAttributeValue(null, "release")?.toFloat() ?: 4.0f,
                        volume = parser.getAttributeValue(null, "volume")?.toFloat() ?: 0.2f,
                        wobble = parser.getAttributeValue(null, "wobble")?.toFloat() ?: 0f
                    )
                    "arpeggio" -> {
                        val patternStr = parser.getAttributeValue(null, "pattern") ?: "1,5,8"
                        val pattern = patternStr.split(",").mapNotNull { it.trim().toIntOrNull() }
                        arpeggio = ArpConfig(
                            pattern = pattern,
                            noteLength = parser.getAttributeValue(null, "noteLength") ?: "eighth",
                            volume = parser.getAttributeValue(null, "volume")?.toFloat() ?: 0.1f
                        )
                    }
                }
            }
            eventType = parser.next()
        }

        return TrackConfig(
            id = id,
            tempo = tempo,
            root = root,
            scale = scale,
            bass = bass ?: BassConfig("triangle", 1, 0.5f),
            pad = pad ?: PadConfig("sine", 2f, 4f, 0.2f),
            arpeggio = arpeggio ?: ArpConfig(listOf(1, 5, 8), "eighth", 0.1f)
        )
    }
}
