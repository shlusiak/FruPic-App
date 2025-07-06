package de.saschahlusiak.frupic.app

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import dagger.hilt.android.qualifiers.ApplicationContext
import de.saschahlusiak.frupic.db.FrupicDao
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val dao: FrupicDao
) {
    private val _synchronizing = MutableStateFlow(false)

    // Flag whether synchronizing is currently in progress
    val synchronizing = _synchronizing.asStateFlow()

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
                dao.add(result)
                updateBadgeCount()
            }
        }.also {
            Log.d(tag, "Stored ${result.size} Frupics in db in $it ms")
        }
    }

    fun asFlow() = dao.getFlow()

    suspend fun markAllAsSeen() {
        dao.markAllAsSeen()
        updateBadgeCount()
    }

    suspend fun setStarred(frupic: Frupic, starred: Boolean) {
        val updated = frupic.copy(isStarred = starred)

        dao.update(updated)
    }

    private suspend fun updateBadgeCount() {
        val count = getNewCount()
        Log.d(tag, "Updating unread badge to $count")
        ShortcutBadger.applyCount(context, count)
    }

    suspend fun getNewCount(): Int {
        return dao.getAllFrupics().count { it.isNew }
    }

    companion object {
        private val tag = FrupicRepository::class.simpleName
    }
}