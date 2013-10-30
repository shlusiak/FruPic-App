package de.saschahlusiak.frupic.services;

import de.saschahlusiak.frupic.services.Job.JobState;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class JobManager extends Service {
    private final IBinder mBinder = new JobManagerBinder();
    
	static final String INDEX_URL = "http://api.freamware.net/2.0/get.picture";
	static final String tag = JobManager.class.getSimpleName();

    Handler handler;

	class JobThread extends Thread {
		Job job;
		
		JobThread(Job job) {
			this.job = job;
		}
		
		@Override
		public void run() {
			job.setState(JobState.JOB_RUNNING);
			handler.post(new Runnable() {
				@Override
				public void run() {
					job.onJobStarted();
				}
			});
			JobState res = job.run();
			job.setState(res);
			handler.post(new Runnable() {
				@Override
				public void run() {
					job.onJobDone();
				}
			});
		}
	}

    public class JobManagerBinder extends Binder {
        public JobManager getService() {
            return JobManager.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	handler = new Handler();
    	Log.d(tag, "onCreate");
    }
    
    @Override
    public void onDestroy() {
    	Log.d(tag, "onDestroy");
    	super.onDestroy();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.d(tag, "onStartCommand");
    	return super.onStartCommand(intent, flags, startId);
    }
    
    public void post(Job job) {
    	if (job.isRunning())
    		return;
    	
    	new JobThread(job).start();
    }
}
