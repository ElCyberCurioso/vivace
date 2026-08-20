package com.guitarchords.app.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.R
import com.guitarchords.app.data.AppDatabase
import com.guitarchords.app.sync.AccountSyncManager
import com.guitarchords.app.sync.PendingUpload
import com.guitarchords.app.sync.UnauthorizedException
import com.guitarchords.app.sync.VivaceClient
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
    private val manager = AccountSyncManager(repo)
    private val chordSync = (app as GuitarChordsApp).chordSync
    private val customChordDao = AppDatabase.get(app).customChordDao()

    val initialUrl: String get() = prefs.baseUrl

    /** Sesión activa (email) o cadena vacía si no se ha iniciado. */
    private val _account = MutableStateFlow(prefs.userEmail)
    val account = _account.asStateFlow()

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
    fun syncChords() {
        if (!prefs.isLoggedIn) {
            _chordMsg.value = getApplication<Application>().getString(R.string.sync_login_required)
            return
        }
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

    /** Cliente con la sesión guardada. */
    private fun client() = VivaceClient(prefs.baseUrl, prefs.authToken)

    /** Inicia sesión (o crea la cuenta) y deja lista la sincronización. */
    fun signIn(url: String, email: String, password: String, name: String, register: Boolean) {
        if (url.isBlank() || email.isBlank() || password.isBlank()) {
            _state.value = SyncUiState.Error(
                getApplication<Application>().getString(R.string.sync_missing_fields)
            )
            return
        }
        prefs.baseUrl = url.trim()
        _state.value = SyncUiState.Running
        viewModelScope.launch {
            try {
                val anon = VivaceClient(prefs.baseUrl)
                val auth = if (register) anon.register(email.trim(), password, name.trim())
                           else anon.login(email.trim(), password)
                prefs.authToken = auth.token
                prefs.userEmail = auth.user.email
                prefs.userName = auth.user.name
                _account.value = auth.user.email
                _state.value = SyncUiState.Idle
                sync()
            } catch (e: Exception) {
                _state.value = SyncUiState.Error(
                    e.message ?: getApplication<Application>().getString(R.string.sync_error_generic)
                )
            }
        }
    }

    /** Cierra la sesión; las partituras siguen en el dispositivo. */
    fun signOut() {
        prefs.clearSession()
        _account.value = ""
        _conflicts.value = emptyList()
        _pendingPush.value = emptyList()
        _state.value = SyncUiState.Idle
    }

    fun sync() {
        if (!prefs.isLoggedIn) {
            _state.value = SyncUiState.Error(
                getApplication<Application>().getString(R.string.sync_login_required)
            )
            return
        }
        _state.value = SyncUiState.Running
        viewModelScope.launch {
            try {
                val result = manager.sync(client())
                _conflicts.value = result.conflicts
                _pendingPush.value = result.pendingUploads
                prefs.lastSync = System.currentTimeMillis()
                _lastSync.value = prefs.lastSync
                _state.value = SyncUiState.Success(result)
            } catch (e: UnauthorizedException) {
                signOut()
                _state.value = SyncUiState.Error(
                    getApplication<Application>().getString(R.string.sync_session_expired)
                )
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
                val uploaded = manager.push(client(), ids)
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
                manager.resolveConflict(client(), conflict, keepLocal)
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
