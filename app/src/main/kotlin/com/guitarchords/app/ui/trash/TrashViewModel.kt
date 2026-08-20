package com.guitarchords.app.ui.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    val items = repo.trash().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Song>()
    )

    init {
        // Limpieza al abrir: lo que supere los 90 días se borra definitivamente.
        viewModelScope.launch { repo.purgeExpiredTrash(TRASH_MAX_AGE_MILLIS) }
    }

    fun restore(id: Long) = viewModelScope.launch { repo.restoreFromTrash(id) }
    fun deleteForever(id: Long) = viewModelScope.launch { repo.deleteForever(id) }
    fun emptyTrash() = viewModelScope.launch { repo.deleteForever(items.value.map { it.id }) }

    companion object {
        /** Las partituras viven 90 días en la papelera antes de purgarse. */
        const val TRASH_MAX_AGE_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
