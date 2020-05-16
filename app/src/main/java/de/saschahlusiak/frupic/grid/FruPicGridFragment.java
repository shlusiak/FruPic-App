package de.saschahlusiak.frupic.grid;

import android.app.DownloadManager;
import android.content.*;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.detail.DetailDialog;
import de.saschahlusiak.frupic.gallery.FruPicGallery;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.services.Job;
import de.saschahlusiak.frupic.services.JobManager;
import de.saschahlusiak.frupic.services.JobManager.JobManagerBinder;
import de.saschahlusiak.frupic.services.RefreshJob;

public class FruPicGridFragment extends Fragment implements FruPicGridAdapter.OnItemClickListener, Job.OnJobListener {
	static private final String tag = FruPicGridFragment.class.getSimpleName();

	private RecyclerView grid;
	private FruPicGridAdapter adapter;
	private FrupicDB db;
	private Cursor cursor;
	private ConnectivityManager cm;

	private int category;

	private static final int FRUPICS_STEP = 100;

	private Runnable mRemoveWindow = new Runnable() {
		public void run() {
			removeWindow();
		}
	};

	private Handler mHandler = new Handler();
	private WindowManager mWindowManager;
	private TextView mDialogText;
	private boolean mShowing;
	private boolean mReady;

	private GridLayoutManager layoutManager;

	private JobManagerConnection jobManagerConnection;

	static class JobManagerConnection implements ServiceConnection {
		FruPicGridFragment activity;
		RefreshJob refreshJob;
		JobManager jobManager;

		JobManagerConnection(FruPicGridFragment activity) {
			this.activity = activity;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(tag, "onServiceConnected");
			jobManager = ((JobManagerBinder) service).getService();

			refreshJob = jobManager.getRefreshJob();
			refreshJob.addJobListener(activity);

			if (activity.adapter != null)
				activity.adapter.notifyDataSetChanged();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(tag, "onServiceDisconnected");
			refreshJob.removeJobListener(activity);
			jobManager = null;
		}

		void unbind(Context context) {
			context.getApplicationContext().unbindService(this);
			this.jobManager = null;
		}

		void requestRefresh(int base, int count) {
			if (jobManager == null)
				return;
			if (refreshJob == null)
				return;
			if (refreshJob.isScheduled() || refreshJob.isRunning())
				return;

			NetworkInfo ni = activity.cm.getActiveNetworkInfo();
			if (ni == null)
				return;
			if (!ni.isConnected())
				return;

			refreshJob.setRange(base, count);

			Intent intent = new Intent(activity.getContext(), JobManager.class);
			activity.getContext().startService(intent);

			jobManager.post(refreshJob, Job.Priority.PRIORITY_HIGH);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		category = getArguments().getInt("nav");

		db = new FrupicDB(getContext());
		db.open();

	    cm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

		mWindowManager = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);

		jobManagerConnection = new JobManagerConnection(this);
		Intent intent = new Intent(getContext(), JobManager.class);
		getContext().getApplicationContext().bindService(intent, jobManagerConnection, Context.BIND_AUTO_CREATE);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.grid_fragment, container, false);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		adapter = new FruPicGridAdapter(this);

		grid = (RecyclerView) getView().findViewById(R.id.gridView);
		int columnWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 88, getResources().getDisplayMetrics());
		layoutManager = new GridAutofitLayoutManager(getContext(), columnWidth, GridLayoutManager.VERTICAL, false);
		grid.setLayoutManager(layoutManager);
		grid.setAdapter(adapter);
		grid.addOnScrollListener(scrollListener);
		registerForContextMenu(grid);

		LayoutInflater inflate = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mDialogText = (TextView) inflate.inflate(R.layout.grid_list_position, null);
		mDialogText.setVisibility(View.INVISIBLE);

		mHandler.post(new Runnable() {
			public void run() {
				mReady = true;
				LayoutParams lp = new LayoutParams(
					LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT,
					LayoutParams.TYPE_APPLICATION,
					LayoutParams.FLAG_NOT_TOUCHABLE
						| LayoutParams.FLAG_NOT_FOCUSABLE,
					PixelFormat.TRANSLUCENT);
				mWindowManager.addView(mDialogText, lp);
			}
		});

		cursorChanged();
	}

	@Override
	public void onDestroy() {
		if (cursor != null)
			cursor.close();
		cursor = null;
		db.close();
		db = null;
		if (mDialogText != null) {
			mWindowManager.removeView(mDialogText);
		}
        mReady = false;

		jobManagerConnection.unbind(getContext());
		jobManagerConnection = null;

		super.onDestroy();
	}

	@Override
	public void onResume() {
		mReady = true;
		cursorChanged();
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		removeWindow();
		mReady = false;
	}

	@Override
	public void onItemClick(int position, long id) {
		Intent intent = new Intent(getContext(), FruPicGallery.class);
		intent.putExtra("position", position);
		intent.putExtra("id", id);
		intent.putExtra("navIndex", category);
		startActivity(intent);
	}

    private void removeWindow() {
        if (mShowing) {
            mShowing = false;
            mDialogText.setVisibility(View.INVISIBLE);
        }
    }

    private OnScrollListener scrollListener = new OnScrollListener() {
		private int lastScrollState = RecyclerView.SCROLL_STATE_IDLE;
		private String mPrevDate = "";

		@Override
		public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
			lastScrollState = newState;

			if (newState == RecyclerView.SCROLL_STATE_IDLE && (category == 0)) {
				int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
				int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

				if (lastVisibleItem > adapter.getItemCount() - FRUPICS_STEP) {
					jobManagerConnection.requestRefresh(adapter.getItemCount() - FRUPICS_STEP, FRUPICS_STEP + FRUPICS_STEP);
				}
			}
		}

		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy){
			int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
			int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

			if (category == 0 && lastVisibleItem > adapter.getItemCount() - FRUPICS_STEP && lastVisibleItem > 0) {
				jobManagerConnection.requestRefresh(adapter.getItemCount() - FRUPICS_STEP, FRUPICS_STEP + FRUPICS_STEP);
			}
			if (mReady) {
				Cursor first = adapter.getItem(firstVisibleItem);
				if (first != null) {
					String date = first.getString(FrupicDB.DATE_INDEX);
					if (date == null)
						date = "";
					else date = date.substring(0, 10);

					if (!mShowing && !date.equals(mPrevDate) && (lastScrollState != RecyclerView.SCROLL_STATE_IDLE)) {
						mShowing = true;
						mDialogText.setVisibility(View.VISIBLE);
					}
					if (mShowing) {
						mDialogText.setText(date);
						mHandler.removeCallbacks(mRemoveWindow);
						mHandler.postDelayed(mRemoveWindow, 1500);
					}
					mPrevDate = date;
				}
			}
		}
	};

	public JobManager getJobManager() {
		return jobManagerConnection.jobManager;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.grid_contextmenu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Frupic frupic = new Frupic((Cursor)adapter.getItem((int) info.position));

		menu.setHeaderTitle("#" + frupic.id);
		menu.findItem(R.id.star).setChecked(frupic.hasFlag(Frupic.FLAG_FAV));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Frupic frupic = new Frupic((Cursor)adapter.getItem((int) info.position));
		Intent intent;

		switch (item.getItemId()) {
		case R.id.star:
			db.updateFlags(frupic, Frupic.FLAG_FAV, !((frupic.flags & Frupic.FLAG_FAV) == Frupic.FLAG_FAV));
			cursorChanged();
			return true;

		case R.id.openinbrowser:
			intent = new Intent("android.intent.action.VIEW", Uri.parse(frupic.getUrl()));
			startActivity(intent);
			return true;

		case R.id.details:
			DetailDialog.create(getContext(), frupic).show();
			
			return true;

		case R.id.share_link:
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, frupic.getUrl());
			intent.putExtra(Intent.EXTRA_SUBJECT, "FruPic #" + frupic.id);
			startActivity(Intent.createChooser(intent,
					getString(R.string.share_link)));
			return true;

		case R.id.cache_now:
			/* Make sure, destination directory exists */
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();
			
			DownloadManager dm = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
			DownloadManager.Request req = new DownloadManager.Request(Uri.parse(frupic.getFullUrl()));
			req.setVisibleInDownloadsUi(true);
			
			req.allowScanningByMediaScanner();
			req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			
			req.setTitle(frupic.getFileName(false));
			req.setDescription("Frupic " + frupic.id);
			req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, frupic.getFileName(false));
			dm.enqueue(req);
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	private void cursorChanged() {
		int mask = 0;
		if (db == null)
			return;
		
		if (category == 2)
			mask |= Frupic.FLAG_FAV;

		if (category == 1)
			mask |= Frupic.FLAG_UNSEEN;

		cursor = db.getFrupics(null, mask);
		
		adapter.setCursor(cursor);
	}

	@Override
	public void OnJobStarted(Job job) {
		if (adapter != null) adapter.notifyDataSetChanged();
	}

	@Override
	public void OnJobProgress(Job job, int progress, int max) {

	}

	@Override
	public void OnJobDone(Job job) {
		if (adapter != null) cursorChanged();
	}
}