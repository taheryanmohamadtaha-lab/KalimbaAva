package com.kalimba.ava.core

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Physical-modelling kalimba voice. A clamped steel tine rings with a fundamental plus
 * strongly inharmonic overtones (bar partial ratios ~1 : 6.27 : 17.55), a slight beating
 * pair gives the characteristic shimmer, and a short low "knock" models the wooden body.
 * The result is rendered once into a PCM buffer per key.
 */
object KalimbaSynth {
    fun render(key: Int, sampleRate: Int, sustain: Boolean, a4: Double = NoteMath.DEFAULT_A4): FloatArray {
        val f0 = KalimbaLayout.frequencyForKey(key, a4)
        val seconds = if (sustain) 4.0 else 2.6
        val n = (seconds * sampleRate).toInt()
        val out = FloatArray(n)
        val tilt = (261.63 / f0).pow(0.35)
        val tau1 = (if (sustain) 2.4 else 1.1) * tilt
        val tau2 = (if (sustain) 0.45 else 0.22) * tilt
        val tau3 = 0.07 * tilt
        val w0 = 2.0 * PI * f0
        val w1 = 2.0 * PI * f0 * 1.0021
        val w2 = 2.0 * PI * f0 * 6.27
        val w3 = 2.0 * PI * f0 * 17.55
        val knockF = 2.0 * PI * 170.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val attack = if (t < 0.0012) t / 0.0012 else 1.0
            var s = 1.0 * sin(w0 * t) * exp(-t / tau1)
            s += 0.28 * sin(w1 * t) * exp(-t / (tau1 * 0.8))
            s += 0.17 * sin(w2 * t) * exp(-t / tau2)
            s += 0.035 * sin(w3 * t) * exp(-t / tau3)
            s += 0.16 * sin(knockF * t) * exp(-t / 0.028)
            out[i] = (s * attack).toFloat()
        }
        var peak = 0f
        for (v in out) if (kotlin.math.abs(v) > peak) peak = kotlin.math.abs(v)
        val g = if (peak > 0f) 0.8f / peak else 1f
        val fade = (0.03 * sampleRate).toInt()
        for (i in 0 until n) {
            var v = out[i] * g
            if (i > n - fade) v *= (n - i).toFloat() / fade
            out[i] = v
        }
        return out
    }

    /** Short metronome click buffers. type: 0 = soft beep, 1 = wood, 2 = tick. */
    fun click(sampleRate: Int, accent: Boolean, type: Int): FloatArray {
        val dur = if (type == 1) 0.06 else 0.04
        val n = (dur * sampleRate).toInt()
        val f = when (type) {
            0 -> if (accent) 1500.0 else 1000.0
            1 -> if (accent) 900.0 else 650.0
            else -> if (accent) 2600.0 else 1900.0
        }
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t / (if (type == 1) 0.012 else 0.008))
            val body = sin(2.0 * PI * f * t)
            val extra = if (type == 1) 0.5 * sin(2.0 * PI * f * 2.4 * t) else 0.0
            out[i] = ((body + extra) * env * (if (accent) 0.9 else 0.6)).toFloat()
        }
        return out
    }
}
