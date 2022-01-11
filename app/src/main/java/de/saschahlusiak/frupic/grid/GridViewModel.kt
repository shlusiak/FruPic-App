package de.saschahlusiak.frupic.grid

import android.app.Application
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.NotificationManager
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class GridViewModel(app: Application) : AndroidViewModel(app) {
    private val tag = GridViewModel::class.simpleName

    val starred = MutableLiveData(false)
    val cursor = MutableLiveData<Cursor>()
    val synchronizing: LiveData<Boolean>
    val lastUpdated: LiveData<Long>

    @Inject
    lateinit var repository: FrupicRepository

    @Inject
    lateinit var storage: FrupicStorage

    @Inject
    lateinit var notificationManager: NotificationManager

    init {
        (app as App).appComponent.inject(this)

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