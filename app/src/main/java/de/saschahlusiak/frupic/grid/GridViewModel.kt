package de.saschahlusiak.frupic.grid

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class GridViewModel(app: Application) : AndroidViewModel(app) {
    private val tag = GridViewModel::class.simpleName

    val starred = MutableLiveData(false)
    val items = MutableLiveData<List<Frupic>>()
    val synchronizing: LiveData<Boolean>
    val lastUpdated: LiveData<Long>

    @Inject
    lateinit var repository: FrupicRepository

    @Inject
    lateinit var storage: FrupicStorage

    /**
     * Number of Frupics we load from the DB. Will slidingly increase as we scroll down.
     */
    private var limit = 1

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

    /**
     * Ensures we have enough frupics loaded from the last visible item position. Will either load from DB or from API.
     *
     * @param lastVisibleItem the position of the last visible item, to determine what needs to be loaded ahead
     */
    fun ensureLoaded(lastVisibleItem: Int) {
        val stepSize = 200

        // if starred, we don't load anything, we should have them all in memory already
        val starred = starred.value ?: false
        if (starred) return

        // the number of Frupics we have in memory

        // make sure the target is steps, so we don't reload continuously while scrolling
        val target = (lastVisibleItem / stepSize + 2) * stepSize

        // nothing to do if we have this many Frupics loaded already
        if (limit >= target) return

        // else go to database
        viewModelScope.launch {
            limit = target
            Log.d(tag, "Loading from database (limit = $limit)")
            val list = repository.getFrupics(starred, limit)
            items.value = list
            val loaded = list.size
            // if we have enough, all good
            if (loaded >= target) return@launch

            // else go to API and fetch
            val offset = loaded
            val count = target - loaded
            Log.d(tag, "Fetching from API (offset = $offset, count = $count)")

            repository.fetch(offset, count)
        }
    }

    fun reloadData() {
        Log.d(tag, "reloadData(limit = $limit)")

        viewModelScope.launch {
            val starred = starred.value ?: false
            val limit = if (starred) null else limit
            items.value = repository.getFrupics(starred, limit)
        }
    }
}