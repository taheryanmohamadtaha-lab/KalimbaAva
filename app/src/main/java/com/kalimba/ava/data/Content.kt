package com.kalimba.ava.data

import android.content.Context
import android.util.Log
import com.kalimba.ava.core.Song
import com.kalimba.ava.core.SongParser
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class QuizDef(val question: String, val options: List<String>, val answer: Int, val explanation: String)
data class ExerciseDef(val notes: String, val tempo: Int)
data class Lesson(
    val id: String, val level: Int, val order: Int, val title: String, val body: List<String>,
    val highlightKeys: List<Int>, val demo: String, val exercise: ExerciseDef?, val quiz: QuizDef,
)

data class ContentReport(val songs: Int, val lessons: Int, val errors: List<String>)

/**
 * Loads data-driven content. Built-in packs live in assets/; user packs dropped into
 * the files/songpacks folder (any .json pack, or imported from the Songs screen) are loaded automatically.
 * Packs with a newer schema_version than this app understands are rejected with a message.
 */
class Content(private val ctx: Context) {
    companion object {
        const val SCHEMA = 1
        val CATEGORY_TITLES = linkedMapOf(
            "iranian_nostalgia" to "نوستالژی ایرانی", "iranian_traditional" to "موسیقی سنتی", "iranian_folk" to "موسیقی محلی",
            "iranian_happy" to "ملودی‌های شاد", "iranian_romantic" to "ملودی‌های عاشقانه", "iranian_calm" to "موسیقی آرام",
            "iranian_kids" to "موسیقی کودکانه", "iranian_film" to "فیلم و سریال",
            "classical" to "ملودی‌های کلاسیک", "folk" to "فولک سنتی جهان", "popular" to "ملودی‌های محبوب",
            "relaxing" to "موسیقی آرام (جهانی)", "movie" to "موسیقی فیلم", "beginner" to "ملودی‌های مبتدی", "advanced" to "تنظیم‌های پیشرفته",
        )
        val LEVEL_TITLES = mapOf(1 to "سطح ۱ – مبتدی", 2 to "سطح ۲ – متوسط", 3 to "سطح ۳ – پیشرفته", 4 to "سطح ۴ – حرفه‌ای")
        fun difficultyFa(d: String) = when (d) { "beginner" -> "مبتدی"; "intermediate" -> "متوسط"; "advanced" -> "پیشرفته"; else -> "حرفه‌ای" }
        fun licenseFa(l: String) = when (l) { "public_domain" -> "حوزه‌ی عمومی"; "original_cc0" -> "اثر اصلی (CC0)"; "cc_by" -> "CC BY"; else -> "دارای مجوز" }
    }

    val songs: List<Song>
    val lessons: List<Lesson>
    val report: ContentReport

    init {
        val errors = ArrayList<String>()
        songs = loadSongs(errors)
        lessons = loadLessons(errors)
        report = ContentReport(songs.size, lessons.size, errors)
    }

    private fun readAsset(path: String): String = ctx.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun loadSongs(errors: MutableList<String>): List<Song> {
        val texts = ArrayList<Pair<String, String>>()
        ctx.assets.list("songs")?.filter { it.endsWith(".json") }?.sorted()?.forEach { texts.add(it to readAsset("songs/$it")) }
        File(ctx.filesDir, "songpacks").listFiles { f -> f.extension == "json" }?.sortedBy { it.name }?.forEach {
            runCatching { texts.add(it.name to it.readText()) }
        }
        val out = LinkedHashMap<String, Song>()
        for ((name, text) in texts) {
            try {
                val pack = JSONObject(text)
                val schema = pack.optInt("schema_version", 1)
                if (schema > SCHEMA) { errors.add("$name: schema $schema is newer than supported"); continue }
                val packId = pack.optString("pack_id", name)
                val arr = pack.getJSONArray("songs")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val parsed = SongParser.parseSequence(o.optString("notes"))
                    if (parsed.isFailure) { errors.add("$name/${o.optString("id")}: ${parsed.exceptionOrNull()?.message}"); continue }
                    val song = Song(
                        id = o.optString("id"), titleFa = o.optString("title_fa"), titleEn = o.optString("title_en"),
                        composer = o.optString("composer"), artist = o.optString("artist"), category = o.optString("category"),
                        difficulty = o.optString("difficulty"), tempo = o.optInt("tempo", 90),
                        timeSignature = o.optString("time_signature", "4/4"), events = parsed.getOrThrow(),
                        license = o.optString("license"), sourceNote = o.optString("source"), packId = packId,
                    )
                    val v = SongParser.validate(song)
                    if (v.isNotEmpty()) { errors.add("$name/${song.id}: ${v.joinToString()}"); continue }
                    if (out.containsKey(song.id)) { errors.add("$name/${song.id}: duplicate id"); continue }
                    out[song.id] = song
                }
            } catch (e: Exception) {
                Log.w("Content", "bad pack $name", e); errors.add("$name: ${e.message}")
            }
        }
        return out.values.toList()
    }

    private fun loadLessons(errors: MutableList<String>): List<Lesson> {
        val out = ArrayList<Lesson>()
        try {
            val pack = JSONObject(readAsset("lessons/lessons.json"))
            val arr = pack.getJSONArray("lessons")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val q = o.getJSONObject("quiz")
                val opts = q.getJSONArray("options").let { a -> List(a.length()) { a.getString(it) } }
                val ex = o.optJSONObject("exercise")?.let { ExerciseDef(it.getString("notes"), it.optInt("tempo", 70)) }
                val keys = o.getJSONArray("highlight_keys").let { a -> List(a.length()) { a.getInt(it) } }
                val body = o.getJSONArray("body_fa").let { a -> List(a.length()) { a.getString(it) } }
                val lesson = Lesson(o.getString("id"), o.getInt("level"), o.optInt("order", i), o.getString("title_fa"), body, keys,
                    o.optString("demo"), ex, QuizDef(q.getString("question"), opts, q.getInt("answer"), q.optString("explanation")))
                out.add(lesson)
            }
        } catch (e: Exception) { errors.add("lessons: ${e.message}") }
        return out.sortedBy { it.order }
    }

    fun songById(id: String) = songs.firstOrNull { it.id == id }
    fun lessonById(id: String) = lessons.firstOrNull { it.id == id }

    /** Copies a user-chosen pack into files/songpacks after validating it; returns an error message or null. */
    fun importPack(text: String, fileName: String): String? {
        return try {
            val pack = JSONObject(text)
            if (pack.optInt("schema_version", 1) > SCHEMA) return "نسخه‌ی این بسته جدیدتر از برنامه است."
            val arr: JSONArray = pack.getJSONArray("songs")
            var ok = 0
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (SongParser.parseSequence(o.optString("notes")).isSuccess) ok++
            }
            if (ok == 0) return "هیچ آهنگ معتبری در بسته پیدا نشد."
            val dir = File(ctx.filesDir, "songpacks").apply { mkdirs() }
            val safe = fileName.replace(Regex("[^A-Za-z0-9_.-]"), "_").ifBlank { "pack_${System.currentTimeMillis()}.json" }
            File(dir, if (safe.endsWith(".json")) safe else "$safe.json").writeText(text)
            null
        } catch (e: Exception) { "فایل بسته معتبر نیست: ${e.message}" }
    }
}
