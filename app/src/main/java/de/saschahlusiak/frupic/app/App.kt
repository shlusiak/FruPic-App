package de.saschahlusiak.frupic.app

import android.app.Application

class App : Application() {
    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent.builder().build()
        appComponent.inject(this)
    }
}