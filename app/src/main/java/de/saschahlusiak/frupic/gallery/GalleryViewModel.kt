package de.saschahlusiak.frupic.gallery

import android.database.Cursor
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.saschahlusiak.frupic.app.FrupicDownloadManager
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    val downloadManager: FrupicDownloadManager,
    val storage: FrupicStorage,
    private val repository: FrupicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val tag = GalleryViewModel::class.simpleName

    val showStarred: Boolean = savedStateHandle["starred"] ?: false

    val frupics = repository.asFlow()
        .map {
            if (showStarred) it.filter { it.isStarred }
            else it
        }

    val position = MutableStateFlow(savedStateHandle["position"] ?: 0)
    val currentFrupic = frupics.combine(position) { frupics, position ->
        if (position >= 0 && position !in frupics.indices) this.position.value = frupics.lastIndex
        frupics.getOrNull(position)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        Log.d(tag, "Initializing")
    }

    override fun onCleared() {
        downloadManager.shutdown()
        super.onCleared()
    }

    fun toggleFrupicStarred(frupic: Frupic) {
        viewModelScope.launch {
            // will update lastUpdated
            repository.setStarred(frupic, !frupic.isStarred)
        }
    }
}