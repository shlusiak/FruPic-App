package de.saschahlusiak.frupic.app

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.saschahlusiak.frupic.db.FrupicDB
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.utils.toList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class FrupicRepository @Inject constructor(
    private val db: FrupicDB
) {
    private val _synchronizing = MutableLiveData<Boolean>(false)
    private val _lastUpdated = MutableLiveData(0L)

    // Flag whether synchronizing is currently in progress
    val synchronizing = _synchronizing as LiveData<Boolean>

    // Timestamp of last successful synchronize. May be used to update UI
    val lastUpdated = _lastUpdated as LiveData<Long>

    init {
        Log.d(tag, "Initializing ${FrupicRepository::class.simpleName}")
    }

    /**
     * Synchronize the most recent 1000 Frupics.
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
     * Fetches the Frupics for the given range. Will not set the [synchronising] status and does not handle
     * errors.
     *
     * Will update value of [lastUpdated], so changes can be observed on.
     *
     * @throws IOException
     * @throws JSONException
     */
    @MainThread
    suspend fun fetch(offset: Int, limit: Int) {
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            val query = "$INDEX_URL?offset=$offset&limit=$limit"
            val result = parse(fetchURL(query))

            result ?: return@withContext

            val duration = System.currentTimeMillis() - start
            Log.d(tag, "Fetched ${result.size} Frupics in $duration ms")

            measureTimeMillis {
                db.open()
                db.addFrupics(result)
                db.close()
            }.also {
                Log.d(tag, "Stored ${result.size} Frupics in db in $it ms")
            }
        }

        _lastUpdated.value = System.currentTimeMillis()
    }

    /**
     * Return the response of the given URL as String
     */
    @Throws(IOException::class)
    private fun fetchURL(url: String): String {
        return URL(url).openStream().use { stream ->
            BufferedReader(InputStreamReader(stream)).use {
                it.readLines().joinToString("\n")
            }
        }
    }

    /**
     * Parses the JSON response to a list of [Frupic]
     */
    @Throws(JSONException::class)
    private fun parse(json: String): List<Frupic>? {
        return JSONArray(json).toList<JSONObject>().map { jo ->
            Frupic(
                jo.getInt("id"),
                Frupic.FLAG_NEW or Frupic.FLAG_UNSEEN,
                jo.getString("url"),
                jo.getString("thumb_url"),
                jo.getString("date"),
                jo.getString("username"),
                jo.getJSONArray("tags").toList()
            )
        }
    }

    companion object {
        private val tag = FrupicRepository::class.simpleName
        private const val INDEX_URL = "https://api.freamware.net/2.0/get.picture"
    }
}