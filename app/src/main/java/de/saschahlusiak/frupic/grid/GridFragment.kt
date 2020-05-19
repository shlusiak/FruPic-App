package de.saschahlusiak.frupic.grid

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.TypedValue
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.about.AboutActivity
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.detail.DetailDialog
import de.saschahlusiak.frupic.gallery.GalleryActivity
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.preferences.FrupicPreferences
import de.saschahlusiak.frupic.upload.UploadActivity
import kotlinx.android.synthetic.main.grid_fragment.*

class GridFragment() : Fragment(), GridAdapter.OnItemClickListener, OnRefreshListener {
    private val mRemoveWindow = Runnable { removeWindow() }
    private val mHandler = Handler()
    private var mWindowManager: WindowManager? = null
    private var mDialogText: TextView? = null
    private var mShowing = false
    private var mReady = false
    private lateinit var gridLayoutManager: GridLayoutManager

    private lateinit var gridAdapter: GridAdapter
    private lateinit var viewModel: GridViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(GridViewModel::class.java)

        val app = requireContext().applicationContext as App
        app.appComponent.inject(this)

        mWindowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.grid_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout.setOnRefreshListener(this)

        val columnWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 88f, resources.displayMetrics).toInt()
        gridLayoutManager = GridAutofitLayoutManager(view.context, columnWidth, GridLayoutManager.VERTICAL, false)

        gridAdapter = GridAdapter(this@GridFragment)
        gridView.apply {
            layoutManager = gridLayoutManager
            adapter = gridAdapter
            addOnScrollListener(scrollListener)
        }

        upload.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent,
                getString(R.string.upload)), REQUEST_PICK_PICTURE)
        }

        viewModel.lastUpdated.observe(viewLifecycleOwner, Observer {
            viewModel.reloadData()
        })

        viewModel.synchronizing.observe(viewLifecycleOwner, Observer { synchronizing ->
            swipeRefreshLayout.isRefreshing = (synchronizing)
        })

        viewModel.cursor.observe(viewLifecycleOwner, Observer { cursor ->
            gridAdapter.setCursor(cursor)
        })

        viewModel.starred.observe(viewLifecycleOwner, Observer { _ ->
            activity?.invalidateOptionsMenu()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.grid_optionsmenu, menu)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mDialogText = inflater.inflate(R.layout.grid_list_position, null) as TextView
        mDialogText?.visibility = View.INVISIBLE
        mHandler.post {
            mReady = true
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION, (
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE),
                PixelFormat.TRANSLUCENT)
            mWindowManager?.addView(mDialogText, lp)
        }

        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
    }

    override fun onDestroy() {
        if (mDialogText != null) {
            mWindowManager?.removeView(mDialogText)
        }
        mReady = false
        super.onDestroy()
    }

    override fun onResume() {
        mReady = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        removeWindow()
        mReady = false
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.starred).setIcon(if (viewModel.starred.value == true) R.drawable.star_label else R.drawable.star_empty)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
            R.id.starred -> {
                viewModel.toggleShowStarred()
                return true
            }
            R.id.upload -> {
                intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.upload)), REQUEST_PICK_PICTURE)
                return true
            }
            R.id.preferences -> {
                intent = Intent(requireContext(), FrupicPreferences::class.java)
                startActivity(intent)
                return true
            }
            R.id.openinbrowser -> {
                intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("http://frupic.frubar.net")
                startActivity(intent)
                return true
            }
            R.id.about -> {
                intent = Intent(requireContext(), AboutActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_PICK_PICTURE -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val uri = data.data
                    val intent = Intent(requireContext(), UploadActivity::class.java)
                    intent.action = Intent.ACTION_SEND
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onRefresh() {
        viewModel.doRefresh()
    }

    override fun onFrupicClick(position: Int, frupic: Frupic) {
        val intent = Intent(context, GalleryActivity::class.java).apply {
            putExtra("id", frupic.id)
            putExtra("position", position)
            putExtra("navIndex", 0)
        }
        startActivity(intent)
    }

    private fun removeWindow() {
        if (mShowing) {
            mShowing = false
            mDialogText?.visibility = View.INVISIBLE
        }
    }

    private val scrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        private var lastScrollState = RecyclerView.SCROLL_STATE_IDLE
        private var mPrevDate = ""
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            lastScrollState = newState
            if (newState == RecyclerView.SCROLL_STATE_IDLE && (viewModel.starred.value != true)) {
                val firstVisibleItem = gridLayoutManager.findFirstVisibleItemPosition()
                val lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition()
                if (lastVisibleItem > gridAdapter.itemCount - FRUPICS_STEP) {
                    val base = gridAdapter.itemCount - FRUPICS_STEP
                    val count = FRUPICS_STEP + FRUPICS_STEP

                    viewModel.doFetch(base, count)
                }
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val firstVisibleItem = gridLayoutManager.findFirstVisibleItemPosition()
            val lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition()
            val isStarred = (viewModel.starred.value == true)
            if (!isStarred && (lastVisibleItem > gridAdapter.itemCount - FRUPICS_STEP) && (lastVisibleItem > 0)) {
                val base = gridAdapter.itemCount - FRUPICS_STEP
                val count = FRUPICS_STEP + FRUPICS_STEP

                viewModel.doFetch(base, count)
            }
            if (mReady) {
                gridAdapter.getItem(firstVisibleItem)?.let { first ->
                    // Cut out YYYY-MM-DD
                    val date = first.date?.substring(0, 10) ?: ""
                    if (!mShowing && date != mPrevDate && (lastScrollState != RecyclerView.SCROLL_STATE_IDLE)) {
                        mShowing = true
                        mDialogText?.visibility = View.VISIBLE
                    }
                    if (mShowing) {
                        mDialogText?.text = date
                        mHandler.removeCallbacks(mRemoveWindow)
                        mHandler.postDelayed(mRemoveWindow, 1500)
                    }
                    mPrevDate = date

                }
            }
        }
    }

    override fun onFrupicLongClick(view: View, position: Int, frupic: Frupic) {
        val inflater = activity?.menuInflater ?: return
        val popup = PopupMenu(requireContext(), view)
        inflater.inflate(R.menu.grid_contextmenu, popup.menu)
        popup.menu.findItem(R.id.star).isChecked = frupic.hasFlag(Frupic.FLAG_FAV)
        popup.setOnMenuItemClickListener { item ->
            onFrupicContextItemSelected(frupic, item)
            true
        }
        popup.show()
    }

    private fun onFrupicContextItemSelected(frupic: Frupic, item: MenuItem) {
        val intent: Intent
        when (item.itemId) {
            R.id.star -> {
                viewModel.toggleFrupicStarred(frupic)
            }
            R.id.openinbrowser -> {
                intent = Intent("android.intent.action.VIEW", Uri.parse(frupic.url))
                startActivity(intent)
            }
            R.id.details -> {
                DetailDialog.create(context, frupic).show()
            }
            R.id.share_link -> {
                intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, frupic.url)
                intent.putExtra(Intent.EXTRA_SUBJECT, "FruPic #" + frupic.id)
                startActivity(Intent.createChooser(intent, getString(R.string.share_link)))
            }

            R.id.download -> {
                /* Make sure, destination directory exists */
                // FIXME: ask for permission
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs()
                val dm = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val req = DownloadManager.Request(Uri.parse(frupic.fullUrl))
                req.setVisibleInDownloadsUi(true)
                req.allowScanningByMediaScanner()
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                req.setTitle(frupic.getFileName())
                req.setDescription("Frupic " + frupic.id)
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, frupic.getFileName())
                dm.enqueue(req)
            }
        }
    }

    companion object {
        private val tag = GridFragment::class.java.simpleName
        private val REQUEST_PICK_PICTURE = 1
        private val FRUPICS_STEP = 100
    }
}