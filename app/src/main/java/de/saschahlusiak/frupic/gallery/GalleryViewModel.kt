package de.saschahlusiak.frupic.gallery

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.saschahlusiak.frupic.app.FrupicDownloadManager
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    val downloadManager: FrupicDownloadManager,
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

    val initialPosition = savedStateHandle["position"] ?: 0

    init {
        Log.d(tag, "Initializing")
    }

    override fun onCleared() {
        downloadManager.shutdown()
        super.onCleared()
    }

    fun toggleStarred(frupic: Frupic) {
        viewModelScope.launch {
            repository.setStarred(frupic, !frupic.isStarred)
        }
    }
}