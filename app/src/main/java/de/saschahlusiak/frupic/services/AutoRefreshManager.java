package de.saschahlusiak.frupic.services;

import android.app.*;
import me.leolin.shortcutbadger.ShortcutBadger;
import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.grid.FruPicGridActivity;
import de.saschahlusiak.frupic.services.Job.OnJobListener;
import de.saschahlusiak.frupic.services.Job.Priority;
import de.saschahlusiak.frupic.services.JobManager.JobManagerBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class AutoRefreshManager extends IntentService implements OnJobListener, ServiceConnection {
	private static final String tag = AutoRefreshManager.class.getSimpleName();
	
	private static final int nId = 2; /* must be a unique notification id */
	
	private JobManager jobManager;
	private RefreshJob refreshJob;
	private ConnectivityManager cm;
	private NotificationManager nm;
	private SharedPreferences prefs;
	
	private boolean only_on_wifi;
	
	private int new_pictures;

	/**
	 * Creates an IntentService.  Invoked by your subclass's constructor.
	 */
	public AutoRefreshManager() {
		super("AutoRefreshManager");
	}


	@Override
	public void onCreate() {
		/* TODO: run on device boot? */
		Log.d(tag, "onCreate");
		Intent intent = new Intent(this, JobManager.class);
		bindService(intent, this, BIND_AUTO_CREATE);
	    cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		only_on_wifi = prefs.getBoolean("refresh_only_on_wlan", false);
		new_pictures = 0;

		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		Log.d(tag, "OnDestroy");
		if (refreshJob != null)
			refreshJob.removeJobListener(this);
		refreshJob = null;
		jobManager = null;
		unbindService(this);
		nm.cancel(nId);
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(tag, "onHandleIntent");
		doRefresh();
	}
	
	/**
	 * @return next interval
	 */
	private void doRefresh() {
		if (refreshJob != null && !refreshJob.isRunning() && !refreshJob.isScheduled()) {
	    	NetworkInfo ni = cm.getActiveNetworkInfo();
	    	/* TODO: schedule sooner or listen to network change event? */
	    	if (ni == null)
	    		return;

	    	if (!ni.isConnected())
	    		return;

	    	if (only_on_wifi && ni.getType() != ConnectivityManager.TYPE_WIFI) {
//	    		Log.d(tag, "not on wifi, skip");
	    		return;
	    	}

			refreshJob.setRange(0, FruPicGridActivity.FRUPICS_STEP);
			jobManager.post(refreshJob, Priority.PRIORITY_LOW);

			// FIXME: refresh is asynchronous but this service terminates when this method terminates and kills the job
		}
	}
	
	@Override
	public void OnJobStarted(Job job) {
		
	}

	@Override
	public void OnJobProgress(Job job, int progress, int max) {
		
	}

	@Override
	public void OnJobDone(Job job) {
		Log.d(tag, "Refresh finished, new frupics: " + refreshJob.getLastCount());
		if (refreshJob.getLastCount() == 0)
			return;
		
		new_pictures += refreshJob.getLastCount();
		
		ShortcutBadger.with(this).count(new_pictures);
		
		Notification.Builder builder = new Notification.Builder(this);

		builder.setContentTitle(getString(R.string.app_name));
		builder.setSmallIcon(R.drawable.frupic_notification_new);
		builder.setContentText(getString(R.string.refresh_service_count_text, new_pictures));
		builder.setTicker(getString(R.string.refresh_service_count_text, new_pictures));
		builder.setAutoCancel(true);
		builder.setOngoing(false);
		Intent intent = new Intent(this, FruPicGridActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);
		
		nm.notify(nId, builder.getNotification());
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		jobManager = ((JobManagerBinder)service).getService();
		refreshJob = jobManager.getRefreshJob();
		refreshJob.addJobListener(this);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		if (refreshJob != null)
			refreshJob.removeJobListener(this);
		refreshJob = null;
		jobManager = null;
	}
}
