package de.saschahlusiak.frupic.gallery

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicDownloadManager
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.launch
import javax.inject.Inject

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val tag = GalleryViewModel::class.simpleName

    val items = MutableLiveData<List<Frupic>>()
    val currentFrupic = MutableLiveData<Frupic>()
    val lastUpdated: LiveData<Long>

    var starred: Boolean = false

    @Inject
    lateinit var downloadManager: FrupicDownloadManager

    @Inject
    lateinit var repository: FrupicRepository

    @Inject
    lateinit var storage: FrupicStorage

    @Inject
    lateinit var crashlytics: FirebaseCrashlytics

    @Inject
    lateinit var analytics: FirebaseAnalytics

    var position: Int = -1
        set(value) {
            Log.d(tag, "Position = $value")
            field = value
            items.value?.let { items ->
                if (value in items.indices) {
                    currentFrupic.value = items[value]
                    // TODO: mark as seen
                }
            }
        }

    init {
        Log.d(tag, "Initializing")

        (app as App).appComponent.inject(this)

        lastUpdated = repository.lastUpdated

        reloadData()
    }

    override fun onCleared() {
        downloadManager.shutdown()
        super.onCleared()
    }

    fun reloadData() {
        viewModelScope.launch {
            val items = repository.getFrupics(starred)
            this@GalleryViewModel.items.value = items

            if (position in items.indices) {
                currentFrupic.value = items[position]
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