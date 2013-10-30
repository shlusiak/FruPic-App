package de.saschahlusiak.frupic.services;

import java.util.concurrent.LinkedBlockingQueue;

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
	
	static final int WORKER_THREADS = 1;

    Handler handler;
    JobWorker worker[] = new JobWorker[WORKER_THREADS];
    LinkedBlockingQueue<Job> jobsWaiting = new LinkedBlockingQueue<Job>();
    LinkedBlockingQueue<Job> jobsRunning = new LinkedBlockingQueue<Job>(worker.length);

	class JobWorker extends Thread {				
		@Override
		public void run() {
			while (!isInterrupted()) {
				final Job job;

				try {
					job = jobsWaiting.take();
					jobsRunning.add(job);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
				
				job.setState(JobState.JOB_RUNNING);
				handler.post(new Runnable() {
					@Override
					public void run() {
						job.onJobStarted();
					}
				});
				JobState res = job.run();
				job.setState(res);
				jobsRunning.remove(job);
				handler.post(new Runnable() {
					@Override
					public void run() {
						job.onJobDone();
					}
				});
			}
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
    	for (int i = 0; i < worker.length; i++) {
    		worker[i] = new JobWorker();
    		worker[i].start();
    	}
    }
    
    @Override
    public void onDestroy() {
    	Log.d(tag, "onDestroy");
    	for (int i = 0; i < worker.length; i++) {
    		worker[i].interrupt();
    	}
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

   		jobsWaiting.add(job);
    }
}
