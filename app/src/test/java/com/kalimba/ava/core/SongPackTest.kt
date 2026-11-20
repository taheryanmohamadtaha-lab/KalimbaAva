package com.kalimba.ava.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Validates every built-in song pack and the lesson pack shipped in assets/. */
class SongPackTest {
    private val assets = File("src/main/assets")

    @Test fun allBuiltInSongsAreValid() {
        val files = File(assets, "songs").listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }
        assertTrue(files.isNotEmpty())
        val ids = HashSet<String>()
        var total = 0
        for (f in files) {
            val songs = JSONObject(f.readText()).getJSONArray("songs")
            for (i in 0 until songs.length()) {
                val o = songs.getJSONObject(i)
                val ev = SongParser.parseSequence(o.getString("notes"))
                assertTrue("${o.getString("id")}: ${ev.exceptionOrNull()?.message}", ev.isSuccess)
                val song = Song(o.getString("id"), o.getString("title_fa"), o.optString("title_en"), "", "", o.getString("category"),
                    o.getString("difficulty"), o.getInt("tempo"), o.getString("time_signature"), ev.getOrThrow(), o.getString("license"), "")
                assertEquals(emptyList<String>(), SongParser.validate(song))
                assertTrue("duplicate id ${song.id}", ids.add(song.id))
                total++
            }
        }
        assertTrue("need at least 20 starter melodies, found $total", total >= 20)
    }

    @Test fun lessonsParse() {
        val arr = JSONObject(File(assets, "lessons/lessons.json").readText()).getJSONArray("lessons")
        assertTrue(arr.length() >= 30)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val q = o.getJSONObject("quiz")
            assertTrue(q.getInt("answer") < q.getJSONArray("options").length())
            val demo = o.optString("demo")
            if (demo.isNotBlank()) assertTrue(o.getString("id"), SongParser.parseSequence(demo).isSuccess)
            val ex = o.optJSONObject("exercise")
            if (ex != null) assertTrue(o.getString("id"), SongParser.parseSequence(ex.getString("notes")).isSuccess)
        }
    }
}
