package com.kalimba.ava.core

data class NoteOnset(
    val timeSec: Double,
    val midi: Int,
    val frequency: Double,
    val cents: Double,
    val confidence: Double,
)

/** One analysis frame as produced by the microphone loop. */
data class PitchFrame(val timeSec: Double, val rms: Double, val pitch: PitchResult?)

/**
 * Turns noisy per-frame pitch estimates into discrete note events:
 * noise gate, minimum confidence, stable confirmation over N frames,
 * duplicate suppression while a note rings, and re-pluck detection (energy rise).
 */
class NoteTracker(
    var noiseGate: Double = 0.01,
    var minConfidence: Double = 0.85,
    var confirmFrames: Int = 3,
    var a4: Double = NoteMath.DEFAULT_A4,
    private val minRepeatGapSec: Double = 0.15,
    private val maxCentsOff: Double = 40.0,
) {
    private var candidate = -1
    private var count = 0
    private var firstTime = 0.0
    private var confSum = 0.0
    private var lastEmitted = -1
    private var lastEmitTime = -10.0
    private val rmsHist = DoubleArray(4)
    private var histCount = 0

    fun reset() {
        candidate = -1; count = 0; lastEmitted = -1; lastEmitTime = -10.0; histCount = 0; confSum = 0.0
    }

    /** Returns an onset when a new note has just been confirmed. */
    fun feed(f: PitchFrame): NoteOnset? {
        val rms = f.rms
        // energy rise relative to the quietest of the last few frames = a fresh pluck
        var floor = Double.MAX_VALUE
        val n0 = minOf(histCount, rmsHist.size)
        for (i in 0 until n0) if (rmsHist[i] < floor) floor = rmsHist[i]
        val rose = n0 >= 2 && rms > floor * 1.3 && rms > noiseGate * 2.0
        rmsHist[histCount % rmsHist.size] = rms
        histCount++
        val p = f.pitch
        if (rms < noiseGate) {
            candidate = -1; count = 0; lastEmitted = -1
            return null
        }
        if (p == null || p.confidence < minConfidence) {
            candidate = -1; count = 0
            return null
        }
        val n = NoteMath.freqToNote(p.frequency, a4)
        if (kotlin.math.abs(n.cents) > maxCentsOff) { candidate = -1; count = 0; return null }
        if (n.midi == candidate) {
            count++
            confSum += p.confidence
        } else {
            candidate = n.midi; count = 1; firstTime = f.timeSec; confSum = p.confidence
        }
        if (count >= confirmFrames) {
            val repluck = rose && (f.timeSec - lastEmitTime) > minRepeatGapSec
            if (n.midi != lastEmitted || repluck) {
                lastEmitted = n.midi
                lastEmitTime = f.timeSec
                return NoteOnset(firstTime, n.midi, p.frequency, n.cents, confSum / count)
            }
        }
        return null
    }
}
