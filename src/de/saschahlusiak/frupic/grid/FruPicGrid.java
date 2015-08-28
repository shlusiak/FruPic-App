package de.saschahlusiak.frupic.grid;

import me.leolin.shortcutbadger.ShortcutBadger;
import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.about.AboutActivity;
import de.saschahlusiak.frupic.cache.FileCacheUtils;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.detail.DetailDialog;
import de.saschahlusiak.frupic.gallery.FruPicGallery;
import de.saschahlusiak.frupic.model.*;
import de.saschahlusiak.frupic.preferences.FrupicPreferences;
import de.saschahlusiak.frupic.services.AutoRefreshManager;
import de.saschahlusiak.frupic.services.Job;
import de.saschahlusiak.frupic.services.PurgeCacheJob;
import de.saschahlusiak.frupic.services.Job.OnJobListener;
import de.saschahlusiak.frupic.services.Job.Priority;
import de.saschahlusiak.frupic.services.JobManager;
import de.saschahlusiak.frupic.services.JobManager.JobManagerBinder;
import de.saschahlusiak.frupic.services.RefreshJob;
import de.saschahlusiak.frupic.upload.UploadActivity;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class FruPicGrid extends Activity implements OnItemClickListener, OnScrollListener, OnJobListener {
	static private final String tag = FruPicGrid.class.getSimpleName();
	static private final int REQUEST_PICK_PICTURE = 1;

	GridView grid;
	FruPicGridAdapter adapter;
	FrupicFactory factory;
	Menu optionsMenu;
	View mRefreshIndeterminateProgressView;
	FrupicDB db;
	Cursor cursor;
	PurgeCacheJob purgeCacheJob;
	ConnectivityManager cm;
	
    DrawerLayout mDrawerLayout;
    ListView mDrawerList;
    NavigationListAdapter navigationAdapter;
    View mDrawer;
    ActionBarDrawerToggle mDrawerToggle;
    int currentCategory;
    
    JobManagerConnection jobManagerConnection;

	public static final int FRUPICS_STEP = 100;
	
    private Runnable mRemoveWindow = new Runnable() {
        public void run() {
            removeWindow();
        }
    };
    
    Handler mHandler = new Handler();
    private WindowManager mWindowManager;
    private TextView mDialogText;
    private boolean mShowing;
    private boolean mReady;
    
    SharedPreferences prefs;
    
    static class JobManagerConnection implements ServiceConnection {
    	FruPicGrid activity;
    	
    	JobManager jobManager;
    	RefreshJob refreshJob;
    	
    	JobManagerConnection(FruPicGrid activity) {
    		this.activity = activity;
    	}
    	
    	@Override
    	public void onServiceConnected(ComponentName name, IBinder service) {
    		Log.d(tag, "onServiceConnected");
    		jobManager = ((JobManagerBinder)service).getService();
    		refreshJob = jobManager.getRefreshJob();
    		refreshJob.addJobListener(activity);
    		if (refreshJob.isRunning())
    			activity.OnJobStarted(refreshJob);
    		else
    			requestRefresh(0, FRUPICS_STEP);
    		activity.adapter.notifyDataSetChanged();
    		activity.invalidateOptionsMenu();
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
    		
    		Intent intent = new Intent(activity, JobManager.class);
    		activity.startService(intent);
    		
    		jobManager.post(refreshJob, Priority.PRIORITY_HIGH);
    	}

    	@Override
    	public void onServiceDisconnected(ComponentName name) {
    		Log.d(tag, "onServiceDisconnected");
    		refreshJob.removeJobListener(activity);
    		refreshJob = null;
    		jobManager = null;
    	}
    	
    	void unbind(Context context) {
    		context.getApplicationContext().unbindService(this);
    		this.refreshJob = null;
    		this.jobManager = null;
    	}
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.grid_activity);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.navigation_list);
        mDrawer = findViewById(R.id.left_drawer);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        navigationAdapter = new NavigationListAdapter(this);
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDrawerList.setAdapter(navigationAdapter);
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				navigationItemSelected(position, id);
			}
		});

        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        		);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

		factory = new FrupicFactory(this);

		db = new FrupicDB(this);
		db.open();
		
		jobManagerConnection = (JobManagerConnection) getLastNonConfigurationInstance();
		if (jobManagerConnection != null) { 
			jobManagerConnection.activity = this;
			if (jobManagerConnection.refreshJob != null)
				jobManagerConnection.refreshJob.addJobListener(this);
		} else {
			jobManagerConnection = new JobManagerConnection(this);
			Intent intent = new Intent(this, JobManager.class);
			getApplicationContext().bindService(intent, jobManagerConnection, Context.BIND_AUTO_CREATE);
		}
	    cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE); 
		
		adapter = new FruPicGridAdapter(this, factory, 300);
		grid = (GridView) findViewById(R.id.gridView);
		grid.setAdapter(adapter);
		grid.setOnItemClickListener(this);
		grid.setOnScrollListener(this);
		registerForContextMenu(grid);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (savedInstanceState != null) {
			currentCategory = savedInstanceState.getInt("navItem", 0);
		} else {
			currentCategory = 0;
		}
		purgeCacheJob = new PurgeCacheJob(new FileCacheUtils(this));
	    
		navigationItemSelected(currentCategory, 0);
		
        mWindowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        
        LayoutInflater inflate = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mDialogText = (TextView) inflate.inflate(R.layout.grid_list_position, null);
        mDialogText.setVisibility(View.INVISIBLE);
        
		mHandler.post(new Runnable() {
			public void run() {
				mReady = true;
				WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
						LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT,
						WindowManager.LayoutParams.TYPE_APPLICATION,
						WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
								| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
						PixelFormat.TRANSLUCENT);
				mWindowManager.addView(mDialogText, lp);
			}
		});
		
		Intent intent = new Intent(this, AutoRefreshManager.class);
		stopService(intent);
				
		cursorChanged();
		
		ShortcutBadger.with(this).count(0);
	}

	@Override
	protected void onDestroy() {
		if (jobManagerConnection != null) {
			if (jobManagerConnection.refreshJob != null) {
				jobManagerConnection.refreshJob.removeJobListener(this);
			}
			
			jobManagerConnection.unbind(this);
			jobManagerConnection = null;
			
			int interval = Integer.parseInt(prefs.getString("autorefresh", "0"));
			if (prefs.getBoolean("autorefresh_enabled", true)) {
				Intent intent = new Intent(this, AutoRefreshManager.class);
				intent.putExtra("interval", interval);
				startService(intent);
			}
		}
		
		
		if (cursor != null)
			cursor.close();
		cursor = null;
		db.updateFlags(null, Frupic.FLAG_NEW, false);
		db.close();
		db = null;
        mWindowManager.removeView(mDialogText);
        mReady = false;		
		super.onDestroy();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (jobManagerConnection.jobManager == null)
			return null;
		
		Object retain = jobManagerConnection;
		if (jobManagerConnection.refreshJob != null) {
			jobManagerConnection.refreshJob.removeJobListener(this);
			jobManagerConnection.activity = null;
		}
		
		jobManagerConnection = null;
		return retain;
	}
	
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("navItem", currentCategory);
	};
	
	@Override
	protected void onStart() {
		/* recreate the factory fileCache object to reread changed 
		 * preferences
		 */
		purgeCacheJob = new PurgeCacheJob(new FileCacheUtils(this));
		factory.recreateFileCache();

		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (jobManagerConnection != null)
			jobManagerConnection.jobManager.post(purgeCacheJob, Priority.PRIORITY_LOW);

		super.onStop();
	}
	
	@Override
	protected void onResume() {
		mReady = true;
		cursorChanged();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		removeWindow();
		mReady = false;
	}
	
	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		Intent intent = new Intent(this, FruPicGallery.class);
		intent.putExtra("position", position);
		intent.putExtra("id", id);
		intent.putExtra("navIndex", currentCategory);
		startActivity(intent);
	}

    private void removeWindow() {
        if (mShowing) {
            mShowing = false;
            mDialogText.setVisibility(View.INVISIBLE);
        }
    }
    
    String mPrevDate = "";

    int lastScrollState = SCROLL_STATE_IDLE;
    
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (currentCategory == 0 && firstVisibleItem + visibleItemCount > adapter.getCount() - FRUPICS_STEP && visibleItemCount > 0) {
			jobManagerConnection.requestRefresh(adapter.getCount() - FRUPICS_STEP, FRUPICS_STEP + FRUPICS_STEP);
		}
        if (mReady) {
        	Cursor first = (Cursor)adapter.getItem(firstVisibleItem);
        	if (first != null) {
	            String date = first.getString(FrupicDB.DATE_INDEX);
	            if (date == null)
	            	date = "";
	            else date = date.substring(0, 10);
	            
	            if (!mShowing && !date.equals(mPrevDate) && lastScrollState == SCROLL_STATE_FLING) {
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

	@Override
	public void onScrollStateChanged(AbsListView view, int state) {
		lastScrollState = state;
		if (state == SCROLL_STATE_IDLE && (currentCategory == 0)) {
			int firstVisibleItem = grid.getFirstVisiblePosition();
			int visibleItemCount = grid.getLastVisiblePosition() - firstVisibleItem + 1;
			if (firstVisibleItem + visibleItemCount > adapter.getCount() - FRUPICS_STEP) {
				jobManagerConnection.requestRefresh(adapter.getCount() - FRUPICS_STEP, FRUPICS_STEP + FRUPICS_STEP);
			}
			
			if (jobManagerConnection.jobManager != null)
				jobManagerConnection.jobManager.post(purgeCacheJob, Priority.PRIORITY_LOW);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.grid_optionsmenu, menu);
		this.optionsMenu = menu;
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.refresh).setEnabled(jobManagerConnection.refreshJob != null);
		menu.findItem(R.id.mark_seen).setVisible(currentCategory == 1);
		menu.findItem(R.id.mark_seen).setEnabled(cursor == null || cursor.getCount() >= 1);
			
		/* in case the tasks gets started before the options menu is created */
		if (jobManagerConnection.refreshJob != null && jobManagerConnection.refreshJob.isRunning())
			setProgressActionView(true);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		
		if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

		switch (item.getItemId()) {
		case R.id.mark_seen:
			db.updateFlags(null, Frupic.FLAG_UNSEEN, false);
			cursorChanged();
			invalidateOptionsMenu();
			return true;
			
		case R.id.refresh:
			db.updateFlags(null, Frupic.FLAG_NEW, false);
			jobManagerConnection.requestRefresh(0, FRUPICS_STEP);
			return true;

		case R.id.upload:
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(Intent.createChooser(intent,
					getString(R.string.upload)), REQUEST_PICK_PICTURE);
			return true;
			
		case R.id.preferences:
			intent = new Intent(this, FrupicPreferences.class);
			startActivity(intent);
			return true;
			
		case R.id.about:
			intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(android.view.ContextMenu menu, View v,
			android.view.ContextMenu.ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.grid_contextmenu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Frupic frupic = new Frupic((Cursor)adapter.getItem((int) info.position));

		menu.setHeaderTitle("#" + frupic.getId());
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
			db.updateFlags(frupic, Frupic.FLAG_FAV, !((frupic.getFlags() & Frupic.FLAG_FAV) == Frupic.FLAG_FAV));
			cursorChanged();
			return true;

		case R.id.openinbrowser:
			intent = new Intent("android.intent.action.VIEW", Uri.parse(frupic.getUrl()));
			startActivity(intent);
			return true;

		case R.id.details:
			DetailDialog.create(this, frupic).show();
			
			return true;

		case R.id.share_link:
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, frupic.getUrl());
			intent.putExtra(Intent.EXTRA_SUBJECT, "FruPic #" + frupic.getId());
			startActivity(Intent.createChooser(intent,
					getString(R.string.share_link)));
			return true;

		case R.id.cache_now:
			/* Make sure, destination directory exists */
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();
			
			DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			DownloadManager.Request req = new DownloadManager.Request(Uri.parse(frupic.getFullUrl()));
			req.setVisibleInDownloadsUi(true);
			
			req.allowScanningByMediaScanner();
			req.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			
			req.setTitle(frupic.getFileName(false));
			req.setDescription("Frupic " + frupic.getId());
			req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, frupic.getFileName(false));
			dm.enqueue(req);
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Intent intent;
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_PICK_PICTURE:
			if (resultCode == RESULT_OK) {
				if (data != null) {
					Uri uri = data.getData();
					intent = new Intent(this, UploadActivity.class);
					intent.setAction(Intent.ACTION_SEND);
					intent.putExtra(Intent.EXTRA_STREAM, uri);

					startActivity(intent);
				}
			}
			break;
		}
	}
	
	void setProgressActionView(boolean refreshing) {
		if (optionsMenu == null)
			return;
        final MenuItem refreshItem = optionsMenu.findItem(R.id.refresh);
        if (refreshItem != null) {
            if (refreshing) {
                if (mRefreshIndeterminateProgressView == null) {
                    LayoutInflater inflater = (LayoutInflater)
                            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    mRefreshIndeterminateProgressView = inflater.inflate(
                            R.layout.actionbar_indeterminate_progress, null);
                }

                refreshItem.setActionView(mRefreshIndeterminateProgressView);
            } else {
                refreshItem.setActionView(null);
            }
        }
	}

	void cursorChanged() {
		int ind = currentCategory;
		int mask = 0;
		
		if (ind == 2)
			mask |= Frupic.FLAG_FAV;
		if (ind == 1)
			mask |= Frupic.FLAG_UNSEEN;
		cursor = db.getFrupics(null, mask);
		
		adapter.changeCursor(cursor);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public boolean navigationItemSelected(int position, long id) {
		mDrawerLayout.closeDrawer(mDrawer);
		if (position <= 2) {
			currentCategory = position;
			mDrawerList.setItemChecked(currentCategory, true);
	        
	        getActionBar().setTitle(navigationAdapter.getItem(position));
	        if (Build.VERSION.SDK_INT >= 14) {
	        	getActionBar().setIcon(navigationAdapter.getIcon(position));
	        }
			cursorChanged();
			invalidateOptionsMenu();
		} else {
			Intent intent;
			mDrawerList.setItemChecked(currentCategory, true);
			switch (position) {
			case 4:
				intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*");
				startActivityForResult(Intent.createChooser(intent,
						getString(R.string.upload)), REQUEST_PICK_PICTURE);
				break;
			case 5:
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("http://frupic.frubar.net"));
				startActivity(intent);
				break;
			case 6:
				intent = new Intent(this, FrupicPreferences.class);
				startActivity(intent);
				break;
			}
		}
		return true;
	}
	
	
	
	@Override
	public void OnJobStarted(Job job) {
		setProgressActionView(true);		
	}

	@Override
	public void OnJobDone(Job job) {
		setProgressActionView(false);
		if (job.isFailed()) {
			Toast.makeText(this, jobManagerConnection.refreshJob.getError(), Toast.LENGTH_LONG).show();
		} else
			cursorChanged();
	}

	@Override
	public void OnJobProgress(Job job, int progress, int max) {
		/* ignore, because RefreshJob does not have progress */
	}
}