package de.saschahlusiak.frupic.gallery

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.saschahlusiak.frupic.app.FrupicDownloadManager
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    val downloadManager: FrupicDownloadManager,
    private val repository: FrupicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val tag = GalleryViewModel::class.simpleName

    private val showStarred: Boolean = savedStateHandle["starred"] ?: false

    val frupics = MutableStateFlow(emptyList<Frupic>())
    val initialPosition = savedStateHandle["position"] ?: 0

    init {
        Log.d(tag, "Initializing")
        viewModelScope.launch {
            frupics.value = if (showStarred) {
                repository.asFlow().first()
                    .filter { it.isStarred }
            } else {
                repository.asFlow().first()
            }
        }
    }

    override fun onCleared() {
        downloadManager.shutdown()
        super.onCleared()
    }

    suspend fun toggleStarred(frupic: Frupic): Boolean {
        val updated = repository.setStarred(frupic, !frupic.isStarred)
        frupics.update {
            it.toMutableList().apply {
                add(indexOf(frupic), updated)
                remove(frupic)
            }.toList()
        }
        return false
    }
}