package de.saschahlusiak.frupic.services;

import java.util.ArrayList;

public abstract class Job {
	public interface OnJobListener {
		/** this is called in the main thread */
		public void OnJobStarted(Job job);
		
		/** this is called in the worker thread context */
		public void OnJobProgress(Job job, int progress, int max);
		
		/** this is called in the main thread */
		public void OnJobDone(Job job);
	}
		
	public enum JobState {
		JOB_IDLE,
		JOB_SCHEDULED,
		JOB_RUNNING,
		JOB_SUCCESS,
		JOB_FAILED,
		JOB_CANCELLED
	};
	
	public enum Priority {
		PRIORITY_LOW,
		PRIORITY_HIGH
	};
	
	ArrayList<OnJobListener> jobListener = new ArrayList<OnJobListener>();
	Object tag;
	JobState state;
	Thread thread;
	
	public Job() {
		state = JobState.JOB_IDLE;
		thread = null;
	}
	
	public void addJobListener(OnJobListener listener) {
		synchronized (jobListener) {
			jobListener.add(listener);
		}
	}
	
	public void removeJobListener(OnJobListener listener) {
		synchronized (jobListener) {
			jobListener.remove(listener);
		}
	}
	
	public void setTag(Object tag) {
		this.tag = tag;
	}
	
	public Object getTag() {
		return tag;
	}
	
	public JobState getState() {
		return state;
	}
	
	void onJobStarted() {		
		synchronized (jobListener) {
			for (OnJobListener jl: jobListener)
				jl.OnJobStarted(this);
		}
	}
	
	void onJobDone() {
		synchronized (jobListener) {
			for (OnJobListener jl: jobListener)
				jl.OnJobDone(this);
		}
	}
	
	public void cancel() {
		if (thread != null)
			thread.interrupt();
		
		setState(JobState.JOB_CANCELLED);
	}
	
	synchronized void setState(JobState state) {
		this.state = state;
	}
	
	public final synchronized boolean isRunning() {
		return getState() == JobState.JOB_RUNNING;
	}
	
	public final synchronized boolean isScheduled() {
		return getState() == JobState.JOB_SCHEDULED;
	}

	public final synchronized boolean isFailed() {
		return getState() == JobState.JOB_FAILED;
	}
	
	public final synchronized boolean isCancelled() {
		return getState() == JobState.JOB_CANCELLED;
	}
	
	abstract JobState run();
}
