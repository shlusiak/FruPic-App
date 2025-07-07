package de.saschahlusiak.frupic.upload

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.saschahlusiak.frupic.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadActivityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    prefs: SharedPreferences,
    @ApplicationContext private val context: Context,
    private val manager: UploadManager,
) : ViewModel() {

    val username = prefs.getString("username", "") ?: ""

    /**
     * The source Uris
     */
    val originals = CompletableDeferred<List<PreparedImage>>()
    val resized = CompletableDeferred<List<PreparedImage>>()

    private var hasSubmitted = false

    init {
        val sources: List<Uri>

        val stream: Any? = savedStateHandle[Intent.EXTRA_STREAM]

        @Suppress("UNCHECKED_CAST")
        sources = when (stream) {
            is Uri -> listOf(stream)
            is List<*> -> stream as List<Uri>
            else -> emptyList()
        }

        viewModelScope.launch {
            // the originals
            val images = manager.prepareForUpload(sources, viewModelScope)
            originals.complete(images)

            // create resized versions of 'em all in the background
            resized.complete(
                coroutineScope {
                    images.map {
                        async { it.resized() }
                    }.mapNotNull { it.await() }
                }
            )
        }
    }

    override fun onCleared() {
        if (!hasSubmitted) {
            MainScope().launch(Dispatchers.Default) {
                originals.await().forEach { it.delete() }
                resized.await().forEach { it.delete() }
            }
        }
        super.onCleared()
    }

    suspend fun submitToService(username: String, tags: String, sendResized: Boolean) {
        hasSubmitted = true

        val images = if (sendResized) {
            originals.await().forEach { it.delete() }
            resized.await()
        } else {
            resized.await().forEach { it.delete() }
            originals.await()
        }

        manager.submit(images, username, tags)
    }
}