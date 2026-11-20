package com.kalimba.ava.core

import kotlin.math.abs
import kotlin.math.sqrt

data class ExpectedNote(val timeSec: Double, val key: Int, val eventIndex: Int = 0)
data class PlayedNote(val timeSec: Double, val key: Int)

enum class Judgement { CORRECT, WRONG, MISSED }

data class NoteResult(val expected: ExpectedNote, val judgement: Judgement, val offsetMs: Double?, val playedKey: Int?)

data class ScoreResult(
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val missed: Int,
    val extra: Int,
    val accuracy: Double,
    val timingScore: Double,
    val meanOffsetMs: Double,
    val offsetStdMs: Double,
    val details: List<NoteResult>,
)

/** Compares what was played with the expected melody. Nothing here is simulated: every number derives from the two input lists. */
object PracticeScorer {

    fun flatten(song: Song, speed: Double, fromEvent: Int = 0, toEvent: Int = song.events.lastIndex): List<ExpectedNote> {
        if (song.events.isEmpty()) return emptyList()
        val secPerBeat = 60.0 / (song.tempo * speed)
        val t0 = song.events[fromEvent].startBeat
        val out = ArrayList<ExpectedNote>()
        for (i in fromEvent..toEvent) {
            val e = song.events[i]
            for (k in e.keys) out.add(ExpectedNote((e.startBeat - t0) * secPerBeat, k, i))
        }
        return out
    }

    fun evaluate(expected: List<ExpectedNote>, played: List<PlayedNote>, windowSec: Double): ScoreResult {
        val usedPlayed = BooleanArray(played.size)
        val results = arrayOfNulls<NoteResult>(expected.size)

        // pass 1: right key, within the timing window (nearest in time wins)
        for ((i, ex) in expected.withIndex()) {
            var best = -1
            var bestDist = Double.MAX_VALUE
            for ((j, p) in played.withIndex()) {
                if (usedPlayed[j] || p.key != ex.key) continue
                val d = abs(p.timeSec - ex.timeSec)
                if (d <= windowSec && d < bestDist) { best = j; bestDist = d }
            }
            if (best >= 0) {
                usedPlayed[best] = true
                results[i] = NoteResult(ex, Judgement.CORRECT, (played[best].timeSec - ex.timeSec) * 1000.0, ex.key)
            }
        }
        // pass 2: a different key played at the right moment = wrong note
        for ((i, ex) in expected.withIndex()) {
            if (results[i] != null) continue
            var best = -1
            var bestDist = Double.MAX_VALUE
            for ((j, p) in played.withIndex()) {
                if (usedPlayed[j]) continue
                val d = abs(p.timeSec - ex.timeSec)
                if (d <= windowSec && d < bestDist) { best = j; bestDist = d }
            }
            if (best >= 0) {
                usedPlayed[best] = true
                results[i] = NoteResult(ex, Judgement.WRONG, null, played[best].key)
            }
        }
        val details = expected.indices.map { results[it] ?: NoteResult(expected[it], Judgement.MISSED, null, null) }
        val correct = details.count { it.judgement == Judgement.CORRECT }
        val wrong = details.count { it.judgement == Judgement.WRONG }
        val missed = details.count { it.judgement == Judgement.MISSED }
        val extra = usedPlayed.count { !it }
        val offsets = details.mapNotNull { it.offsetMs }
        val mean = if (offsets.isEmpty()) 0.0 else offsets.average()
        val std = if (offsets.size < 2) 0.0 else sqrt(offsets.sumOf { (it - mean) * (it - mean) } / offsets.size)
        val timing = if (offsets.isEmpty()) 0.0
        else offsets.map { 1.0 - (abs(it) / 1000.0) / windowSec }.average() * 100.0
        val total = expected.size
        val accuracy = if (total == 0) 0.0 else correct * 100.0 / total
        return ScoreResult(total, correct, wrong, missed, extra, accuracy, timing, mean, std, details)
    }

    /** Persian practice tips derived only from the detected mistakes. */
    fun suggestions(r: ScoreResult): List<String> {
        val tips = ArrayList<String>()
        if (r.total == 0) return tips
        if (r.accuracy >= 95 && r.extra == 0) tips.add("عالی بود! سرعت را یک پله بالاتر ببر یا در حالت اجرا امتحان کن.")
        if (r.missed > 0) {
            val worst = r.details.filter { it.judgement == Judgement.MISSED }
                .groupingBy { it.expected.key }.eachCount().maxByOrNull { it.value }
            if (worst != null) tips.add("بیشترین نوت‌های از‌دست‌رفته روی کلید ${KalimbaLayout.numberLabel(worst.key)} (${KalimbaLayout.noteLabel(worst.key)}) بود؛ این کلید را جداگانه تمرین کن.")
        }
        if (r.wrong > 0) {
            val w = r.details.filter { it.judgement == Judgement.WRONG }
                .groupingBy { it.expected.key to it.playedKey }.eachCount().maxByOrNull { it.value }
            val pk = w?.key?.second
            if (w != null && pk != null) tips.add("چند بار به‌جای ${KalimbaLayout.noteLabel(w.key.first)} نوت ${KalimbaLayout.noteLabel(pk)} نواخته شد؛ جای انگشت را دوباره بررسی کن.")
        }
        if (r.extra > 0) tips.add("${r.extra} نوت اضافه نواخته شد؛ آرام‌تر و دقیق‌تر بنواز.")
        if (r.correct >= 3) {
            if (r.meanOffsetMs > 70) tips.add("به‌طور میانگین حدود ${r.meanOffsetMs.toInt()} میلی‌ثانیه دیرتر از ضرب می‌نوازی؛ کمی زودتر شروع کن.")
            else if (r.meanOffsetMs < -70) tips.add("به‌طور میانگین حدود ${(-r.meanOffsetMs).toInt()} میلی‌ثانیه زودتر از ضرب می‌نوازی؛ منتظر ضرب بمان.")
            if (r.offsetStdMs > 120) tips.add("فاصله‌ی نواختن نوت‌ها ناپایدار است؛ با مترونوم و سرعت پایین‌تر تمرین کن.")
        }
        if (tips.isEmpty()) tips.add("تمرین را ادامه بده؛ نتیجه رو‌به‌بهبود است.")
        return tips
    }
}
