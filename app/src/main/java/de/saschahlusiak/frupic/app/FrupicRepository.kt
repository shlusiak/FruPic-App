package de.saschahlusiak.frupic.app

import android.content.Context
import android.database.Cursor
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import de.saschahlusiak.frupic.db.FrupicDB
import de.saschahlusiak.frupic.db.FrupicDao
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.leolin.shortcutbadger.ShortcutBadger
import org.json.JSONException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class FrupicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: FreamwareApi,
    private val db: FrupicDB,
    private val dao: FrupicDao
) {
    private val _synchronizing = MutableStateFlow(false)
    private val _lastUpdated = MutableStateFlow(0L)
    private val dbLock = Mutex()

    // Flag whether synchronizing is currently in progress
    val synchronizing = _synchronizing.asStateFlow()

    // Timestamp of last successful synchronize. May be used to update UI
    val lastUpdated = _lastUpdated as StateFlow<Long>

    init {
        Log.d(tag, "Initializing ${FrupicRepository::class.simpleName}")
        // FIXME: there is no close. :(
        db.open()
    }

    /**
     * Synchronize the most recent Frupics.
     *
     * Will set the [synchronizing] status while running and catch errors transparently.
     *
     * @return true on success, false if failed
     */
    @MainThread
    suspend fun synchronize(base: Int = 0, limit: Int = 100): Boolean {
        // skip if already synchronizing
        if (_synchronizing.value == true)
            return true

        _synchronizing.value = true

        return try {
            fetch(base, limit)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
            withContext(Dispatchers.IO) {
                withDB {
                    addFrupics(result)
                }
                updateBadgeCount()
            }
        }.also {
            Log.d(tag, "Stored ${result.size} Frupics in db in $it ms")
        }

        _lastUpdated.value = System.currentTimeMillis()
    }

    fun asFlow() = dao.getFlow()

    suspend fun getFrupics(starred: Boolean = false): Cursor {
        return withContext(Dispatchers.IO) {
            val mask = if (starred) Frupic.FLAG_FAV else 0
            return@withContext withDB {
                // only return pics that do NOT have the FLAG_HIDDEN flag set to 1
                getFrupics(null, Frupic.FLAG_HIDDEN or mask, Frupic.FLAG_FAV)
            }
        }
    }

    /**
     * Remove the given flag (e.g. [Frupic.FLAG_NEW]) from all Frupics.
     */
    @MainThread
    suspend fun removeFlags(flag: Int) {
        withContext(Dispatchers.IO) {
            withDB {
                updateFlags(null, flag, false)
            }
            updateBadgeCount()
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

    private suspend fun updateBadgeCount() {
        val count = getFrupicCount(Frupic.FLAG_NEW)
        Log.d(tag, "Updating unread badge to $count")
        ShortcutBadger.applyCount(context, count)
    }

    suspend fun getFrupicCount(mask: Int): Int {
        return withContext(Dispatchers.IO) {
            withDB {
                getFrupics(null, mask)
            }
        }.use { it.count }
    }

    /**
     * Runs the given block in an exclusive DB session.
     */
    private suspend fun <R> withDB(block: FrupicDB.() -> R): R {
        return dbLock.withLock {
            block(db)
        }
    }

    companion object {
        private val tag = FrupicRepository::class.simpleName
    }
}