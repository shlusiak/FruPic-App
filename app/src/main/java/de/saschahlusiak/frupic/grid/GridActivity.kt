package de.saschahlusiak.frupic.grid

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import de.saschahlusiak.frupic.gallery.GalleryActivity
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.preferences.FrupicPreferencesActivity
import de.saschahlusiak.frupic.upload.UploadActivity
import de.saschahlusiak.frupic.utils.AppTheme

@AndroidEntryPoint
class GridActivity : AppCompatActivity() {

    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (true) {
            setContent {
                AppTheme {
                    GridScreen(
                        viewModel = hiltViewModel(),
                        onFrupicClick = ::onFrupicClick,
                        onUpload = ::onUpload,
                        onSettings = ::onSettings
                    )
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

        pickMediaLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
            ::onMediaPicked
        )
    }

    private fun onFrupicClick(position: Int, frupic: Frupic) {
        val intent = Intent(this, GalleryActivity::class.java).apply {
            putExtra("id", frupic.id)
            putExtra("position", position)
//            putExtra("starred", viewModel.starred.value)
        }
        startActivity(intent)
    }

    private fun onUpload() {
        val mediaType = ImageOnly as VisualMediaType
        val request = PickVisualMediaRequest.Builder()
            .setMediaType(mediaType)
            .build()
        pickMediaLauncher.launch(request)
    }

    private fun onMediaPicked(uri: Uri?) {
        uri ?: return

        val intent = Intent(this, UploadActivity::class.java)
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(intent)
    }

    private fun onSettings() {
        startActivity(Intent(this, FrupicPreferencesActivity::class.java))
    }
}