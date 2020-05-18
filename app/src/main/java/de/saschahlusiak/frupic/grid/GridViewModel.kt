package de.saschahlusiak.frupic.grid

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class GridViewModel(app: Application) : AndroidViewModel(app) {
    val starred = MutableLiveData(false)
    val cursor = MutableLiveData<Cursor>()
    val synchronizing: LiveData<Boolean>
    val lastUpdated: LiveData<Long>

    @Inject
    lateinit var repository: FrupicRepository

    init {
        (app as App).appComponent.inject(this)

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

        GlobalScope.launch(Dispatchers.Main) {
            repository.markAllAsOld()
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

    fun doRefresh() {
        viewModelScope.launch {
            repository.markAllAsOld()
            repository.synchronize()
        }
    }

    fun doFetch(base: Int, count: Int) {
        viewModelScope.launch {
            repository.fetch(base, count)
        }
    }

    fun reloadData() {
        viewModelScope.launch {
            val c = repository.getFrupics(starred.value ?: false)
            cursor.value?.close()
            cursor.value = c
        }
    }
}