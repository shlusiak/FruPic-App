package de.saschahlusiak.frupic.db

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.saschahlusiak.frupic.model.Frupic
import javax.inject.Singleton

@Database(
    entities = [
        Frupic::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class FrupicDatabase : RoomDatabase() {
    abstract fun frupicDao(): FrupicDao
}

@Module
@InstallIn(SingletonComponent::class)
class FrupicDatabaseModule {
    @Provides
    @Singleton
    fun getDatabase(app: Application): FrupicDatabase = Room.databaseBuilder(
        app,
        FrupicDatabase::class.java, "frupic.db"
    ).apply {
        fallbackToDestructiveMigration()
    }.build()

    @Provides
    fun getFrupicDao(db: FrupicDatabase) = db.frupicDao()
}