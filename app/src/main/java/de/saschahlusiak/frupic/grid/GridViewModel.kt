package de.saschahlusiak.frupic.grid

import android.database.Cursor
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.app.NotificationManager
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GridViewModel @Inject constructor(
    val repository: FrupicRepository,
    val storage: FrupicStorage,
    val notificationManager: NotificationManager
) : ViewModel() {
    private val tag = GridViewModel::class.simpleName

    val starred = MutableLiveData(false)
    val cursor = MutableLiveData<Cursor>()
    val synchronizing: LiveData<Boolean>
    val lastUpdated: LiveData<Long>


    init {
        Log.d(tag, "Initializing")

        synchronizing = repository.synchronizing
        lastUpdated = repository.lastUpdated

        viewModelScope.launch {
            repository.synchronize()
        }

        reloadData()
    }

    override fun onCleared() {
        super.onCleared()

        cursor.value?.close()
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

    fun doRefresh() {
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