package de.saschahlusiak.frupic.grid;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.LruCache;
import android.view.*;
import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.about.AboutActivity;
import de.saschahlusiak.frupic.cache.FileCacheUtils;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.preferences.FrupicPreferences;
import de.saschahlusiak.frupic.services.*;
import de.saschahlusiak.frupic.services.Job.OnJobListener;
import de.saschahlusiak.frupic.services.Job.Priority;
import de.saschahlusiak.frupic.services.JobManager.JobManagerBinder;
import de.saschahlusiak.frupic.upload.UploadActivity;
import me.leolin.shortcutbadger.ShortcutBadger;

public class FruPicGridActivity extends AppCompatActivity implements OnJobListener, ViewPager.OnPageChangeListener, SwipeRefreshLayout.OnRefreshListener {
	static private final String tag = FruPicGridActivity.class.getSimpleName();
	static private final int REQUEST_PICK_PICTURE = 1;

	private PurgeCacheJob purgeCacheJob;
	private ConnectivityManager cm;

	/* access from GridAdapter */
	private JobManagerConnection jobManagerConnection;

	public static final int FRUPICS_STEP = 100;

	private SharedPreferences prefs;

	private ViewPager viewPager;
	private ViewPagerAdapter viewPagerAdapter;

	private SwipeRefreshLayout swipeRefreshLayout;


	private static class JobManagerConnection implements ServiceConnection {
		FruPicGridActivity activity;

		JobManager jobManager;
		RefreshJob refreshJob;

		JobManagerConnection(FruPicGridActivity activity) {
			this.activity = activity;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(tag, "onServiceConnected");
			jobManager = ((JobManagerBinder) service).getService();
			refreshJob = jobManager.getRefreshJob();
			refreshJob.addJobListener(activity);
			if (refreshJob.isRunning())
				activity.OnJobStarted(refreshJob);
			else
				requestRefresh(0, FRUPICS_STEP);
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

	static class RetainedConfig {
		JobManagerConnection jobManagerConnection;
		LruCache<Integer, Bitmap> cache;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.grid_activity);


		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(this);

		TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
		tabLayout.setupWithViewPager(viewPager);

		swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
		swipeRefreshLayout.setOnRefreshListener(this);

		RetainedConfig retainedConfig = (RetainedConfig) getLastCustomNonConfigurationInstance();
		if (retainedConfig != null) {
			jobManagerConnection = retainedConfig.jobManagerConnection;
		}
		
		if (jobManagerConnection != null) { 
			jobManagerConnection.activity = this;
			if (jobManagerConnection.refreshJob != null)
				jobManagerConnection.refreshJob.addJobListener(this);
			invalidateOptionsMenu();
		} else {
			jobManagerConnection = new JobManagerConnection(this);
			Intent intent = new Intent(this, JobManager.class);
			getApplicationContext().bindService(intent, jobManagerConnection, Context.BIND_AUTO_CREATE);
		}
	    cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

		findViewById(R.id.upload).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*");
				startActivityForResult(Intent.createChooser(intent,
					getString(R.string.upload)), REQUEST_PICK_PICTURE);
			}
		});
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		purgeCacheJob = new PurgeCacheJob(new FileCacheUtils(this));
	    
		Intent intent = new Intent(this, AutoRefreshManager.class);
		stopService(intent);

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
			
			int interval = Integer.parseInt(prefs.getString("autorefresh", "86400"));
			scheduleAlarm(prefs.getBoolean("autorefresh_enabled", true), interval);
		}

		FrupicDB db = new FrupicDB(this);
		if (db.open()) {
			db.updateFlags(null, Frupic.FLAG_NEW, false);
			db.close();
		}

		super.onDestroy();
	}

	public void scheduleAlarm(boolean enable, int interval) {
		PackageManager pm = getPackageManager();
		AlarmManager alarmMgr;
		PendingIntent alarmIntent;

		alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this, AutoRefreshManager.class);
		alarmIntent = PendingIntent.getService(this, 0, intent, 0);

		if (enable) {
			Log.d(tag, "scheduling alarm");
			alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
				interval,
				interval,
				alarmIntent);
		} else {
			Log.d(tag, "cancel alarm");
			alarmMgr.cancel(alarmIntent);
		}
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		RetainedConfig retain = new RetainedConfig();

		if (jobManagerConnection.jobManager != null) {
			jobManagerConnection.refreshJob.removeJobListener(this);
			jobManagerConnection.activity = null;
			retain.jobManagerConnection = jobManagerConnection;
			jobManagerConnection = null;
		}

		return retain;
	}

	@Override
	protected void onStart() {
		/* recreate the factory fileCache object to reread changed 
		 * preferences
		 */
		purgeCacheJob = new PurgeCacheJob(new FileCacheUtils(this));

		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (jobManagerConnection != null && jobManagerConnection.jobManager != null)
			jobManagerConnection.jobManager.post(purgeCacheJob, Priority.PRIORITY_LOW);

		super.onStop();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.grid_optionsmenu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		
		switch (item.getItemId()) {
		case R.id.mark_seen:
			// TODO
//			db.updateFlags(null, Frupic.FLAG_UNSEEN, false);
//			cursorChanged();
			invalidateOptionsMenu();
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

		case R.id.openinbrowser:
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("http://frupic.frubar.net"));
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
	
	@Override
	public void OnJobStarted(Job job) {
		swipeRefreshLayout.setRefreshing(true);
	}

	@Override
	public void OnJobProgress(Job job, int progress, int max) {
		/* ignore, because RefreshJob does not have progress */
	}

	@Override
	public void OnJobDone(Job job) {
		swipeRefreshLayout.setRefreshing(false);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

	}

	@Override
	public void onPageSelected(int position) {
		Log.d(tag, "onPageSelected: " + position);
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		swipeRefreshLayout.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
	}

	@Override
	public void onRefresh() {
		FrupicDB db = new FrupicDB(this);
		if (db.open()) {
			db.updateFlags(null, Frupic.FLAG_NEW, false);
			db.close();
		}

		jobManagerConnection.requestRefresh(0, FRUPICS_STEP);
	}

	private class ViewPagerAdapter extends FragmentPagerAdapter {

		public ViewPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0: return getString(R.string.all_frupics);
				case 1: return getString(R.string.unseen);
				case 2: return getString(R.string.starred);
				default: return String.format("Fragment " + position);
			}
		}

		@Override
		public Fragment getItem(int position) {
			FruPicGridFragment f;
			Bundle args = new Bundle();
			switch (position) {
				case 0:
					f = new FruPicGridFragment();
					args.putInt("nav", 0);
					break;
				case 1:
					f = new FruPicGridFragment();
					args.putInt("nav", 1);
					break;
				case 2:
					f = new FruPicGridFragment();
					args.putInt("nav", 2);
					break;
				default:
					throw new IllegalArgumentException("Fragment " + position + " not supported");
			}
			f.setArguments(args);
			return f;
		}

		@Override
		public int getCount() {
			return 3;
		}
	}
}