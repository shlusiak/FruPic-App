package de.saschahlusiak.frupic.services

import android.content.Context
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.db.FrupicDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@Deprecated("Use repo directly")
class RefreshJob internal constructor(context: Context) : Job() {
    var base = 0
    var count = 0
    var db: FrupicDB = FrupicDB(context)

    fun setRange(base: Int, count: Int) {
        this.base = base.coerceAtLeast(0)
        this.count = count
    }

    public override fun run(): JobState {
        runBlocking(Dispatchers.Main) {
            val repo = FrupicRepository(db)
            repo.synchronize(base, count)
        }
        return JobState.JOB_SUCCESS
    }
}