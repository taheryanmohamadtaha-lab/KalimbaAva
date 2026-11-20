package com.kalimba.ava.core

/** One musical event: one or more kalimba keys sounding together. Times are in beats (quarter-note units). */
data class NoteEvent(val startBeat: Double, val durBeats: Double, val keys: List<Int>)

data class SongSection(val index: Int, val firstEvent: Int, val lastEvent: Int, val startBeat: Double, val endBeat: Double)

data class Song(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val composer: String,
    val artist: String,
    val category: String,
    val difficulty: String,
    val tempo: Int,
    val timeSignature: String,
    val events: List<NoteEvent>,
    val license: String,
    val sourceNote: String,
    val packId: String = "",
) {
    val totalBeats: Double get() = events.lastOrNull()?.let { it.startBeat + it.durBeats } ?: 0.0
    val durationSeconds: Double get() = totalBeats * 60.0 / tempo
    /** Bar length in quarter-note beats (6/8 -> 3.0, 3/4 -> 3.0, 4/4 -> 4.0). */
    val barBeats: Double get() {
        val n = timeSignature.substringBefore('/').toIntOrNull() ?: 4
        val d = timeSignature.substringAfter('/').toIntOrNull() ?: 4
        return n * 4.0 / d
    }
    val noteCount: Int get() = events.sumOf { it.keys.size }

    /** Practice sections of two bars each. */
    fun sections(): List<SongSection> {
        if (events.isEmpty()) return emptyList()
        val len = barBeats * 2.0
        val out = ArrayList<SongSection>()
        var cur = 0
        var first = 0
        for (i in events.indices) {
            val idx = (events[i].startBeat / len + 1e-9).toInt()
            if (idx != cur) {
                out.add(SongSection(out.size, first, i - 1, events[first].startBeat, events[i].startBeat))
                first = i
                cur = idx
            }
        }
        out.add(SongSection(out.size, first, events.size - 1, events[first].startBeat, totalBeats))
        return out
    }

    /** Numbered tablature, one string per event. */
    fun tablature(): List<String> = events.map { e ->
        if (e.keys.isEmpty()) "-" else e.keys.joinToString("+") { KalimbaLayout.numberLabel(it) }
    }
}

object SongParser {
    /**
     * Grammar (whitespace separated, '|' ignored):
     *   C4:1  note with duration in beats      R:2  rest
     *   C4+E4+G4:2  chord                      duration may be "1/3"; default 1
     */
    fun parseSequence(text: String): Result<List<NoteEvent>> {
        val events = ArrayList<NoteEvent>()
        var beat = 0.0
        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() && it != "|" }
        for (tok in tokens) {
            val pitchPart = tok.substringBefore(':')
            val durPart = if (tok.contains(':')) tok.substringAfter(':') else "1"
            val dur = parseDuration(durPart)
                ?: return Result.failure(IllegalArgumentException("bad duration in '$tok'"))
            if (dur <= 0.0) return Result.failure(IllegalArgumentException("non-positive duration in '$tok'"))
            if (pitchPart.equals("R", ignoreCase = true)) {
                beat += dur
                continue
            }
            val keys = ArrayList<Int>()
            for (p in pitchPart.split('+')) {
                val midi = NoteMath.parsePitch(p)
                    ?: return Result.failure(IllegalArgumentException("bad pitch '$p' in '$tok'"))
                val key = KalimbaLayout.keyForMidi(midi)
                    ?: return Result.failure(IllegalArgumentException("pitch '$p' is not on a C-major 17-key kalimba"))
                keys.add(key)
            }
            events.add(NoteEvent(beat, dur, keys))
            beat += dur
        }
        if (events.isEmpty()) return Result.failure(IllegalArgumentException("no notes"))
        return Result.success(events)
    }

    private fun parseDuration(s: String): Double? {
        if (s.contains('/')) {
            val a = s.substringBefore('/').toDoubleOrNull() ?: return null
            val b = s.substringAfter('/').toDoubleOrNull() ?: return null
            if (b == 0.0) return null
            return a / b
        }
        return s.toDoubleOrNull()
    }

    private val CATEGORIES = setOf(
        "iranian_nostalgia", "iranian_traditional", "iranian_folk", "iranian_happy", "iranian_romantic",
        "iranian_calm", "iranian_kids", "iranian_film",
        "classical", "folk", "popular", "relaxing", "movie", "beginner", "advanced",
    )
    private val DIFFICULTIES = setOf("beginner", "intermediate", "advanced", "professional")
    private val LICENSES = setOf("public_domain", "original_cc0", "cc_by", "licensed")

    /** Returns a list of validation errors; empty means the song is valid. */
    fun validate(song: Song): List<String> {
        val e = ArrayList<String>()
        if (song.id.isBlank()) e.add("id is empty")
        if (song.titleFa.isBlank()) e.add("title_fa is empty")
        if (song.category !in CATEGORIES) e.add("unknown category '${song.category}'")
        if (song.difficulty !in DIFFICULTIES) e.add("unknown difficulty '${song.difficulty}'")
        if (song.tempo !in 20..240) e.add("tempo out of range: ${song.tempo}")
        if (!Regex("^[1-9][0-9]?/(2|4|8|16)$").matches(song.timeSignature)) e.add("bad time signature '${song.timeSignature}'")
        if (song.license !in LICENSES) e.add("unknown license '${song.license}'")
        if (song.events.isEmpty()) e.add("no notes")
        return e
    }
}
