package com.guitarchords.app.ui.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.BuildConfig
import com.guitarchords.app.update.UpdateInfo
import com.guitarchords.app.update.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUiState {
    object Idle : UpdateUiState
    object Checking : UpdateUiState
    object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val progress: Float) : UpdateUiState
    /** Descargado, pero falta el permiso de "instalar apps de esta fuente". */
    data class NeedsPermission(val file: File) : UpdateUiState
    /** Instalador del sistema lanzado. */
    object Launched : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state = _state.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    fun check() {
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            try {
                val info = UpdateManager.check(getApplication())
                _state.value = if (info == null) UpdateUiState.UpToDate
                               else UpdateUiState.Available(info)
            } catch (e: Exception) {
                _state.value = UpdateUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun download(info: UpdateInfo) {
        _state.value = UpdateUiState.Downloading(0f)
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val file = UpdateManager.download(ctx, info) { p ->
                    _state.value = UpdateUiState.Downloading(p)
                }
                launchInstall(file)
            } catch (e: Exception) {
                _state.value = UpdateUiState.Error(e.message ?: "Error")
            }
        }
    }

    /** Reintenta instalar (tras conceder el permiso) o lo vuelve a pedir. */
    fun installNow(file: File) = launchInstall(file)

    private fun launchInstall(file: File) {
        val ctx = getApplication<Application>()
        if (UpdateManager.canInstall(ctx)) {
            UpdateManager.install(ctx, file)
            _state.value = UpdateUiState.Launched
        } else {
            _state.value = UpdateUiState.NeedsPermission(file)
        }
    }

    fun openPermissionSettings() =
        UpdateManager.openInstallPermissionSettings(getApplication())
}
