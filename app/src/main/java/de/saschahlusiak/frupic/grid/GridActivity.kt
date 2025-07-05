package de.saschahlusiak.frupic.grid

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import de.saschahlusiak.frupic.app.FrupicRepository
import de.saschahlusiak.frupic.app.NotificationManager
import de.saschahlusiak.frupic.gallery.GalleryActivity
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.utils.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GridActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        if (true) {
            setContent {
                AppTheme {
                    GridScreen(hiltViewModel(), ::onFrupicClick)
                }
            }
        } else {
            if (savedInstanceState == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(android.R.id.content, GridFragment())
                    .commit()
            }
        }
    }

    private fun onFrupicClick(position: Int, frupic: Frupic) {
        val intent = Intent(this, GalleryActivity::class.java).apply {
            putExtra("id", frupic.id)
            putExtra("position", position)
//            putExtra("starred", viewModel.starred.value)
        }
        startActivity(intent)
    }
}