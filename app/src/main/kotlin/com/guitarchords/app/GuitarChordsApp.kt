package com.guitarchords.app

import android.app.Application
import com.guitarchords.app.chords.ChordDb
import com.guitarchords.app.data.AppDatabase
import com.guitarchords.app.data.Repository

class GuitarChordsApp : Application() {
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repo = Repository(db.playlistDao(), db.songDao(), db.songVersionDao())
        ChordDb.init(this)
    }
}
