package com.kalimba.ava.core

/**
 * Standard 17-key kalimba tuned in C major.
 * Keys are numbered 1..17 by ascending pitch (1 = C4 ... 17 = E6).
 * Physically the keys alternate left/right around the centre key (C4).
 */
object KalimbaLayout {
    const val KEY_COUNT = 17

    private val MIDI_BY_KEY = intArrayOf(60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84, 86, 88)

    /** Key numbers from the left edge to the right edge of the instrument. */
    val PHYSICAL_ORDER = intArrayOf(16, 14, 12, 10, 8, 6, 4, 2, 1, 3, 5, 7, 9, 11, 13, 15, 17)

    private val DEGREE_DIGIT = mapOf(0 to 1, 2 to 2, 4 to 3, 5 to 4, 7 to 5, 9 to 6, 11 to 7)

    fun midiForKey(key: Int): Int {
        require(key in 1..KEY_COUNT) { "key out of range: $key" }
        return MIDI_BY_KEY[key - 1]
    }

    fun keyForMidi(midi: Int): Int? {
        val i = MIDI_BY_KEY.indexOf(midi)
        return if (i >= 0) i + 1 else null
    }

    fun frequencyForKey(key: Int, a4: Double = NoteMath.DEFAULT_A4): Double =
        NoteMath.midiToFreq(midiForKey(key), a4)

    /** Left-to-right slot (0..16) of a key. */
    fun slotOfKey(key: Int): Int = PHYSICAL_ORDER.indexOf(key)

    fun keyAtSlot(slot: Int): Int = PHYSICAL_ORDER[slot]

    fun noteLabel(key: Int): String = NoteMath.midiLabel(midiForKey(key))

    /** Numbered notation like the engraving on the instrument: 1..7 plus dots for higher octaves. */
    fun numberLabel(key: Int): String {
        val midi = midiForKey(key)
        val digit = DEGREE_DIGIT.getValue(midi % 12)
        val dots = when {
            midi < 72 -> ""
            midi < 84 -> "\u2022"
            else -> "\u2022\u2022"
        }
        return "$digit$dots"
    }

    /** Fraction (0..1] of the tine length: lowest note has the longest tine. */
    fun tineLengthFraction(key: Int): Float = 1.0f - (key - 1) * 0.034f
}
