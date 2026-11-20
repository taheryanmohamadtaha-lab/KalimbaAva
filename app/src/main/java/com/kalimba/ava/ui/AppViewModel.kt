package com.kalimba.ava.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kalimba.ava.KalimbaApp
import com.kalimba.ava.core.Progress
import com.kalimba.ava.core.ScoreResult
import com.kalimba.ava.core.Song
import com.kalimba.ava.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

data class DayMinutes(val epochDay: Long, val minutes: Int)

class AppViewModel(private val app: Application) : AndroidViewModel(app) {
    private val c = (app as KalimbaApp).container
    val content = c.content
    val audio = c.audio
    val prefs = c.prefs
    private val dao = c.db.dao()

    val settings = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val lessonProgress = dao.lessonProgress().map { l -> l.associateBy { it.lessonId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val songProgress = dao.songProgress().map { l -> l.associateBy { it.songId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val recordings = dao.recordings().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val logs = dao.logs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val sessions = dao.allSessions().map { it.sortedByDescending { s -> s.startedAt } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun today(): Long = LocalDate.now().toEpochDay()
    private fun dayOf(ms: Long): Long = java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

    fun practiceDays(l: List<PracticeLogE>): Set<Long> = l.map { dayOf(it.startedAt) }.toSet()
    fun streak(l: List<PracticeLogE>): Int = Progress.streak(practiceDays(l), today())
    fun minutesToday(l: List<PracticeLogE>): Int = l.filter { dayOf(it.startedAt) == today() }.sumOf { it.durationSec } / 60
    fun weekly(l: List<PracticeLogE>): List<DayMinutes> {
        val t = today()
        return (6 downTo 0).map { off ->
            val d = t - off
            DayMinutes(d, l.filter { dayOf(it.startedAt) == d }.sumOf { it.durationSec } / 60)
        }
    }

    // ---- lessons ----
    fun completeLesson(id: String) = viewModelScope.launch {
        val already = lessonProgress.value[id]?.completed == true
        dao.upsertLesson(LessonProgressE(id, true, true, System.currentTimeMillis()))
        if (!already) prefs.addXp(Progress.XP_LESSON)
    }

    // ---- songs ----
    fun toggleFavorite(id: String) = viewModelScope.launch {
        val p = dao.songProgressOnce(id) ?: SongProgressE(id, 0.0, 0, false, false, 0)
        dao.upsertSong(p.copy(favorite = !p.favorite))
    }

    /** Persists a finished practice session and updates XP, song progress and the practice log. */
    fun saveSession(song: Song?, label: String, r: ScoreResult, mode: String, input: String, speedPct: Int, startedAt: Long, durationSec: Int, wholeSong: Boolean) =
        viewModelScope.launch {
            val id = song?.id ?: "free"
            dao.insertSession(SessionE(0, id, label, startedAt, durationSec, r.total, r.correct, r.wrong, r.missed, r.accuracy, r.timingScore, mode, input, speedPct))
            dao.insertLog(PracticeLogE(0, startedAt, durationSec, "song", label))
            var xp = Progress.sessionXp(r.accuracy)
            if (song != null) {
                val p = dao.songProgressOnce(id) ?: SongProgressE(id, 0.0, 0, false, false, 0)
                val completed = p.completed || (wholeSong && r.accuracy >= 80.0)
                if (completed && !p.completed) xp += Progress.XP_SONG_FIRST
                dao.upsertSong(p.copy(bestAccuracy = maxOf(p.bestAccuracy, r.accuracy), plays = p.plays + 1, completed = completed, lastPlayed = System.currentTimeMillis()))
            }
            prefs.addXp(xp)
        }

    fun logPractice(kind: String, label: String, startedAt: Long, durationSec: Int) = viewModelScope.launch {
        if (durationSec < 5) return@launch
        dao.insertLog(PracticeLogE(0, startedAt, durationSec, kind, label))
        prefs.addXp((durationSec / 60) * 2)
    }

    // ---- recordings ----
    fun recordingsDir(): File = File(app.filesDir, "recordings").apply { mkdirs() }

    fun addRecording(name: String, file: File, kind: String, durationMs: Long) = viewModelScope.launch {
        dao.insertRecording(RecordingE(0, name, file.absolutePath, kind, durationMs, System.currentTimeMillis()))
    }
    fun renameRecording(id: Long, name: String) = viewModelScope.launch { dao.renameRecording(id, name) }
    fun deleteRecording(r: RecordingE) = viewModelScope.launch {
        withContext(Dispatchers.IO) { runCatching { File(r.path).delete() } }
        dao.deleteRecording(r.id)
    }

    // ---- settings ----
    fun setVolume(v: Float) = viewModelScope.launch { prefs.setVolume(v) }
    fun setReverb(v: Boolean) = viewModelScope.launch { prefs.setReverb(v) }
    fun setSustain(v: Boolean) = viewModelScope.launch { prefs.setSustain(v) }
    fun setNames(v: Boolean) = viewModelScope.launch { prefs.setNames(v) }
    fun setNumbers(v: Boolean) = viewModelScope.launch { prefs.setNumbers(v) }
    fun setA4(v: Float) = viewModelScope.launch { prefs.setA4(v) }
    fun setGate(v: Float) = viewModelScope.launch { prefs.setGate(v) }
    fun setConf(v: Float) = viewModelScope.launch { prefs.setConf(v) }
    fun setTheme(v: Int) = viewModelScope.launch { prefs.setTheme(v) }
    fun setClick(v: Int) = viewModelScope.launch { prefs.setClick(v) }
    fun setGoal(v: Int) = viewModelScope.launch { prefs.setGoal(v) }
    fun setName(v: String) = viewModelScope.launch { prefs.setName(v) }
    fun setLatency(v: Int) = viewModelScope.launch { prefs.setLatency(v) }

    fun deleteAllData() = viewModelScope.launch {
        withContext(Dispatchers.IO) { recordings.value.forEach { runCatching { File(it.path).delete() } } }
        dao.clearLessons(); dao.clearSongs(); dao.clearSessions(); dao.clearLogs(); dao.clearRecordings(); prefs.resetXp()
    }

    fun importPack(text: String, name: String): String? = content.importPack(text, name)
}
