package de.saschahlusiak.frupic.gallery

import android.app.DownloadManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import de.saschahlusiak.frupic.BuildConfig
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.detail.createDetailDialog
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.utils.AppTheme
import javax.inject.Inject

@AndroidEntryPoint
class GalleryActivity : AppCompatActivity() {
    private val viewModel: GalleryViewModel by viewModels()

    @Inject
    lateinit var analytics: FirebaseAnalytics

    @Inject
    lateinit var storage: FrupicStorage

    @Inject
    lateinit var crashlytics: FirebaseCrashlytics

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme {
                GalleryScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onToggleFavourite = { viewModel.toggleStarred(it) },
                    onShare = ::onShare,
                    onDownload = ::startDownload,
                    onDetails = ::onDetails,
                    onOpenInBrowser = ::onOpenInBrowser
                ) { forceDark ->
                    if (forceDark)
                        enableEdgeToEdge(
                            SystemBarStyle.dark(Color.TRANSPARENT),
                            SystemBarStyle.dark(Color.TRANSPARENT)
                        )
                    else
                        enableEdgeToEdge()

                    window?.isNavigationBarContrastEnforced = false
                }
            }
        }
    }

    private fun startDownload(frupic: Frupic) {
        val filename = frupic.filename

        analytics.logEvent("frupic_download", null)

        /* Make sure, destination directory exists */
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs()
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(frupic.fullUrl.toUri())
        req.setVisibleInDownloadsUi(true)
        req.allowScanningByMediaScanner()
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        req.setTitle(filename)
        req.setDescription("Frupic " + frupic.id)
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        dm.enqueue(req)
        Toast.makeText(
            this,
            getString(R.string.fetching_image_message, filename),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onDetails(frupic: Frupic) {
        analytics.logEvent("frupic_details", null)
        createDetailDialog(this, storage, frupic).show()
    }

    private fun onOpenInBrowser(frupic: Frupic) {
        analytics.logEvent("frupic_open_in_browser", null)
        intent = Intent(Intent.ACTION_VIEW, frupic.url.toUri())
        startActivity(intent)
    }

    private fun onShare(frupic: Frupic) {
        val file = storage.getFile(frupic)
        if (file.exists()) {
            analytics.logEvent("frupic_share", null)
            val uri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".provider",
                file
            )
            intent = ShareCompat.IntentBuilder.from(this)
                .setType("image/?")
                .setStream(uri)
                .setChooserTitle(R.string.share_picture)
                .createChooserIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }
    }
}