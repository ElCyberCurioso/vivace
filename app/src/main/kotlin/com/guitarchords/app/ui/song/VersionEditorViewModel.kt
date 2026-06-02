package com.guitarchords.app.ui.song

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.SongVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VersionEditorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    private val _version = MutableStateFlow<SongVersion?>(null)
    val version = _version.asStateFlow()

    private var loaded = false

    fun load(versionId: Long) {
        if (loaded) return
        loaded = true
        viewModelScope.launch { _version.value = repo.versionOnce(versionId) }
    }

    fun updateName(v: String) { _version.value = _version.value?.copy(name = v) }
    fun updateCapo(v: Int) { _version.value = _version.value?.copy(capo = v.coerceIn(0, 12)) }
    fun updateContent(v: String) { _version.value = _version.value?.copy(content = v) }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val v = _version.value
        if (v != null) {
            repo.updateVersion(v.copy(name = v.name.ifBlank { "Versión" }))
        }
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        _version.value?.let { repo.deleteVersion(it.id) }
        onDone()
    }
}
