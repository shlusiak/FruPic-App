package de.saschahlusiak.frupic.grid

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.about.AboutFragment
import de.saschahlusiak.frupic.app.NotificationManager
import de.saschahlusiak.frupic.databinding.GridFragmentBinding
import de.saschahlusiak.frupic.detail.createDetailDialog
import de.saschahlusiak.frupic.gallery.GalleryActivity
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.preferences.FrupicPreferencesActivity
import de.saschahlusiak.frupic.upload.UploadActivity
import javax.inject.Inject

@AndroidEntryPoint
class GridFragment : Fragment(R.layout.grid_fragment), GridAdapter.OnItemClickListener, OnRefreshListener {
    private val mRemoveWindow = Runnable { removeWindow() }
    private val mHandler = Handler()
    private var mWindowManager: WindowManager? = null
    private var mDialogText: TextView? = null
    private var mShowing = false
    private var mReady = false
    private lateinit var gridLayoutManager: GridLayoutManager

    private lateinit var gridAdapter: GridAdapter
    private val viewModel: GridViewModel by viewModels()

    @Inject
    lateinit var analytics: FirebaseAnalytics

    @Inject
    lateinit var notificationManager: NotificationManager

    lateinit var binding: GridFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mWindowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = GridFragmentBinding.bind(view)

        binding.swipeRefreshLayout.setOnRefreshListener(this)

        mDialogText = layoutInflater.inflate(R.layout.grid_list_position, null) as TextView
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.appbar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.uploadButtonContainer) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = bars.bottom, right = bars.right)
            insets
        }

        val columnWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 88f, resources.displayMetrics).toInt()
        gridLayoutManager = GridAutofitLayoutManager(view.context, columnWidth, GridLayoutManager.VERTICAL, false)

        gridAdapter = GridAdapter(this@GridFragment)
        binding.gridView.apply {
            layoutManager = gridLayoutManager
            adapter = gridAdapter
            addOnScrollListener(scrollListener)
        }

        binding.upload.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, getString(R.string.upload)), REQUEST_PICK_PICTURE)
        }

        viewModel.lastUpdated.observe(viewLifecycleOwner, Observer {
            viewModel.reloadData()
        })

        viewModel.synchronizing.observe(viewLifecycleOwner, Observer { synchronizing ->
            binding.swipeRefreshLayout.isRefreshing = (synchronizing)
        })

        viewModel.cursor.observe(viewLifecycleOwner, Observer { cursor ->
            gridAdapter.setCursor(cursor)
        })

        viewModel.starred.observe(viewLifecycleOwner, Observer { _ ->
            activity?.invalidateOptionsMenu()
        })
    }

    override fun onDestroyView() {
        if (mDialogText != null) {
            mWindowManager?.removeView(mDialogText)
            mDialogText = null
        }
        mReady = false

        super.onDestroyView()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)
    }

    override fun onDestroy() {
        mReady = false
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        mReady = true

        notificationManager.clearUnseenNotification()
    }

    override fun onPause() {
        removeWindow()
        mReady = false
        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.grid_optionsmenu, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.starred).setIcon(if (viewModel.starred.value == true) R.drawable.star_label else R.drawable.star_empty)
    }

    @Deprecated("Deprecated in Java")
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
                intent = Intent(requireContext(), FrupicPreferencesActivity::class.java)
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
                AboutFragment().show(parentFragmentManager, null)
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
            putExtra("starred", viewModel.starred.value)
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
            if (firstVisibleItem < 0) return
            val isStarred = (viewModel.starred.value == true)
            if (!isStarred && (lastVisibleItem > gridAdapter.itemCount - FRUPICS_STEP) && (lastVisibleItem > 0)) {
                val base = gridAdapter.itemCount - FRUPICS_STEP
                val count = FRUPICS_STEP + FRUPICS_STEP

                viewModel.doFetch(base, count)
            }
            if (mReady) {
                gridAdapter.getItem(firstVisibleItem)?.let { first ->
                    // Cut out YYYY-MM-DD
                    val date = first.date.substring(0, 10)
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
                analytics.logEvent("frupic_star", null)
                viewModel.toggleFrupicStarred(frupic)
            }
            R.id.openinbrowser -> {
                analytics.logEvent("frupic_open_in_browser", null)
                intent = Intent("android.intent.action.VIEW", Uri.parse(frupic.url))
                startActivity(intent)
            }
            R.id.details -> {
                analytics.logEvent("frupic_details", null)
                createDetailDialog(requireContext(), viewModel.storage, frupic).show()
            }
            R.id.share_link -> {
                analytics.logEvent("frupic_share_link", null)
                intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, frupic.url)
                intent.putExtra(Intent.EXTRA_SUBJECT, "FruPic #" + frupic.id)
                startActivity(Intent.createChooser(intent, getString(R.string.share_link)))
            }
        }
    }

    companion object {
        private val tag = GridFragment::class.java.simpleName
        private val REQUEST_PICK_PICTURE = 1
        private val FRUPICS_STEP = 100
    }
}