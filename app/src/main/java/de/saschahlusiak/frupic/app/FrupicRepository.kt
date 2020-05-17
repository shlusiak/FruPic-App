package de.saschahlusiak.frupic.app

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.saschahlusiak.frupic.db.FrupicDB
import kotlinx.coroutines.*
import org.json.JSONException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class FrupicRepository @Inject constructor(
    private val api: FreamwareApi,
    private val db: FrupicDB
) {
    private val _synchronizing = MutableLiveData(false)
    private val _lastUpdated = MutableLiveData(0L)

    // Flag whether synchronizing is currently in progress
    val synchronizing = _synchronizing as LiveData<Boolean>

    // Timestamp of last successful synchronize. May be used to update UI
    val lastUpdated = _lastUpdated as LiveData<Long>

    init {
        Log.d(tag, "Initializing ${FrupicRepository::class.simpleName}")
    }

    @Deprecated("Remove in favour of suspend function")
    fun synchronizeAsync(base: Int, limit: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                synchronize(base, limit)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Synchronize the most recent Frupics.
     *
     * Will set the [synchronizing] status while running and handle errors transparently.
     */
    @MainThread
    suspend fun synchronize(base: Int = 0, limit: Int = 100) {
        // skip if already synchronizing
        if (_synchronizing.value == true)
            return

        _synchronizing.value = true

        try {
            fetch(base, limit)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _synchronizing.value = false
        }
    }

    /**
     * Fetches the Frupics for the given range. Will not set the [synchronizing] status and does not handle
     * errors.
     *
     * Will update value of [lastUpdated], so changes can be observed on.
     *
     * @throws IOException
     * @throws JSONException
     */
    @MainThread
    suspend fun fetch(offset: Int, limit: Int) {
        Log.d(tag, "Fetching $limit Frupics")

        val start = System.currentTimeMillis()
        val result = api.getPicture(offset, limit)

        val duration = System.currentTimeMillis() - start
        Log.d(tag, "Fetched ${result.size} Frupics in $duration ms")

        measureTimeMillis {
            withContext(Dispatchers.Default) {
                db.open()
                db.addFrupics(result)
                db.close()
            }
        }.also {
            Log.d(tag, "Stored ${result.size} Frupics in db in $it ms")
        }

        _lastUpdated.value = System.currentTimeMillis()
    }

    companion object {
        private val tag = FrupicRepository::class.simpleName
    }
}