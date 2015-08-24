package de.saschahlusiak.frupic.services;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.grid.FruPicGrid;
import de.saschahlusiak.frupic.services.Job.OnJobListener;
import de.saschahlusiak.frupic.services.Job.Priority;
import de.saschahlusiak.frupic.services.JobManager.JobManagerBinder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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

public class AutoRefreshManager extends Service implements ServiceConnection, Runnable, OnJobListener {
	private static final String tag = AutoRefreshManager.class.getSimpleName();
	
	public static final int DEFAULT_INTERVAL = 7200; /* 7200 sec */
	
	private static final int nId = 2; /* must be a unique notification id */
	
	Handler handler = new Handler();
	JobManager jobManager;
	RefreshJob refreshJob;
	ConnectivityManager cm;
	NotificationManager nm;
	SharedPreferences prefs;
	
	boolean only_on_wifi;
	
	int new_pictures;
	
	int interval = DEFAULT_INTERVAL;
	
	@Override
	public void onCreate() {
		/* TODO: run on device boot? */
		/* TODO: persistate the current waiting time, in case service is killed? */
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
			refreshJob.removeJobDoneListener(this);
		handler.removeCallbacks(this);
		refreshJob = null;
		jobManager = null;
		unbindService(this);
		nm.cancel(nId);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		interval = intent.getIntExtra("interval", DEFAULT_INTERVAL) * 1000;
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		jobManager = ((JobManagerBinder)service).getService();
		refreshJob = jobManager.getRefreshJob();
		refreshJob.addJobDoneListener(this);
		handler.postDelayed(this, interval);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		if (refreshJob != null)
			refreshJob.removeJobDoneListener(this);
		refreshJob = null;
		jobManager = null;
		handler.removeCallbacks(this);
	}
	
	@Override
	public void run() {
//		Log.d(tag, "autorefresh");
		handler.postDelayed(this, interval);
		
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

			refreshJob.setRange(0, FruPicGrid.FRUPICS_STEP);
			jobManager.post(refreshJob, Priority.PRIORITY_LOW);
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
		if (refreshJob.getLastCount() == 0)
			return;
		
		new_pictures += refreshJob.getLastCount();
		
		Notification.Builder builder = new Notification.Builder(this);

		builder.setContentTitle(getString(R.string.app_name));
		builder.setSmallIcon(R.drawable.frupic_notification_new);
		builder.setContentText(getString(R.string.refresh_service_count_text, new_pictures));
		builder.setTicker(getString(R.string.refresh_service_count_text, new_pictures));
		builder.setAutoCancel(true);
		builder.setOngoing(false);
		Intent intent = new Intent(this, FruPicGrid.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);
		
		nm.notify(nId, builder.getNotification());
	}
}
