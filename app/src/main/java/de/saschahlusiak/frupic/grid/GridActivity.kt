package de.saschahlusiak.frupic.grid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.NotificationManager
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GridActivity : AppCompatActivity() {
    @Inject
    lateinit var repository: FrupicRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, GridFragment())
                .commit()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        GlobalScope.launch(Dispatchers.Main) {
            repository.removeFlags(Frupic.FLAG_NEW)
            notificationManager.clearUnseenNotification()
        }

        super.onBackPressed()
    }
}