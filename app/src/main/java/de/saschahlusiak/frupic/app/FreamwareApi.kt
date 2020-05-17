package de.saschahlusiak.frupic.app

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

/**
 * Wrapper for API calls to https://api.freamware.net
 */
class FreamwareApi @Inject constructor() {
    /**
     * Fetches the list of available Frupics for the given window.
     *
     * @return list of [Frupic]
     */
    @Throws(JSONException::class)
    suspend fun getPicture(offset: Int, limit: Int): List<Frupic> {
        val query = "${INDEX_URL}?offset=$offset&limit=$limit"
        val json = get(query)
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

    /**
     * Return the response of the given URL as String.
     *
     * Runs on the IO dispatcher.
     */
    @Throws(IOException::class)
    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        return@withContext URL(url).openStream().use { stream ->
            BufferedReader(InputStreamReader(stream)).use {
                it.readLines().joinToString("\n")
            }
        }
    }

    companion object {
        private const val INDEX_URL = "https://api.freamware.net/2.0/get.picture"
    }
}