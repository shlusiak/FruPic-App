package de.saschahlusiak.frupic.app

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.saschahlusiak.frupic.db.FrupicDB
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class FrupicRepository @Inject constructor(
    private val api: FreamwareApi,
    private val db: FrupicDB,
    private val crashlytics: FirebaseCrashlytics
) {
    private val _synchronizing = MutableLiveData(false)
    private val _lastUpdated = MutableLiveData(0L)
    private val dbLock = Mutex()

    // Flag whether synchronizing is currently in progress
    val synchronizing = _synchronizing as LiveData<Boolean>

    // Timestamp of last successful synchronize. May be used to update UI
    val lastUpdated = _lastUpdated as LiveData<Long>

    init {
        Log.d(tag, "Initializing ${FrupicRepository::class.simpleName}")
        // FIXME: there is no close. :(
        db.open()
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
        val result = try {
            api.getPicture(offset, limit)
        }
        catch (e: Exception) {
            e.printStackTrace()
            crashlytics.recordException(e)
            return
        }

        val duration = System.currentTimeMillis() - start
        Log.d(tag, "Fetched ${result.size} Frupics in $duration ms")

        measureTimeMillis {
            withContext(Dispatchers.Default) {
                withDB {
                    addFrupics(result)
                }
            }
        }.also {
            Log.d(tag, "Stored ${result.size} Frupics in db in $it ms")
        }

        _lastUpdated.value = System.currentTimeMillis()
    }

    suspend fun getFrupics(starred: Boolean = false, limit: Int? = null): List<Frupic> {
        return withContext(Dispatchers.IO) {
            val mask = if (starred) Frupic.FLAG_FAV else 0
            return@withContext withDB {
                getFrupics(null, mask, limit)
            }
        }
    }

    /**
     * Remove the [Frupic.FLAG_NEW] flag from all Frupics.
     */
    @MainThread
    suspend fun markAllAsOld() {
        withContext(Dispatchers.IO) {
            withDB {
                updateFlags(null, Frupic.FLAG_NEW, false)
            }
        }
        _lastUpdated.value = System.currentTimeMillis()
    }

    @MainThread
    suspend fun setStarred(frupic: Frupic, starred: Boolean) {
        withContext(Dispatchers.IO) {
            withDB {
                updateFlags(frupic, Frupic.FLAG_FAV, starred)
            }
        }
        _lastUpdated.value = System.currentTimeMillis()
    }

    /**
     * Runs the given block in an exclusive DB session.
     */
    private suspend fun <R> withDB(block: FrupicDB.() -> R): R {
        dbLock.withLock {
            return block(db)
        }
    }

    companion object {
        private val tag = FrupicRepository::class.simpleName
    }
}