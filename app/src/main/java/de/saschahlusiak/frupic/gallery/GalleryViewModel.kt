package de.saschahlusiak.frupic.gallery

import android.app.Application
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicDownloadManager
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    val downloadManager: FrupicDownloadManager,
    private val repository: FrupicRepository
) : ViewModel() {
    private val tag = GalleryViewModel::class.simpleName

    val cursor = MutableLiveData<Cursor>()
    val currentFrupic = MutableLiveData<Frupic>()
    val lastUpdated: StateFlow<Long>

    var starred: Boolean = false

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

    init {
        Log.d(tag, "Initializing")

        lastUpdated = repository.lastUpdated

        reloadData()
    }

    override fun onCleared() {
        downloadManager.shutdown()
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