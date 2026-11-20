package com.kalimba.ava.core

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** Result of mapping a frequency to the nearest equal-tempered note. */
data class NoteReading(
    val midi: Int,
    val name: String,
    val octave: Int,
    val cents: Double,
    val frequency: Double,
    val targetFrequency: Double,
)

object NoteMath {
    const val DEFAULT_A4 = 440.0
    private val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun midiToFreq(midi: Int, a4: Double = DEFAULT_A4): Double = a4 * 2.0.pow((midi - 69) / 12.0)

    fun freqToMidiFloat(freq: Double, a4: Double = DEFAULT_A4): Double = 69.0 + 12.0 * ln(freq / a4) / ln(2.0)

    fun noteName(midi: Int): String = NAMES[((midi % 12) + 12) % 12]

    fun octaveOf(midi: Int): Int = Math.floorDiv(midi, 12) - 1

    fun midiLabel(midi: Int): String = noteName(midi) + octaveOf(midi)

    fun freqToNote(freq: Double, a4: Double = DEFAULT_A4): NoteReading {
        val exact = freqToMidiFloat(freq, a4)
        val midi = exact.roundToInt()
        val cents = (exact - midi) * 100.0
        return NoteReading(midi, noteName(midi), octaveOf(midi), cents, freq, midiToFreq(midi, a4))
    }

    /** Parses "C4", "F#5", "Bb3" into a MIDI number. Returns null when invalid. */
    fun parsePitch(text: String): Int? {
        if (text.length < 2) return null
        val base = when (text[0].uppercaseChar()) {
            'C' -> 0; 'D' -> 2; 'E' -> 4; 'F' -> 5; 'G' -> 7; 'A' -> 9; 'B' -> 11
            else -> return null
        }
        var i = 1
        var accidental = 0
        while (i < text.length && (text[i] == '#' || text[i] == 'b')) {
            accidental += if (text[i] == '#') 1 else -1
            i++
        }
        val octave = text.substring(i).toIntOrNull() ?: return null
        return (octave + 1) * 12 + base + accidental
    }

    fun centsBetween(f1: Double, f2: Double): Double = 1200.0 * ln(f1 / f2) / ln(2.0)

    fun isClose(a: Double, b: Double, eps: Double) = abs(a - b) <= eps
}
