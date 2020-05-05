package de.saschahlusiak.frupic.services;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.services.Job.JobState;
import de.saschahlusiak.frupic.services.Job.OnJobListener;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class JobManager extends Service {
    private final IBinder mBinder = new JobManagerBinder();
    
	static final String INDEX_URL = "https://api.freamware.net/2.0/get.picture";
	static final String tag = JobManager.class.getSimpleName();
	
	static final int WORKER_THREADS = 20;
	
    Handler handler;
    JobWorker worker[] = new JobWorker[WORKER_THREADS];
    LinkedBlockingDeque<Job> jobsWaiting = new LinkedBlockingDeque<Job>();
    ArrayBlockingQueue<Job> jobsRunning = new ArrayBlockingQueue<Job>(worker.length);
    
    RefreshJob refreshJob;

	class JobWorker extends Thread {
		public boolean goDown;
		
		public JobWorker() {
			goDown = false;
		}
		
		@Override
		public void run() {
			while (!goDown) {
				final Job job;

				try {
					job = jobsWaiting.take();
					if (job.getState() == JobState.JOB_CANCELLED)
						continue;
					jobsRunning.add(job);
				} catch (InterruptedException e) {
					return;
				}
				
				job.setState(JobState.JOB_RUNNING);
				job.thread = this;
				handler.post(new Runnable() {
					@Override
					public void run() {
						job.onJobStarted();
					}
				});
				JobState res = job.run();
				job.thread = null;
				if (job.getState() != JobState.JOB_CANCELLED)
					job.setState(res);
				jobsRunning.remove(job);
				handler.post(new Runnable() {
					@Override
					public void run() {
						job.onJobDone();
						if (jobsWaiting.isEmpty() && jobsRunning.isEmpty())
							stopSelf();
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
    	refreshJob = new RefreshJob(getApplicationContext());
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
    		worker[i].goDown = true;
    		worker[i].interrupt();
    	}
    	super.onDestroy();
    }
    
    public synchronized void removeAllJobListener(OnJobListener listener) {
    	for (Job job: jobsWaiting)
    		job.removeJobListener(listener);
    	for (Job job: jobsRunning)
    		job.removeJobListener(listener);
    }
    
    public RefreshJob getRefreshJob() {
    	return refreshJob;
    }
    
    public FetchFrupicJob getFetchJob(Frupic frupic, FrupicFactory factory) {
    	FetchFrupicJob j = null;
    	
    	for (Job job: jobsWaiting)
    		if (job instanceof FetchFrupicJob)
    			if (((FetchFrupicJob)job).getFrupic().equals(frupic))
    				j = (FetchFrupicJob)job;
    	
    	for (Job job: jobsRunning)
    		if (job instanceof FetchFrupicJob)
    			if (((FetchFrupicJob)job).getFrupic().equals(frupic))
    				j = (FetchFrupicJob)job;
    	
    	if (j == null)
    		j = new FetchFrupicJob(frupic, factory);
    	
    	j.setFrupic(frupic);
    	
    	return j;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.d(tag, "onStartCommand");
    	return super.onStartCommand(intent, flags, startId);
    }
    
    public void post(Job job, Job.Priority priority) {
    	if (job.isScheduled())
    		return;
    	if (job.isRunning())
    		return;
    	
    	job.setState(JobState.JOB_SCHEDULED);

    	if (priority == Job.Priority.PRIORITY_HIGH)
    		jobsWaiting.addFirst(job);
    	else
    		jobsWaiting.addLast(job);
    }
}
