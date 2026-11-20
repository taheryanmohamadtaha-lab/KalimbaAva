package com.kalimba.ava.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lesson_progress")
data class LessonProgressE(@PrimaryKey val lessonId: String, val completed: Boolean, val quizPassed: Boolean, val updatedAt: Long)

@Entity(tableName = "song_progress")
data class SongProgressE(
    @PrimaryKey val songId: String,
    val bestAccuracy: Double,
    val plays: Int,
    val completed: Boolean,
    val favorite: Boolean,
    val lastPlayed: Long,
)

@Entity(tableName = "sessions")
data class SessionE(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val title: String,
    val startedAt: Long,
    val durationSec: Int,
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val missed: Int,
    val accuracy: Double,
    val timing: Double,
    val mode: String,
    val input: String,
    val speedPct: Int,
)

/** Every period of practice (song session, free play, stopwatch) for goals, streaks and weekly charts. */
@Entity(tableName = "practice_log")
data class PracticeLogE(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val durationSec: Int,
    val kind: String,
    val label: String,
)

@Entity(tableName = "recordings")
data class RecordingE(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    val kind: String, // "audio" (microphone) or "performance" (virtual kalimba)
    val durationMs: Long,
    val createdAt: Long,
)

@Dao
interface AppDao {
    @Query("SELECT * FROM lesson_progress") fun lessonProgress(): Flow<List<LessonProgressE>>
    @Upsert suspend fun upsertLesson(e: LessonProgressE)

    @Query("SELECT * FROM song_progress") fun songProgress(): Flow<List<SongProgressE>>
    @Query("SELECT * FROM song_progress WHERE songId = :id") suspend fun songProgressOnce(id: String): SongProgressE?
    @Upsert suspend fun upsertSong(e: SongProgressE)

    @Insert suspend fun insertSession(e: SessionE): Long
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC LIMIT :n") fun recentSessions(n: Int): Flow<List<SessionE>>
    @Query("SELECT * FROM sessions") fun allSessions(): Flow<List<SessionE>>

    @Insert suspend fun insertLog(e: PracticeLogE): Long
    @Query("SELECT * FROM practice_log ORDER BY startedAt DESC") fun logs(): Flow<List<PracticeLogE>>

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC") fun recordings(): Flow<List<RecordingE>>
    @Insert suspend fun insertRecording(e: RecordingE): Long
    @Query("UPDATE recordings SET name = :name WHERE id = :id") suspend fun renameRecording(id: Long, name: String)
    @Query("DELETE FROM recordings WHERE id = :id") suspend fun deleteRecording(id: Long)
    @Query("SELECT * FROM recordings") suspend fun recordingsOnce(): List<RecordingE>

    @Query("DELETE FROM lesson_progress") suspend fun clearLessons()
    @Query("DELETE FROM song_progress") suspend fun clearSongs()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM practice_log") suspend fun clearLogs()
    @Query("DELETE FROM recordings") suspend fun clearRecordings()
}

/**
 * Migration strategy: bump `version` and add a Migration object to `MIGRATIONS` for every schema change.
 * Destructive fallback is deliberately NOT enabled so learner progress is never silently lost.
 */
@Database(
    entities = [LessonProgressE::class, SongProgressE::class, SessionE::class, PracticeLogE::class, RecordingE::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        val MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf()
        @Volatile private var inst: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "kalimba_ava.db")
                .addMigrations(*MIGRATIONS)
                .build().also { inst = it }
        }
    }
}
