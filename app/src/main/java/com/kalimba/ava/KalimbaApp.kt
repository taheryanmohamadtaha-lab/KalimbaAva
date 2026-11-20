package com.kalimba.ava

import android.app.Application
import com.kalimba.ava.audio.AudioEngine
import com.kalimba.ava.data.AppDatabase
import com.kalimba.ava.data.Content
import com.kalimba.ava.data.Prefs

class KalimbaApp : Application() {
    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        container = Container(this)
    }
}

/** Hand-written dependency container (keeps the build small and dependency-free). */
class Container(app: Application) {
    val prefs = Prefs(app)
    val db = AppDatabase.get(app)
    val content = Content(app)
    val audio = AudioEngine(app, prefs)
}
