package de.saschahlusiak.frupic.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var processObserver: ProcessLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        processObserver.startObserving()
    }
}
