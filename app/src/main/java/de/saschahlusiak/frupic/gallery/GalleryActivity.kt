package de.saschahlusiak.frupic.gallery

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.firebase.analytics.FirebaseAnalytics
import de.saschahlusiak.frupic.BuildConfig
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.detail.DetailDialog.create
import de.saschahlusiak.frupic.model.Frupic
import kotlinx.android.synthetic.main.gallery_activity.*
import javax.inject.Inject

class GalleryActivity : AppCompatActivity(R.layout.gallery_activity), OnPageChangeListener {
    private var adapter: GalleryAdapter? = null
    private var fadeAnimation: Animation? = null
    private var menu: Menu? = null
    private val viewModel: GalleryViewModel by viewModels()

    @Inject
    lateinit var analytics: FirebaseAnalytics

    @Inject
    lateinit var prefs: SharedPreferences

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as App).appComponent.inject(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            viewModel.starred = intent.getBooleanExtra("starred", false)
            viewModel.position = intent.getIntExtra("position", 0)
        }

        fadeAnimation = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 200
            fillAfter = true

            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    url.visibility = View.VISIBLE
                }

                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    /* hide URL view, because with alpha of 0, it's still clickable */
                    url.visibility = View.INVISIBLE
                }
            })
        }
        viewPager.addOnPageChangeListener(this)


        val animateGifs = prefs.getBoolean("animatedgifs", true)
        adapter = GalleryAdapter(this, animateGifs, viewModel.storage, viewModel.downloadManager)
        viewPager.adapter = adapter

        viewModel.items.observe(this, Observer { items ->
            adapter?.setItems(items)
            viewPager.setCurrentItem(viewModel.position, false)
        })

        viewModel.currentFrupic.observe(this, Observer { frupic: Frupic -> updateLabels(frupic) })
        viewModel.lastUpdated.observe(this, Observer { updated: Long? -> viewModel.reloadData() })
    }

    /**
     * toggles visibility of the controls
     */
    fun toggleControls() {
        if (supportActionBar?.isShowing == true) {
            supportActionBar?.hide()
            all_controls.startAnimation(fadeAnimation)
        } else {
            supportActionBar?.show()
            all_controls.clearAnimation()
            url.visibility = View.VISIBLE
        }
    }

    private fun updateLabels(frupic: Frupic) {
        /* TODO: Display information about unavailable frupic */
        var t = findViewById<TextView>(R.id.url)
        t.text = frupic.url
        t = findViewById(R.id.username)
        t.text = getString(R.string.gallery_posted_by, frupic.username)
        supportActionBar?.title = String.format("#%d", frupic.id)
        menu?.findItem(R.id.star)?.apply {
            setIcon(if (frupic.hasFlag(Frupic.FLAG_FAV)) R.drawable.star_label else R.drawable.star_empty)
            isChecked = frupic.hasFlag(Frupic.FLAG_FAV)
        }
        t = findViewById(R.id.tags)
        val tags = frupic.tagsString
        if (tags.isEmpty()) {
            t.visibility = View.INVISIBLE
        } else {
            t.text = tags
            t.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.gallery_optionsmenu, menu)
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        when (requestCode) {
            RC_WRITE_EXTERNAL_STORAGE_PERMISSION -> if (grantResults.isNotEmpty() && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                Log.i(tag, "permission granted")
                val frupic = viewModel.currentFrupic.value ?: return
                startDownload(frupic)
            }
        }
    }

    private fun startDownload(frupic: Frupic) {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        // we only need the permission if we are downloading the attachment, not when storing in internal storage
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), RC_WRITE_EXTERNAL_STORAGE_PERMISSION)
            return
        }

        val filename = frupic.filename

        /* Make sure, destination directory exists */
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs()
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(frupic.fullUrl))
        req.setVisibleInDownloadsUi(true)
        req.allowScanningByMediaScanner()
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        req.setTitle(filename)
        req.setDescription("Frupic " + frupic.id)
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        dm.enqueue(req)
        Toast.makeText(this, getString(R.string.fetching_image_message, filename), Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        val frupic = viewModel.currentFrupic.value ?: return false
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.openinbrowser -> {
                analytics.logEvent("frupic_open_in_browser", null)
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(frupic.url))
                startActivity(intent)
                true
            }
            R.id.details -> {
                analytics.logEvent("frupic_details", null)
                create(this, viewModel.storage, frupic).show()
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
                val file = viewModel.storage.getFile(frupic)
                if (file.exists()) {
                    analytics.logEvent("frupic_share", null)
                    val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
                    intent = ShareCompat.IntentBuilder.from(this)
                        .setType("image/?")
                        .setStream(uri)
                        .setChooserTitle(R.string.share_picture)
                        .createChooserIntent()
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
                true
            }
            R.id.star -> {
                analytics.logEvent("frupic_star", null)
                viewModel.toggleFrupicStarred(frupic)
                updateLabels(frupic)
                true
            }
            R.id.copy_to_clipboard -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("URL", frupic.url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@GalleryActivity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                true
            }
            else -> true
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        Log.i(tag, "onPageSelected($position)")
        viewModel.position = position
        if (supportActionBar?.isShowing == false) {
            toggleControls()
        }
    }

    override fun onPageScrollStateChanged(state: Int) {}

    companion object {
        private val tag = GalleryActivity::class.java.simpleName
        private const val RC_WRITE_EXTERNAL_STORAGE_PERMISSION = 10001
    }
}