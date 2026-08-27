package com.guitarchords.app

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import com.guitarchords.app.chords.ChordDb
import com.guitarchords.app.chords.CustomChords
import com.guitarchords.app.data.AppDatabase
import com.guitarchords.app.data.Repository
import com.guitarchords.app.sync.ChordSyncManager
import com.guitarchords.app.sync.SyncEngine
import com.guitarchords.app.sync.SyncPrefs
import com.guitarchords.app.sync.SyncWorker
import com.guitarchords.app.training.TrainingRepository
import com.guitarchords.app.ui.theme.ThemeController
import com.guitarchords.app.update.UpdateController
import com.guitarchords.app.update.UpdateManager
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
    lateinit var syncEngine: SyncEngine
        private set
    lateinit var syncPrefs: SyncPrefs
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val NINETY_DAYS_MILLIS = 90L * 24 * 60 * 60 * 1000

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repo = Repository(
            db, db.playlistDao(), db.songDao(), db.songVersionDao(), db.pendingDeleteDao()
        )
        trainingRepo = TrainingRepository(db.trainingDao())
        ChordDb.init(this)
        CustomChords.init(db.customChordDao())
        ThemeController.init(this)

        // Papelera: purga las partituras que llevan más de 90 días borradas.
        // Solo se van las que el servidor ya sabe borradas (ver purgeTrashOlderThan):
        // si no, la fila desaparecía sin avisar y la partitura volvía a bajarse.
        appScope.launch { runCatching { repo.purgeExpiredTrash(NINETY_DAYS_MILLIS) } }

        // Borra cualquier APK de actualización que quedara descargado y
        // comprueba en segundo plano si hay una versión nueva (1 vez al día).
        UpdateManager.cleanup(this)
        appScope.launch { UpdateController.checkSilently(this@GuitarChordsApp) }

        // Sincronización automática de acordes personalizados: intento inicial,
        // cambios locales (debounce sobre revision) y reconexión de red.
        syncPrefs = SyncPrefs(this)
        chordSync = ChordSyncManager(db.customChordDao(), syncPrefs)
        chordSync.start(appScope)

        // Partituras, listas y versiones: el trabajo real lo hace WorkManager,
        // que sobrevive a que se cierre la app. Aquí solo se pide una pasada al
        // arrancar y se deja programada la de repaso.
        syncEngine = SyncEngine(repo, syncPrefs)
        // Cada cambio local pide una subida. El retraso agrupa las ráfagas de
        // guardado del editor en una sola pasada.
        repo.onLocalChange = {
            if (syncPrefs.isLoggedIn) SyncWorker.schedule(this, delaySeconds = 5)
        }
        if (syncPrefs.isLoggedIn) {
            SyncWorker.schedule(this, delaySeconds = 0)
            SyncWorker.schedulePeriodic(this)
        }
        registerNetworkTrigger()
    }

    /**
     * Al recuperar la red se pide una pasada inmediata. WorkManager ya reintenta
     * por su cuenta, pero esto acorta la espera al volver de un túnel o del modo
     * avión, que es justo cuando el usuario espera ver sus cambios subir.
     */
    private fun registerNetworkTrigger() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    appScope.launch { runCatching { chordSync.sync() } }
                    if (syncPrefs.isLoggedIn) SyncWorker.schedule(this@GuitarChordsApp, delaySeconds = 1)
                }
            })
        }
    }
}
