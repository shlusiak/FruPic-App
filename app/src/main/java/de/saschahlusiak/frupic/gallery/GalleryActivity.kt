package de.saschahlusiak.frupic.gallery

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import de.saschahlusiak.frupic.BuildConfig
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.utils.AppTheme
import javax.inject.Inject

@AndroidEntryPoint
class GalleryActivity : AppCompatActivity() {
    private val viewModel: GalleryViewModel by viewModels()

    @Inject
    lateinit var analytics: FirebaseAnalytics

    @Inject
    lateinit var prefs: SharedPreferences

    @Inject
    lateinit var storage: FrupicStorage

    @Inject
    lateinit var crashlytics: FirebaseCrashlytics

    public override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (true) {
            setContent {
                AppTheme {
                    GalleryScreen(
                        viewModel = viewModel,
                        onBack = { finish() },
                        onToggleFavourite = { viewModel.toggleStarred(it) },
                        onShare = ::onShare
                    )
                }
            }
        }
    }

    private fun startDownload(frupic: Frupic) {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        // we only need the permission if we are downloading the attachment, not when storing in internal storage
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                RC_WRITE_EXTERNAL_STORAGE_PERMISSION
            )
            return
        }

        val filename = frupic.filename

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
/*
        return when (item.itemId) {
            R.id.openinbrowser -> {
                analytics.logEvent("frupic_open_in_browser", null)
                intent = Intent(Intent.ACTION_VIEW, frupic.url.toUri())
                startActivity(intent)
                true
            }

            R.id.details -> {
                analytics.logEvent("frupic_details", null)
                createDetailDialog(this, storage, frupic).show()
                true
            }

            R.id.download -> {
                analytics.logEvent("frupic_download", null)
                startDownload(frupic)
                true
            }

            R.id.share_link -> {
                analytics.logEvent("frupic_share_link", null)
                intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, frupic.url)
                intent.putExtra(Intent.EXTRA_SUBJECT, "FruPic #" + frupic.id)
                startActivity(Intent.createChooser(intent, getString(R.string.share_link)))
                true
            }

            R.id.share_picture -> {
                onShare(frupic)
                true
            }

            else -> true
        }

 */
        return false
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

    companion object {
        private val tag = GalleryActivity::class.java.simpleName
        private const val RC_WRITE_EXTERNAL_STORAGE_PERMISSION = 10001
    }
}