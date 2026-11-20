package com.kalimba.ava.audio

import android.os.Handler
import android.os.Looper

/** Drift-free metronome on its own thread; beats are scheduled against System.nanoTime(). */
class Metronome(private val engine: AudioEngine) {
    @Volatile var bpm = 90
    @Volatile var beatsPerBar = 4
    /** Note value of the pulse: 4 => quarter, 8 => eighth. */
    @Volatile var denominator = 4
    @Volatile var clickType = 0
    @Volatile private var running = false
    private var thread: Thread? = null
    private val main = Handler(Looper.getMainLooper())

    val isRunning get() = running

    fun start(onBeat: (Int) -> Unit) {
        if (running) return
        running = true
        thread = Thread {
            var next = System.nanoTime() + 100_000_000L
            var beat = 0
            while (running) {
                val now = System.nanoTime()
                if (now >= next) {
                    val b = beat % beatsPerBar
                    engine.click(b == 0, clickType)
                    main.post { if (running) onBeat(b) }
                    beat++
                    next += (60_000_000_000L / bpm) * 4 / denominator
                } else {
                    val wait = next - now
                    if (wait > 3_000_000L) Thread.sleep((wait - 2_000_000L) / 1_000_000L) else Thread.yield()
                }
            }
        }.apply { name = "kalimba-metronome"; priority = Thread.MAX_PRIORITY; start() }
    }

    fun stop() {
        running = false
        thread?.join(300)
        thread = null
    }
}
