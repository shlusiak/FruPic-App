package de.saschahlusiak.frupic.services;

import java.util.ArrayList;

public abstract class Job {
	public interface OnJobListener {
		public void OnJobStarted(Job job);
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
	
	public Job() {
		state = JobState.JOB_IDLE;
	}
	
	public void addJobDoneListener(OnJobListener listener) {
		synchronized (jobListener) {
			jobListener.add(listener);
		}
	}
	
	public void removeJobDoneListener(OnJobListener listener) {
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
	
	synchronized void onJobStarted() {		
		synchronized (jobListener) {
			for (OnJobListener jl: jobListener)
				jl.OnJobStarted(this);
		}
	}
	
	synchronized void onJobDone() {
		synchronized (jobListener) {
			for (OnJobListener jl: jobListener)
				jl.OnJobDone(this);
		}
	}
	
	public void cancel() {
		setState(JobState.JOB_CANCELLED);
	}
	
	synchronized void setState(JobState state) {
		this.state = state;
	}
	
	public final synchronized boolean isRunning() {
		return 	getState() == JobState.JOB_RUNNING;
	}
	
	public final synchronized boolean isScheduled() {
		return 	getState() == JobState.JOB_SCHEDULED;
	}

	public final synchronized boolean isFailed() {
		return 	getState() == JobState.JOB_FAILED;
	}
	
	abstract JobState run();
}
