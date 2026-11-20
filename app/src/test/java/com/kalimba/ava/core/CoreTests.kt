package com.kalimba.ava.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class NoteMathTest {
    @Test fun a4Is440() { assertEquals(440.0, NoteMath.midiToFreq(69), 1e-9) }
    @Test fun c4Frequency() { assertEquals(261.6256, NoteMath.midiToFreq(60), 1e-3) }
    @Test fun freqToNoteExact() {
        val n = NoteMath.freqToNote(523.25)
        assertEquals("C", n.name); assertEquals(5, n.octave); assertEquals(0.0, n.cents, 0.5)
    }
    @Test fun centsDeviation() {
        val n = NoteMath.freqToNote(440.0 * Math.pow(2.0, 20.0 / 1200.0))
        assertEquals(69, n.midi); assertEquals(20.0, n.cents, 0.01)
    }
    @Test fun calibration() { assertEquals(0.0, NoteMath.freqToNote(432.0, 432.0).cents, 1e-6) }
    @Test fun parsePitch() {
        assertEquals(60, NoteMath.parsePitch("C4")); assertEquals(70, NoteMath.parsePitch("Bb4"))
        assertEquals(66, NoteMath.parsePitch("F#4")); assertNull(NoteMath.parsePitch("H4")); assertNull(NoteMath.parsePitch("C"))
    }
}

class KalimbaLayoutTest {
    @Test fun keyMapping() {
        assertEquals(60, KalimbaLayout.midiForKey(1)); assertEquals(88, KalimbaLayout.midiForKey(17))
        assertEquals(1, KalimbaLayout.keyForMidi(60)); assertNull(KalimbaLayout.keyForMidi(61))
    }
    @Test fun physicalOrderIsPermutation() {
        assertEquals((1..17).toList(), KalimbaLayout.PHYSICAL_ORDER.sorted())
        assertEquals(1, KalimbaLayout.PHYSICAL_ORDER[8]) // centre key is C4
        assertEquals(16, KalimbaLayout.PHYSICAL_ORDER.first()); assertEquals(17, KalimbaLayout.PHYSICAL_ORDER.last())
    }
    @Test fun labels() {
        assertEquals("1", KalimbaLayout.numberLabel(1)); assertEquals("1\u2022", KalimbaLayout.numberLabel(8))
        assertEquals("1\u2022\u2022", KalimbaLayout.numberLabel(15)); assertEquals("C4", KalimbaLayout.noteLabel(1))
    }
}

class SongParserTest {
    @Test fun parsesNotesRestsChords() {
        val ev = SongParser.parseSequence("C4:1 R:1 C4+E4+G4:2 | D4").getOrThrow()
        assertEquals(3, ev.size); assertEquals(2.0, ev[1].startBeat, 1e-9); assertEquals(listOf(1, 3, 5), ev[1].keys)
        assertEquals(4.0, ev[2].startBeat, 1e-9)
    }
    @Test fun triplets() { assertEquals(1.0 / 3.0, SongParser.parseSequence("C4:1/3").getOrThrow()[0].durBeats, 1e-9) }
    @Test fun rejectsBad() {
        assertTrue(SongParser.parseSequence("C3:1").isFailure); assertTrue(SongParser.parseSequence("X4:1").isFailure)
        assertTrue(SongParser.parseSequence("C4:0").isFailure); assertTrue(SongParser.parseSequence("R:2").isFailure)
    }
    @Test fun durationAndSections() {
        val ev = SongParser.parseSequence("C4:1 D4:1 E4:1 F4:1 G4:1 A4:1 B4:1 C5:1 D5:1").getOrThrow()
        val s = Song("a", "الف", "A", "", "", "beginner", "beginner", 120, "4/4", ev, "original_cc0", "")
        assertEquals(4.5, s.durationSeconds, 1e-9); assertEquals(2, s.sections().size)
        assertEquals(8, s.sections()[0].lastEvent - s.sections()[0].firstEvent + 1)
        assertTrue(SongParser.validate(s).isEmpty())
        assertTrue(SongParser.validate(s.copy(tempo = 5)).isNotEmpty())
    }
}

class PracticeScorerTest {
    private val ex = listOf(ExpectedNote(0.0, 1), ExpectedNote(1.0, 2), ExpectedNote(2.0, 3), ExpectedNote(3.0, 4))
    @Test fun perfect() {
        val r = PracticeScorer.evaluate(ex, ex.map { PlayedNote(it.timeSec + 0.02, it.key) }, 0.3)
        assertEquals(4, r.correct); assertEquals(100.0, r.accuracy, 1e-9); assertTrue(r.timingScore > 90)
    }
    @Test fun missedWrongExtra() {
        val played = listOf(PlayedNote(0.05, 1), PlayedNote(1.0, 5), PlayedNote(3.1, 4), PlayedNote(2.5, 7))
        val r = PracticeScorer.evaluate(ex, played, 0.3)
        assertEquals(2, r.correct); assertEquals(1, r.wrong); assertEquals(1, r.missed); assertEquals(1, r.extra)
        assertEquals(50.0, r.accuracy, 1e-9)
    }
    @Test fun outsideWindowIsMissed() {
        val r = PracticeScorer.evaluate(ex, listOf(PlayedNote(0.9, 1)), 0.3)
        assertEquals(0, r.correct); assertEquals(3, r.missed); assertEquals(1, r.wrong); assertEquals(0, r.extra)
    }
    @Test fun nothingPlayedHasNoFakeScore() {
        val r = PracticeScorer.evaluate(ex, emptyList(), 0.3)
        assertEquals(0.0, r.accuracy, 0.0); assertEquals(0.0, r.timingScore, 0.0)
    }
    @Test fun lateOffsetReported() {
        val r = PracticeScorer.evaluate(ex, ex.map { PlayedNote(it.timeSec + 0.15, it.key) }, 0.3)
        assertEquals(150.0, r.meanOffsetMs, 1e-6); assertTrue(PracticeScorer.suggestions(r).any { it.contains("دیرتر") })
    }
    @Test fun flattenAppliesSpeed() {
        val ev = SongParser.parseSequence("C4:1 D4:1").getOrThrow()
        val s = Song("a", "الف", "A", "", "", "beginner", "beginner", 120, "4/4", ev, "original_cc0", "")
        assertEquals(1.0, PracticeScorer.flatten(s, 0.5)[1].timeSec, 1e-9)
    }
}

class AudioProcessingTest {
    private val sr = 44100
    private fun tone(f: Double, n: Int) = FloatArray(n) { (0.5 * sin(2 * PI * f * it / sr)).toFloat() }

    @Test fun yinFindsSine() {
        val d = YinPitchDetector(sr)
        for (f in listOf(261.63, 440.0, 880.0, 1318.5)) {
            val r = d.detect(tone(f, 2048)); assertNotNull(r)
            assertEquals(f, r!!.frequency, f * 0.005); assertTrue(r.confidence > 0.9)
        }
    }
    @Test fun yinRejectsNoise() {
        val rnd = java.util.Random(1)
        val noise = FloatArray(2048) { (rnd.nextGaussian() * 0.1).toFloat() }
        val r = YinPitchDetector(sr).detect(noise)
        assertTrue(r == null || r.confidence < 0.8)
    }
    @Test fun yinSilenceIsNull() { assertNull(YinPitchDetector(sr).detect(FloatArray(2048))) }

    @Test fun synthKeysDetectedAsCorrectNote() {
        val d = YinPitchDetector(sr)
        for (key in 1..17) {
            val buf = KalimbaSynth.render(key, sr, false)
            val frame = buf.copyOfRange(int(0.15), int(0.15) + 2048)
            val r = d.detect(frame); assertNotNull("key $key", r)
            assertEquals("key $key", KalimbaLayout.midiForKey(key), NoteMath.freqToNote(r!!.frequency).midi)
        }
    }
    private fun int(sec: Double) = (sec * sr).toInt()

    @Test fun trackerEmitsOneOnsetPerPluck() {
        val d = YinPitchDetector(sr); val tr = NoteTracker()
        val seq = listOf(1, 3, 3, 8) // C4, E4, E4 again, C5
        val gapSamples = int(0.8)
        val song = FloatArray(gapSamples * seq.size + 4096)
        for ((i, k) in seq.withIndex()) { val b = KalimbaSynth.render(k, sr, false); for (j in 0 until minOf(b.size, gapSamples + 2000)) song[i * gapSamples + j] += b[j] }
        val onsets = ArrayList<NoteOnset>()
        var pos = 0
        while (pos + 2048 <= song.size) {
            val fr = song.copyOfRange(pos, pos + 2048)
            val o = tr.feed(PitchFrame(pos.toDouble() / sr, YinPitchDetector.rms(fr), d.detect(fr)))
            if (o != null) onsets.add(o); pos += 512
        }
        assertEquals(seq.map { KalimbaLayout.midiForKey(it) }, onsets.map { it.midi })
    }
    @Test fun trackerIgnoresQuietNoise() {
        val tr = NoteTracker(noiseGate = 0.02)
        assertNull(tr.feed(PitchFrame(0.0, 0.005, PitchResult(440.0, 0.99))))
    }

    @Test fun mixerSumsVoicesAndClips() {
        val m = PolyMixer(sr); m.masterVolume = 1f
        m.trigger(FloatArray(100) { 1f }); m.trigger(FloatArray(100) { 1f })
        val out = ShortArray(100); m.render(out, 100)
        assertTrue(out[10] > 0); assertEquals(2, 2)
        m.render(out, 100); assertEquals(0, m.activeVoices); assertEquals(0, out[50].toInt())
    }
    @Test fun reverbAddsTail() {
        val m = PolyMixer(sr); m.masterVolume = 1f; m.reverbMix = 0.5f
        m.trigger(FloatArray(10) { 1f })
        val out = ShortArray(4000); m.render(out, 4000)
        assertTrue((1000 until 4000).any { abs(out[it].toInt()) > 0 })
    }
    @Test fun synthLooksSane() {
        val b = KalimbaSynth.render(1, sr, false); assertTrue(b.maxOrNull()!! <= 0.81f); assertEquals(0f, b.last(), 1e-3f)
    }
}

class ProgressTest {
    @Test fun levels() {
        assertEquals(0, Progress.levelIndex(0)); assertEquals(1, Progress.levelIndex(500)); assertEquals(3, Progress.levelIndex(9999))
        assertEquals(0.5f, Progress.levelProgress(250), 1e-6f)
    }
    @Test fun streakCounting() {
        assertEquals(3, Progress.streak(setOf(10L, 9L, 8L, 5L), 10L))
        assertEquals(2, Progress.streak(setOf(9L, 8L), 10L)) // today not practiced yet
        assertEquals(0, Progress.streak(setOf(7L), 10L))
    }
    @Test fun badges() {
        val b = Progress.badges(1, 0, 91.0, 0, 0, 0, false)
        assertTrue(b.first { it.id == "first_lesson" }.earned); assertTrue(b.first { it.id == "accuracy90" }.earned)
        assertTrue(!b.first { it.id == "first_song" }.earned)
    }
}
