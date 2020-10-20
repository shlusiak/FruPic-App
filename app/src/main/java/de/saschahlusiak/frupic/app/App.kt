package de.saschahlusiak.frupic.app

import android.app.Application
import javax.inject.Inject

class App : Application() {
    lateinit var appComponent: AppComponent

    @Inject
    lateinit var processObserver: ProcessLifecycleObserver

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent
            .builder()
            .appModule(AppModule(this))
            .build()

        appComponent.inject(this)

        processObserver.startObserving()
    }
}
