package de.saschahlusiak.frupic.app

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.saschahlusiak.frupic.app.job.SynchronizeJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessLifecycleObserver @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    private val tag = ProcessLifecycleObserver::class.java.simpleName
    private var isObservingLifecycle = false

    fun startObserving() {
        if (isObservingLifecycle) return
        isObservingLifecycle = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d(tag, "App goes into foreground")
        SynchronizeJob.unschedule(context)
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(tag, "App goes into background")

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.getBoolean("background_notifications", true)) {
            SynchronizeJob.schedule(context)
        }
    }
}