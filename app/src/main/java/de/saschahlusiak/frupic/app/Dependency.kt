package de.saschahlusiak.frupic.app

import com.squareup.picasso.Picasso
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val app: App) {
    @Provides
    fun getPicasso(): Picasso = Picasso.get()


}

@Component(modules = [AppModule::class])
@Singleton
interface AppComponent {
    fun inject(app: App)
}
