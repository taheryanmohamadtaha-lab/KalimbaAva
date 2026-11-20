package com.kalimba.ava.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "kalimba_prefs")

data class Settings(
    val name: String = "نوازنده",
    val xp: Int = 0,
    val dailyGoalMin: Int = 10,
    val volume: Float = 0.9f,
    val reverb: Boolean = false,
    val sustain: Boolean = false,
    val showNoteNames: Boolean = true,
    val showNumbers: Boolean = true,
    val a4: Float = 440f,
    val noiseGate: Float = 0.012f,
    val minConfidence: Float = 0.85f,
    val theme: Int = 0, // 0 = system, 1 = light, 2 = dark
    val clickType: Int = 0,
    val micLatencyMs: Int = 60,
)

class Prefs(private val ctx: Context) {
    private object K {
        val name = stringPreferencesKey("name")
        val xp = intPreferencesKey("xp")
        val goal = intPreferencesKey("goal")
        val volume = floatPreferencesKey("volume")
        val reverb = booleanPreferencesKey("reverb")
        val sustain = booleanPreferencesKey("sustain")
        val names = booleanPreferencesKey("names")
        val numbers = booleanPreferencesKey("numbers")
        val a4 = floatPreferencesKey("a4")
        val gate = floatPreferencesKey("gate")
        val conf = floatPreferencesKey("conf")
        val theme = intPreferencesKey("theme")
        val click = intPreferencesKey("click")
        val lat = intPreferencesKey("lat")
    }

    val settings: Flow<Settings> = ctx.store.data.map { p ->
        val d = Settings()
        Settings(
            name = p[K.name] ?: d.name, xp = p[K.xp] ?: 0, dailyGoalMin = p[K.goal] ?: d.dailyGoalMin,
            volume = p[K.volume] ?: d.volume, reverb = p[K.reverb] ?: d.reverb, sustain = p[K.sustain] ?: d.sustain,
            showNoteNames = p[K.names] ?: d.showNoteNames, showNumbers = p[K.numbers] ?: d.showNumbers,
            a4 = p[K.a4] ?: d.a4, noiseGate = p[K.gate] ?: d.noiseGate, minConfidence = p[K.conf] ?: d.minConfidence,
            theme = p[K.theme] ?: 0, clickType = p[K.click] ?: 0, micLatencyMs = p[K.lat] ?: d.micLatencyMs,
        )
    }

    private suspend fun <T> set(key: Preferences.Key<T>, v: T) { ctx.store.edit { it[key] = v } }
    suspend fun setName(v: String) = set(K.name, v)
    suspend fun addXp(delta: Int) { ctx.store.edit { it[K.xp] = (it[K.xp] ?: 0) + delta } }
    suspend fun resetXp() = set(K.xp, 0)
    suspend fun setGoal(v: Int) = set(K.goal, v)
    suspend fun setVolume(v: Float) = set(K.volume, v)
    suspend fun setReverb(v: Boolean) = set(K.reverb, v)
    suspend fun setSustain(v: Boolean) = set(K.sustain, v)
    suspend fun setNames(v: Boolean) = set(K.names, v)
    suspend fun setNumbers(v: Boolean) = set(K.numbers, v)
    suspend fun setA4(v: Float) = set(K.a4, v)
    suspend fun setGate(v: Float) = set(K.gate, v)
    suspend fun setConf(v: Float) = set(K.conf, v)
    suspend fun setTheme(v: Int) = set(K.theme, v)
    suspend fun setClick(v: Int) = set(K.click, v)
    suspend fun setLatency(v: Int) = set(K.lat, v)
}
