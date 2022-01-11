package de.saschahlusiak.frupic.app

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.picasso.Picasso
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Reusable
import de.saschahlusiak.frupic.app.job.CleanupJob
import de.saschahlusiak.frupic.app.job.SynchronizeJob
import de.saschahlusiak.frupic.gallery.GalleryActivity
import de.saschahlusiak.frupic.gallery.GalleryViewModel
import de.saschahlusiak.frupic.grid.GridActivity
import de.saschahlusiak.frupic.grid.GridFragment
import de.saschahlusiak.frupic.grid.GridViewModel
import de.saschahlusiak.frupic.upload.UploadActivityViewModel
import de.saschahlusiak.frupic.upload.UploadService
import javax.inject.Singleton

@Module
class AppModule(private val app: App) {
    @Provides
    fun getContext(): Context = app

    @Reusable
    @Provides
    fun getPicasso(): Picasso = Picasso.get()

    @Reusable
    @Provides
    fun getSharedPreferences(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Reusable
    @Provides
    fun getFirebaseAnalytics(context: Context) = FirebaseAnalytics.getInstance(context)

    @Reusable
    @Provides
    fun getFirebaseCrashlytics() = FirebaseCrashlytics.getInstance()
}

@Component(modules = [AppModule::class])
@Singleton
interface AppComponent {
    fun inject(app: App)

    fun inject(activity: GridActivity)
    fun inject(activity: GalleryActivity)
    fun inject(fragment: GridFragment)
    fun inject(viewModel: GridViewModel)
    fun inject(viewModel: GalleryViewModel)
    fun inject(viewModel: UploadActivityViewModel)

    fun inject(service: UploadService)

    fun inject(job: SynchronizeJob)
    fun inject(job: CleanupJob)
}
