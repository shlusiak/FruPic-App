package de.saschahlusiak.frupic.grid

import android.database.Cursor
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.app.NotificationManager
import de.saschahlusiak.frupic.model.Frupic
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
    val cursor = MutableStateFlow<Cursor?>(null)
    val synchronizing: StateFlow<Boolean>
    val lastUpdated: StateFlow<Long>

    val fruPics = repository.asFlow()
        .combine(starred) { frupics, starred ->
            if (starred)
                frupics.filter { it.isStarred }
            else
                frupics
        }.flowOn(Dispatchers.Default)

    init {
        Log.d(tag, "Initializing")

        synchronizing = repository.synchronizing
        lastUpdated = repository.lastUpdated

        viewModelScope.launch {
            repository.synchronize()
        }
    }

    override fun onCleared() {
        super.onCleared()

        cursor.value?.close()

        GlobalScope.launch(Dispatchers.Main) {
            repository.removeFlags(Frupic.FLAG_NEW)
            notificationManager.clearUnseenNotification()
        }
    }

    fun toggleShowStarred() {
        starred.value = (starred.value == false)
        reloadData()
    }

    fun toggleFrupicStarred(frupic: Frupic) {
        viewModelScope.launch {
            // will update lastUpdated
            repository.setStarred(frupic, !frupic.isStarred)
        }
    }

    fun synchronize() {
        viewModelScope.launch {
            repository.removeFlags(Frupic.FLAG_NEW)
            repository.synchronize()
        }
    }

    fun doFetch(base: Int, count: Int) {
        viewModelScope.launch {
            repository.fetch(base, count)
        }
    }

    fun reloadData() {
        Log.d(tag, "reloadData")

        viewModelScope.launch {
            val c = repository.getFrupics(starred.value ?: false)

            cursor.value?.close()
            cursor.value = c
        }
    }
}