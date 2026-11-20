package com.kalimba.ava.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class PitchResult(val frequency: Double, val confidence: Double)

/**
 * YIN fundamental-frequency estimator (de Cheveigné & Kawahara, 2002).
 * confidence = 1 - d'(tau) of the chosen lag, i.e. it is computed from the signal itself.
 */
class YinPitchDetector(
    private val sampleRate: Int,
    val bufferSize: Int = 2048,
    private val threshold: Double = 0.15,
    private val minFreq: Double = 180.0,
    private val maxFreq: Double = 1500.0,
) {
    private val tauMax = min(bufferSize / 2, (sampleRate / minFreq).toInt())
    private val tauMin = max(2, (sampleRate / maxFreq).toInt())
    private val cmnd = DoubleArray(tauMax + 1)

    fun detect(buf: FloatArray): PitchResult? {
        require(buf.size >= bufferSize) { "buffer too small" }
        val w = bufferSize - tauMax
        // difference function
        cmnd[0] = 1.0
        var running = 0.0
        for (tau in 1..tauMax) {
            var sum = 0.0
            for (j in 0 until w) {
                val d = (buf[j] - buf[j + tau]).toDouble()
                sum += d * d
            }
            running += sum
            cmnd[tau] = if (running == 0.0) 1.0 else sum * tau / running
        }
        // absolute threshold: first dip below threshold, then walk to its minimum
        var tau = -1
        var t = tauMin
        while (t < tauMax) {
            if (cmnd[t] < threshold) {
                while (t + 1 < tauMax && cmnd[t + 1] < cmnd[t]) t++
                tau = t
                break
            }
            t++
        }
        if (tau < 0) {
            // fall back to global minimum, but only if it is reasonably periodic
            var best = tauMin
            for (k in tauMin until tauMax) if (cmnd[k] < cmnd[best]) best = k
            if (cmnd[best] > 0.35) return null
            tau = best
        }
        // parabolic interpolation
        var better = tau.toDouble()
        if (tau in 1 until tauMax) {
            val s0 = cmnd[tau - 1]; val s1 = cmnd[tau]; val s2 = cmnd[tau + 1]
            val denom = 2.0 * (2.0 * s1 - s2 - s0)
            if (denom != 0.0) better = tau + (s2 - s0) / denom
        }
        val freq = sampleRate / better
        return PitchResult(freq, (1.0 - cmnd[tau]).coerceIn(0.0, 1.0))
    }

    companion object {
        fun rms(buf: FloatArray, n: Int = buf.size): Double {
            var s = 0.0
            for (i in 0 until n) s += buf[i] * buf[i]
            return sqrt(s / n)
        }
    }
}
