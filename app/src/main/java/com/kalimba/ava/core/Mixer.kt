package com.kalimba.ava.core

import java.util.concurrent.ConcurrentLinkedQueue

/** Simple Schroeder reverb: 4 parallel combs into 2 series all-pass filters. */
class Reverb(sampleRate: Int) {
    private class Comb(val buf: FloatArray, val fb: Float) {
        var idx = 0; var store = 0f
        fun process(x: Float): Float {
            val y = buf[idx]
            store = y * 0.8f + store * 0.2f
            buf[idx] = x + store * fb
            idx = (idx + 1) % buf.size
            return y
        }
    }
    private class AllPass(val buf: FloatArray) {
        var idx = 0
        fun process(x: Float): Float {
            val b = buf[idx]
            val y = -x + b
            buf[idx] = x + b * 0.5f
            idx = (idx + 1) % buf.size
            return y
        }
    }
    private val scale = sampleRate / 44100.0
    private val combs = intArrayOf(1116, 1188, 1277, 1356).map { Comb(FloatArray((it * scale).toInt()), 0.80f) }
    private val allpass = intArrayOf(556, 441).map { AllPass(FloatArray((it * scale).toInt())) }
    fun process(x: Float): Float {
        var s = 0f
        for (c in combs) s += c.process(x * 0.25f)
        for (a in allpass) s = a.process(s)
        return s
    }
}

/** Polyphonic voice mixer. trigger() may be called from any thread; render() from the audio thread. */
class PolyMixer(sampleRate: Int, private val maxVoices: Int = 32) {
    private class Voice(val data: FloatArray, val gain: Float) { var pos = 0 }
    private val pending = ConcurrentLinkedQueue<Voice>()
    private val voices = ArrayList<Voice>()
    private val reverb = Reverb(sampleRate)

    @Volatile var masterVolume: Float = 0.9f
    @Volatile var reverbMix: Float = 0f

    fun trigger(data: FloatArray, gain: Float = 1f) { pending.add(Voice(data, gain)) }

    @Volatile private var clearRequested = false
    fun clear() { pending.clear(); clearRequested = true }

    val activeVoices: Int get() = voices.size

    fun render(out: ShortArray, frames: Int) {
        if (clearRequested) { voices.clear(); clearRequested = false }
        while (true) {
            val v = pending.poll() ?: break
            voices.add(v)
            while (voices.size > maxVoices) voices.removeAt(0)
        }
        val vol = masterVolume
        val rv = reverbMix
        for (i in 0 until frames) {
            var s = 0f
            var k = 0
            while (k < voices.size) {
                val v = voices[k]
                if (v.pos < v.data.size) { s += v.data[v.pos++] * v.gain; k++ } else { voices.removeAt(k) }
            }
            var y = s * 0.45f
            if (rv > 0f) y += reverb.process(y) * rv * 1.4f
            y *= vol
            if (y > 1f) y = 1f else if (y < -1f) y = -1f
            out[i] = (y * 32767f).toInt().toShort()
        }
    }
}
