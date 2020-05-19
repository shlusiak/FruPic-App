package de.saschahlusiak.frupic.gallery

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicManager
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.services.JobManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class GalleryViewModel(app: Application): AndroidViewModel(app) {
    private val tag = GalleryViewModel::class.simpleName

    @Deprecated("Remove")
    val jobManager = MutableLiveData<JobManager?>()
    val cursor = MutableLiveData<Cursor>()
    val currentFrupic = MutableLiveData<Frupic>()
    val lastUpdated: LiveData<Long>

    var starred: Boolean = false

    @Inject
    lateinit var repository: FrupicRepository

    @Inject
    lateinit var manager: FrupicManager

    var position: Int = -1
        set(value) {
            Log.d(tag, "Position = " + value)
            field = value
            cursor.value?.let { cursor ->
                if (cursor.moveToPosition(value)) {
                    val frupic = Frupic(cursor)
                    currentFrupic.value = frupic
                    // TODO: mark as seen
                }
            }
        }

    @Deprecated("Replace with not service")
    private val serviceConnection = object:ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(tag, "onServiceDisconnected")
            jobManager.value = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(tag, "onServiceConnected")
            jobManager.value  = (service as JobManager.JobManagerBinder).service;
        }
    }

    init {
        Log.d(tag, "Initializing")

        (app as App).appComponent.inject(this)

        lastUpdated = repository.lastUpdated

        val intent = Intent(app, JobManager::class.java)
        app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        reloadData()
    }

    override fun onCleared() {
        getApplication<App>().unbindService(serviceConnection)
        cursor.value?.close()
        super.onCleared()
    }

    fun reloadData() {
        viewModelScope.launch {
            val c = repository.getFrupics(starred)
            cursor.value?.close()
            cursor.value = c

            if (c.moveToPosition(position)) {
                val frupic = Frupic(c)
                currentFrupic.value = frupic
                // TODO: mark as seen
            }
        }
    }

    fun toggleFrupicStarred(frupic: Frupic) {
        viewModelScope.launch {
            // will update lastUpdated
            repository.setStarred(frupic, !frupic.isStarred)
        }
    }
}