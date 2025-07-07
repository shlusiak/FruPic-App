package de.saschahlusiak.frupic.grid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

        pickMediaLauncher = registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(),
            ::onMediaPicked
        )

        checkAndRequestNotificationPermission()
    }
    
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
    }

    private fun onFrupicClick(position: Int, frupic: Frupic, showStarred: Boolean) {
        val intent = Intent(this, GalleryActivity::class.java).apply {
            putExtra("id", frupic.id)
            putExtra("position", position)
            putExtra("starred", showStarred)
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

    private fun onMediaPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val intent = Intent(this, UploadActivity::class.java)
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        startActivity(intent)
    }

    private fun onSettings() {
        startActivity(Intent(this, FrupicPreferencesActivity::class.java))
    }
}