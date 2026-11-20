package com.kalimba.ava.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.kalimba.ava.core.KalimbaLayout
import com.kalimba.ava.core.KalimbaSynth
import com.kalimba.ava.core.NoteMath
import com.kalimba.ava.core.PolyMixer
import com.kalimba.ava.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Low-latency streaming output (AudioTrack, performance mode LOW_LATENCY) feeding the polyphonic mixer. */
class AudioEngine(private val context: Context, private val prefs: Prefs) {
    @Suppress("DEPRECATION")
    val sampleRate: Int = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC).let { if (it in 8000..192000) it else 44100 }
    val mixer = PolyMixer(sampleRate)

    private val shortBuf = arrayOfNulls<FloatArray>(KalimbaLayout.KEY_COUNT + 1)
    private val longBuf = arrayOfNulls<FloatArray>(KalimbaLayout.KEY_COUNT + 1)
    private val clicks = HashMap<Int, FloatArray>()

    @Volatile var sustain = false
    @Volatile var a4 = 440.0
    private var track: AudioTrack? = null
    @Volatile private var running = false
    private var thread: Thread? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var prepared = false

    init {
        scope.launch {
            prefs.settings.collectLatest { s ->
                mixer.masterVolume = s.volume
                mixer.reverbMix = if (s.reverb) 0.35f else 0f
                sustain = s.sustain
            }
        }
    }

    /** Pre-renders every key once (a few hundred ms on a modern phone) so playback is just memory reads. */
    fun prepare() {
        if (prepared) return
        prepared = true
        scope.launch {
            for (k in 1..KalimbaLayout.KEY_COUNT) sampleFor(k, false)
            for (k in 1..KalimbaLayout.KEY_COUNT) sampleFor(k, true)
        }
    }

    fun sampleFor(key: Int, sustained: Boolean): FloatArray {
        val arr = if (sustained) longBuf else shortBuf
        arr[key]?.let { return it }
        return synchronized(arr) { arr[key] ?: KalimbaSynth.render(key, sampleRate, sustained, a4).also { arr[key] = it } }
    }

    fun start() {
        if (running) return
        prepare()
        val minBytes = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val frames = (minBytes / 2).coerceAtLeast(256)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(frames * 2 * 2)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        running = true
        t.play()
        thread = Thread({
            val chunk = ShortArray(frames / 2)
            while (running) {
                mixer.render(chunk, chunk.size)
                val n = t.write(chunk, 0, chunk.size)
                if (n < 0) break
            }
        }, "kalimba-audio").apply { priority = Thread.MAX_PRIORITY; start() }
    }

    fun stop() {
        running = false
        thread?.join(300)
        thread = null
        track?.let { runCatching { it.stop() }; it.release() }
        track = null
        mixer.clear()
    }

    fun playKey(key: Int, gain: Float = 1f) {
        if (!running) start()
        mixer.trigger(sampleFor(key, sustain), gain)
    }

    fun click(accent: Boolean, type: Int) {
        if (!running) start()
        val id = type * 2 + if (accent) 1 else 0
        val buf = synchronized(clicks) { clicks.getOrPut(id) { KalimbaSynth.click(sampleRate, accent, type) } }
        mixer.trigger(buf, 1f)
    }

    fun stopAll() = mixer.clear()
}
