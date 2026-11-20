package com.kalimba.ava.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.kalimba.ava.core.PolyMixer
import java.io.File
import java.io.RandomAccessFile

object WavIO {
    fun header(dataBytes: Int, sampleRate: Int): ByteArray {
        val b = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()); b.putInt(36 + dataBytes); b.put("WAVE".toByteArray())
        b.put("fmt ".toByteArray()); b.putInt(16); b.putShort(1); b.putShort(1)
        b.putInt(sampleRate); b.putInt(sampleRate * 2); b.putShort(2); b.putShort(16)
        b.put("data".toByteArray()); b.putInt(dataBytes)
        return b.array()
    }

    fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
        file.outputStream().buffered().use { o ->
            o.write(header(samples.size * 2, sampleRate))
            val bb = java.nio.ByteBuffer.allocate(samples.size * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            samples.forEach { bb.putShort(it) }
            o.write(bb.array())
        }
    }

    /** Renders recorded virtual-kalimba taps offline (same samples, volume and reverb as live) into a WAV file. */
    fun renderPerformance(events: List<Pair<Long, Int>>, engine: AudioEngine, file: File): Long {
        val sr = engine.sampleRate
        val mixer = PolyMixer(sr)
        mixer.masterVolume = engine.mixer.masterVolume
        mixer.reverbMix = engine.mixer.reverbMix
        val endMs = (events.maxOfOrNull { it.first } ?: 0L) + 2500L
        val total = (endMs * sr / 1000).toInt()
        val out = ShortArray(total)
        val sorted = events.sortedBy { it.first }
        var idx = 0
        val chunk = ShortArray(512)
        var pos = 0
        while (pos < total) {
            val chunkEndMs = (pos + chunk.size) * 1000L / sr
            while (idx < sorted.size && sorted[idx].first < chunkEndMs) {
                mixer.trigger(engine.sampleFor(sorted[idx].second, engine.sustain)); idx++
            }
            val n = minOf(chunk.size, total - pos)
            mixer.render(chunk, n)
            System.arraycopy(chunk, 0, out, pos, n)
            pos += n
        }
        writeWav(file, out, sr)
        return endMs
    }
}

/** Streams microphone audio into a WAV file. */
class WavRecorder(private val file: File) {
    @Volatile private var running = false
    private var thread: Thread? = null
    @Volatile var level: Float = 0f
    @Volatile var elapsedMs: Long = 0L
    private val sr = 44100

    @SuppressLint("MissingPermission")
    fun start(onError: (String) -> Unit): Boolean {
        val minBuf = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) { onError("میکروفون در دسترس نیست"); return false }
        val rec = try { AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2) }
        catch (e: Exception) { onError("خطا در باز کردن میکروفون"); return false }
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); onError("میکروفون آماده نشد"); return false }
        running = true
        thread = Thread {
            val raf = RandomAccessFile(file, "rw")
            raf.setLength(0)
            raf.write(WavIO.header(0, sr))
            val buf = ShortArray(minBuf)
            var bytes = 0
            rec.startRecording()
            try {
                while (running) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    val bb = java.nio.ByteBuffer.allocate(n * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    var peak = 0
                    for (i in 0 until n) { bb.putShort(buf[i]); peak = maxOf(peak, kotlin.math.abs(buf[i].toInt())) }
                    raf.write(bb.array())
                    bytes += n * 2
                    level = peak / 32768f
                    elapsedMs = bytes / 2L * 1000 / sr
                }
            } finally {
                runCatching { rec.stop() }
                rec.release()
                raf.seek(0); raf.write(WavIO.header(bytes, sr)); raf.close()
            }
        }.apply { name = "kalimba-wavrec"; start() }
        return true
    }

    fun stop(): Long {
        running = false
        thread?.join(1000)
        return elapsedMs
    }
}
