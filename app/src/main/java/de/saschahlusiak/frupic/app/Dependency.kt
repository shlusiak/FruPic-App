package de.saschahlusiak.frupic.app

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.squareup.picasso.Picasso
import dagger.Component
import dagger.Module
import dagger.Provides
import de.saschahlusiak.frupic.grid.FruPicGridActivity
import javax.inject.Singleton

@Module
class AppModule(private val app: App) {
    @Provides
    fun getContext(): Context = app

    @Provides
    fun getPicasso(): Picasso = Picasso.get()

    @Provides
    fun getSharedPreferences(app: App): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)
}

@Component(modules = [AppModule::class])
@Singleton
interface AppComponent {
    fun inject(app: App)
    fun inject(activity: FruPicGridActivity)
}
