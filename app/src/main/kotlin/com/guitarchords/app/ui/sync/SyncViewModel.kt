package com.guitarchords.app.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.R
import com.guitarchords.app.data.AppDatabase
import com.guitarchords.app.sync.ResolvedConflict
import com.guitarchords.app.sync.SyncFailure
import com.guitarchords.app.sync.SyncOutcome
import com.guitarchords.app.sync.SyncPrefs
import com.guitarchords.app.sync.SyncResult
import com.guitarchords.app.sync.SyncWorker
import com.guitarchords.app.sync.VivaceClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SyncUiState {
    object Idle : SyncUiState
    object Running : SyncUiState
    data class Success(val result: SyncResult) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

/**
 * Estado de la sincronización.
 *
 * Ya no es un panel de mando: la subida y la bajada las hace [SyncWorker] por su
 * cuenta. Aquí se ve qué está pendiente, cuándo fue la última pasada y qué
 * conflictos se resolvieron guardando una versión aparte; el botón de
 * "sincronizar ahora" es solo un atajo para no esperar.
 */
class SyncViewModel(app: Application) : AndroidViewModel(app) {

    private val application = app as GuitarChordsApp
    private val repo = application.repo
    private val prefs = SyncPrefs(app)
    private val engine = application.syncEngine
    private val chordSync = application.chordSync
    private val customChordDao = AppDatabase.get(app).customChordDao()

    val initialUrl: String get() = prefs.baseUrl

    private val _account = MutableStateFlow(prefs.userEmail)
    val account = _account.asStateFlow()

    private val _role = MutableStateFlow(prefs.userRole)
    val role = _role.asStateFlow()

    private val _state = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val state = _state.asStateFlow()

    /** Conflictos resueltos en la última pasada, para poder avisar de ellos. */
    private val _conflicts = MutableStateFlow<List<ResolvedConflict>>(emptyList())
    val conflicts = _conflicts.asStateFlow()

    private val _lastSync = MutableStateFlow(prefs.lastSync)
    val lastSync = _lastSync.asStateFlow()

    /** Sesión caducada mientras se sincronizaba en segundo plano. */
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired = _sessionExpired.asStateFlow()

    /** Todo lo que falta por subir: partituras, más los borrados definitivos. */
    val pendingUploads = combine(repo.pendingUploadCount(), repo.pendingDeleteCount()) { a, b -> a + b }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ---- Acordes personalizados (también automáticos) ----
    private val _chordSyncing = MutableStateFlow(false)
    val chordSyncing = _chordSyncing.asStateFlow()

    private val _chordsLastSync = MutableStateFlow(prefs.chordsLastSync)
    val chordsLastSync = _chordsLastSync.asStateFlow()

    private val _chordMsg = MutableStateFlow<String?>(null)
    val chordMsg = _chordMsg.asStateFlow()

    val pendingChords = customChordDao.countDirty()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // Si el trabajo en segundo plano se topó con un 401, la sesión ya está
        // cerrada: hay que decirlo aquí, porque nadie estaba mirando.
        if (prefs.userEmail.isNotBlank() && !prefs.isLoggedIn) _sessionExpired.value = true
    }

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

    /** Inicia sesión (o crea la cuenta) y deja la sincronización en marcha. */
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
                prefs.userRole = auth.user.role
                _account.value = auth.user.email
                _role.value = auth.user.role
                _sessionExpired.value = false
                _state.value = SyncUiState.Idle
                SyncWorker.schedulePeriodic(getApplication())
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
        SyncWorker.cancel(getApplication())
        prefs.clearSession()
        _account.value = ""
        _role.value = "user"
        _conflicts.value = emptyList()
        _sessionExpired.value = false
        _state.value = SyncUiState.Idle
    }

    /** Atajo manual: lo mismo que hace el trabajo en segundo plano, pero ya. */
    fun sync() {
        if (!prefs.isLoggedIn) {
            _state.value = SyncUiState.Error(
                getApplication<Application>().getString(R.string.sync_login_required)
            )
            return
        }
        _state.value = SyncUiState.Running
        viewModelScope.launch {
            when (val salida = engine.sync()) {
                is SyncOutcome.Skipped -> _state.value = SyncUiState.Idle
                is SyncOutcome.Done -> {
                    _conflicts.value = salida.result.conflicts
                    _lastSync.value = prefs.lastSync
                    _state.value = SyncUiState.Success(salida.result)
                }
                is SyncOutcome.Failed -> {
                    val f = salida.failure
                    if (f is SyncFailure.Unauthorized) {
                        _account.value = ""
                        _sessionExpired.value = true
                        _state.value = SyncUiState.Error(
                            getApplication<Application>().getString(R.string.sync_session_expired)
                        )
                    } else {
                        val msg = when (f) {
                            is SyncFailure.Network -> f.message
                            is SyncFailure.Other -> f.message
                            else -> ""
                        }
                        _state.value = SyncUiState.Error(
                            msg.ifBlank {
                                getApplication<Application>().getString(R.string.sync_error_generic)
                            }
                        )
                    }
                }
            }
        }
    }

    fun dismissConflicts() { _conflicts.value = emptyList() }
    fun dismissSessionExpired() { _sessionExpired.value = false }
}
