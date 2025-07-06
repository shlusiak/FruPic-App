package de.saschahlusiak.frupic.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.flow.Flow

@Dao
interface FrupicDao {
    @Query("SELECT * from frupics ORDER BY _id DESC")
    suspend fun getAllFrupics(): List<Frupic>

    @Query("SELECT * from frupics ORDER BY _id DESC")
    fun getFlow(): Flow<List<Frupic>>

    @Insert
    suspend fun add(frupic: Frupic)

    @Insert
    suspend fun add(list: List<Frupic>)

    @Update
    suspend fun update(frupic: Frupic)
}