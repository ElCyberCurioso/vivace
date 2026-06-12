package com.guitarchords.app.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.R
import com.guitarchords.app.data.AppDatabase
import com.guitarchords.app.sync.PendingUpload
import com.guitarchords.app.sync.R2Client
import com.guitarchords.app.sync.SyncConflict
import com.guitarchords.app.sync.SyncManager
import com.guitarchords.app.sync.SyncPrefs
import com.guitarchords.app.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SyncUiState {
    object Idle : SyncUiState
    object Running : SyncUiState
    data class Success(val result: SyncResult) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

class SyncViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as GuitarChordsApp).repo
    private val prefs = SyncPrefs(app)
    private val manager = SyncManager(repo)
    private val chordSync = (app as GuitarChordsApp).chordSync
    private val customChordDao = AppDatabase.get(app).customChordDao()

    val initialUrl: String get() = prefs.baseUrl
    val initialToken: String get() = prefs.token

    private val _state = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val state = _state.asStateFlow()

    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts = _conflicts.asStateFlow()

    /** Cambios locales detectados en el último sync, a la espera de confirmar la subida. */
    private val _pendingPush = MutableStateFlow<List<PendingUpload>>(emptyList())
    val pendingPush = _pendingPush.asStateFlow()

    private val _lastSync = MutableStateFlow(prefs.lastSync)
    val lastSync = _lastSync.asStateFlow()

    val pendingUploads = repo.pendingUploadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ---- Acordes personalizados (sincronización automática; aquí, manual) ----
    private val _chordSyncing = MutableStateFlow(false)
    val chordSyncing = _chordSyncing.asStateFlow()

    private val _chordsLastSync = MutableStateFlow(prefs.chordsLastSync)
    val chordsLastSync = _chordsLastSync.asStateFlow()

    private val _chordMsg = MutableStateFlow<String?>(null)
    val chordMsg = _chordMsg.asStateFlow()

    val pendingChords = customChordDao.countDirty()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Sincroniza ahora los acordes (reutiliza la URL y token del formulario). */
    fun syncChords(url: String, token: String) {
        if (url.isBlank() || token.isBlank()) {
            _chordMsg.value = getApplication<Application>().getString(R.string.sync_missing_fields)
            return
        }
        prefs.baseUrl = url.trim()
        prefs.token = token.trim()
        _chordSyncing.value = true
        _chordMsg.value = null
        viewModelScope.launch {
            try {
                chordSync.sync()
                _chordsLastSync.value = prefs.chordsLastSync
                _chordMsg.value = getApplication<Application>().getString(R.string.chords_sync_done)
            } catch (e: Exception) {
                _chordMsg.value = e.message
                    ?: getApplication<Application>().getString(R.string.sync_error_generic)
            } finally {
                _chordSyncing.value = false
            }
        }
    }

    fun sync(url: String, token: String) {
        if (url.isBlank() || token.isBlank()) {
            _state.value = SyncUiState.Error(
                getApplication<Application>().getString(R.string.sync_missing_fields)
            )
            return
        }
        prefs.baseUrl = url.trim()
        prefs.token = token.trim()
        _state.value = SyncUiState.Running
        viewModelScope.launch {
            try {
                val client = R2Client(prefs.baseUrl, prefs.token)
                val result = manager.sync(client)
                _conflicts.value = result.conflicts
                _pendingPush.value = result.pendingUploads
                prefs.lastSync = System.currentTimeMillis()
                _lastSync.value = prefs.lastSync
                _state.value = SyncUiState.Success(result)
            } catch (e: Exception) {
                _state.value = SyncUiState.Error(
                    e.message ?: getApplication<Application>().getString(R.string.sync_error_generic)
                )
            }
        }
    }

    /** Sube los cambios locales confirmados por el usuario. */
    fun confirmPush() {
        val ids = _pendingPush.value.map { it.songId }
        if (ids.isEmpty()) return
        val downloaded = (_state.value as? SyncUiState.Success)?.result?.downloaded ?: 0
        _state.value = SyncUiState.Running
        viewModelScope.launch {
            try {
                val client = R2Client(prefs.baseUrl, prefs.token)
                val uploaded = manager.push(client, ids)
                _pendingPush.value = emptyList()
                prefs.lastSync = System.currentTimeMillis()
                _lastSync.value = prefs.lastSync
                _state.value = SyncUiState.Success(
                    SyncResult(downloaded, uploaded, emptyList(), _conflicts.value)
                )
            } catch (e: Exception) {
                _state.value = SyncUiState.Error(
                    e.message ?: getApplication<Application>().getString(R.string.sync_error_generic)
                )
            }
        }
    }

    /** Pospone la subida: las canciones siguen marcadas como pendientes. */
    fun cancelPush() { _pendingPush.value = emptyList() }

    /**
     * Resuelve UN conflicto: subir la versión local o quedarse con la del
     * servidor. Los no resueltos quedan en espera (la canción sigue dirty y
     * reaparecerán en el siguiente sync).
     */
    fun resolveOne(conflict: SyncConflict, keepLocal: Boolean) {
        viewModelScope.launch {
            try {
                val client = R2Client(prefs.baseUrl, prefs.token)
                manager.resolveConflict(client, conflict, keepLocal)
                _conflicts.value = _conflicts.value - conflict
                if (_conflicts.value.isEmpty()) {
                    prefs.lastSync = System.currentTimeMillis()
                    _lastSync.value = prefs.lastSync
                }
            } catch (e: Exception) {
                _state.value = SyncUiState.Error(
                    e.message ?: getApplication<Application>().getString(R.string.sync_conflict_error)
                )
            }
        }
    }

    fun dismissConflicts() { _conflicts.value = emptyList() }
}
