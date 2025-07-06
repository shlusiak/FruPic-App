package de.saschahlusiak.frupic.grid

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.app.NotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GridViewModel @Inject constructor(
    val repository: FrupicRepository,
    val storage: FrupicStorage,
    val notificationManager: NotificationManager
) : ViewModel() {
    private val tag = GridViewModel::class.simpleName

    val starred = MutableStateFlow(false)
    val synchronizing = repository.synchronizing

    val fruPics = repository.asFlow()
        .combine(starred) { frupics, starred ->
            if (starred)
                frupics.filter { it.isStarred }
            else
                frupics
        }.flowOn(Dispatchers.Default)

    init {
        Log.d(tag, "Initializing")

        viewModelScope.launch {
            repository.synchronize()
        }
    }

    override fun onCleared() {
        super.onCleared()

        GlobalScope.launch(Dispatchers.Main) {
            repository.markAllAsSeen()
            notificationManager.clearUnseenNotification()
        }
    }

    fun toggleShowStarred() {
        starred.value = (starred.value == false)
    }

    fun synchronize() {
        viewModelScope.launch {
            repository.markAllAsSeen()
            repository.synchronize()
        }
    }

    suspend fun needsMoreData(size: Int) {
        runCatching {
            repository.fetch(size, 50)
        }
    }
}