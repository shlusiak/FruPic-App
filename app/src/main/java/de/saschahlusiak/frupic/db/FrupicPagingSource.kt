package de.saschahlusiak.frupic.db

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FrupicPagingSource(
    val repository: FrupicRepository
) : PagingSource<Int, Frupic>() {
    private val tag = "FrupicPagingSource"

    override fun getRefreshKey(state: PagingState<Int, Frupic>): Int? {
        return null
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Frupic> {
        val cursor = repository.getFrupics(false)
        val position = params.key ?: 0

        val list = withContext(Dispatchers.IO) {
            Log.i(tag, "Loading ${params.loadSize} frupics, key = $position")

            cursor.moveToPosition(position)

            buildList {
                repeat(params.loadSize) {
                    if (cursor.isAfterLast) return@buildList
                    add(Frupic(cursor))
                    cursor.moveToNext()
                }
            }
        }

        if (cursor.count < position + 200) {
            Log.i(tag, "Fetching 100 frupics from $position")
            repository.fetch(position, 100)
        }

        cursor.close()

        Log.i(tag, "Loaded ${list.size} frupics")

        return LoadResult.Page(
            data = list,
            prevKey = null,
            nextKey = if (list.size < params.loadSize) null else (params.key ?: 0) + list.size
        )
    }
}