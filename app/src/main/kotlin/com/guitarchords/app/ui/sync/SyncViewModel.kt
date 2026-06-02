package com.guitarchords.app.ui.sync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
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

    val initialUrl: String get() = prefs.baseUrl
    val initialToken: String get() = prefs.token

    private val _state = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val state = _state.asStateFlow()

    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts = _conflicts.asStateFlow()

    private val _lastSync = MutableStateFlow(prefs.lastSync)
    val lastSync = _lastSync.asStateFlow()

    val pendingUploads = repo.pendingUploadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun sync(url: String, token: String) {
        if (url.isBlank() || token.isBlank()) {
            _state.value = SyncUiState.Error("Indica la URL del servidor y el token")
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
                prefs.lastSync = System.currentTimeMillis()
                _lastSync.value = prefs.lastSync
                _state.value = SyncUiState.Success(result)
            } catch (e: Exception) {
                _state.value = SyncUiState.Error(e.message ?: "Error de sincronización")
            }
        }
    }

    fun resolveAll(keepLocal: Boolean) {
        val list = _conflicts.value
        if (list.isEmpty()) return
        _state.value = SyncUiState.Running
        viewModelScope.launch {
            try {
                val client = R2Client(prefs.baseUrl, prefs.token)
                list.forEach { manager.resolveConflict(client, it, keepLocal) }
                _conflicts.value = emptyList()
                prefs.lastSync = System.currentTimeMillis()
                _lastSync.value = prefs.lastSync
                _state.value = SyncUiState.Idle
            } catch (e: Exception) {
                _state.value = SyncUiState.Error(e.message ?: "Error resolviendo conflictos")
            }
        }
    }

    fun dismissConflicts() { _conflicts.value = emptyList() }
}
