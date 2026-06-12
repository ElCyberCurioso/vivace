package com.guitarchords.app

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import com.guitarchords.app.chords.ChordDb
import com.guitarchords.app.chords.CustomChords
import com.guitarchords.app.data.AppDatabase
import com.guitarchords.app.data.Repository
import com.guitarchords.app.sync.ChordSyncManager
import com.guitarchords.app.sync.SyncPrefs
import com.guitarchords.app.training.TrainingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GuitarChordsApp : Application() {
    lateinit var repo: Repository
        private set
    lateinit var trainingRepo: TrainingRepository
        private set
    lateinit var chordSync: ChordSyncManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repo = Repository(db.playlistDao(), db.songDao(), db.songVersionDao())
        trainingRepo = TrainingRepository(db.trainingDao())
        ChordDb.init(this)
        CustomChords.init(db.customChordDao())

        // Sincronización automática de acordes personalizados: intento inicial,
        // cambios locales (debounce sobre revision) y reconexión de red.
        chordSync = ChordSyncManager(db.customChordDao(), SyncPrefs(this))
        chordSync.start(appScope)
        registerNetworkTrigger()
    }

    private fun registerNetworkTrigger() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    appScope.launch { runCatching { chordSync.sync() } }
                }
            })
        }
    }
}
