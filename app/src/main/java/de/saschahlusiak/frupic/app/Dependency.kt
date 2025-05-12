package de.saschahlusiak.frupic.app

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Reusable
    @Provides
    fun getPicasso(): Picasso = Picasso.get()

    @Reusable
    @Provides
    fun getSharedPreferences(app: Application): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)

    @Reusable
    @Provides
    fun getFirebaseAnalytics(app: Application) = FirebaseAnalytics.getInstance(app)

    @Reusable
    @Provides
    fun getFirebaseCrashlytics() = FirebaseCrashlytics.getInstance()
}
