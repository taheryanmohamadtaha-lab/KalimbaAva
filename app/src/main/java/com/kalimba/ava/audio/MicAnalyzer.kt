package com.kalimba.ava.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.kalimba.ava.core.NoteOnset
import com.kalimba.ava.core.NoteTracker
import com.kalimba.ava.core.PitchFrame
import com.kalimba.ava.core.YinPitchDetector

/** One analysis step delivered on the main thread. Only real detector output is reported. */
data class MicFrame(val frame: PitchFrame, val onset: NoteOnset?)

/**
 * Microphone -> YIN -> NoteTracker pipeline (single-note mode).
 * Polyphonic analysis is NOT implemented; the UI says so.
 */
class MicAnalyzer(
    private val noiseGate: Double,
    private val minConfidence: Double,
    private val a4: Double,
) {
    companion object { const val SAMPLE_RATE = 44100; const val WINDOW = 2048; const val HOP = 512 }

    @Volatile private var running = false
    private var thread: Thread? = null
    private val main = Handler(Looper.getMainLooper())
    /** System.nanoTime() corresponding to analyzer time 0 (set when capture starts). */
    @Volatile var startNano: Long = 0L
        private set

    @SuppressLint("MissingPermission")
    fun start(onFrame: (MicFrame) -> Unit, onError: (String) -> Unit) {
        if (running) return
        running = true
        thread = Thread {
            val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuf <= 0) { main.post { onError("میکروفون در دسترس نیست") }; running = false; return@Thread }
            val rec = try {
                AudioRecord(MediaRecorder.AudioSource.UNPROCESSED, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, WINDOW * 4))
                    .takeIf { it.state == AudioRecord.STATE_INITIALIZED }
                    ?: AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, WINDOW * 4))
            } catch (e: Exception) { main.post { onError("خطا در باز کردن میکروفون") }; running = false; return@Thread }
            if (rec.state != AudioRecord.STATE_INITIALIZED) { main.post { onError("میکروفون آماده نشد") }; rec.release(); running = false; return@Thread }
            val yin = YinPitchDetector(SAMPLE_RATE, WINDOW)
            val tracker = NoteTracker(noiseGate = noiseGate, minConfidence = minConfidence, a4 = a4)
            val ring = FloatArray(WINDOW)
            val window = FloatArray(WINDOW)
            val hop = ShortArray(HOP)
            var filled = 0
            var totalRead = 0L
            rec.startRecording()
            startNano = System.nanoTime()
            try {
                while (running) {
                    val n = rec.read(hop, 0, HOP)
                    if (n <= 0) continue
                    // slide the window
                    System.arraycopy(ring, n, ring, 0, WINDOW - n)
                    for (i in 0 until n) ring[WINDOW - n + i] = hop[i] / 32768f
                    totalRead += n
                    filled = minOf(WINDOW, filled + n)
                    if (filled < WINDOW) continue
                    System.arraycopy(ring, 0, window, 0, WINDOW)
                    val rms = YinPitchDetector.rms(window)
                    val pitch = if (rms >= noiseGate * 0.5) yin.detect(window) else null
                    // timestamp = centre of the analysis window
                    val t = (totalRead - WINDOW / 2).toDouble() / SAMPLE_RATE
                    val frame = PitchFrame(t, rms, pitch)
                    val onset = tracker.feed(frame)
                    main.post { if (running) onFrame(MicFrame(frame, onset)) }
                }
            } finally {
                runCatching { rec.stop() }
                rec.release()
            }
        }.apply { name = "kalimba-mic"; priority = Thread.MAX_PRIORITY; start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }
}
