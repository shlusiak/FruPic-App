package de.saschahlusiak.frupic.app

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import de.saschahlusiak.frupic.app.job.SynchronizeJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessLifecycleObserver @Inject constructor(
    private val context: Context
) : LifecycleObserver {
    private val tag = ProcessLifecycleObserver::class.java.simpleName
    private var isObservingLifecycle = false

    fun startObserving() {
        if (isObservingLifecycle) return
        isObservingLifecycle = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() { // App comes into foreground (will also get called on app launch)
        Log.d(tag, "App goes into foreground")
        SynchronizeJob.unschedule(context)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.d(tag, "App goes into background")

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.getBoolean("background_notifications", true)) {
            SynchronizeJob.schedule(context)
        }
    }
}