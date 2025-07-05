package de.saschahlusiak.frupic.grid

import android.database.Cursor
import android.net.http.HttpException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.app.NotificationManager
import de.saschahlusiak.frupic.db.FrupicPagingSource
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class GridViewModel @Inject constructor(
    val repository: FrupicRepository,
    val storage: FrupicStorage,
    val notificationManager: NotificationManager
) : ViewModel() {
    private val tag = GridViewModel::class.simpleName

    val starred = MutableLiveData(false)
    val cursor = MutableStateFlow<Cursor?>(null)
    val synchronizing: LiveData<Boolean>
    val lastUpdated: StateFlow<Long>

    val fruPics = Pager(
        config = PagingConfig(pageSize = 50),
        initialKey = 0,
        pagingSourceFactory = { FrupicPagingSource(repository) }
    ).flow.cachedIn(viewModelScope)

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