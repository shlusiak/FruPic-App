package de.saschahlusiak.frupic.services;

import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.model.FrupicFactory.OnFetchProgress;

public class FetchFrupicJob extends Job implements OnFetchProgress {
	Frupic frupic;
	FrupicFactory factory;
	
	public FetchFrupicJob(Frupic frupic, FrupicFactory factory) {
		this.frupic = frupic;
		this.factory = factory;
	}

	@Override
	public JobState run() {
		try {			
			factory.fetchFrupicImage(frupic, false, this);
		}
		catch (InterruptedException e) {
			return JobState.JOB_CANCELLED;
		}
		
		return JobState.JOB_SUCCESS;
	}
	
	public Frupic getFrupic() {
		return frupic;
	}
	
	@Override
	public void OnProgress(int read, int length) {
		synchronized (jobListener) {
			for (OnJobListener l: jobListener) {
				l.OnJobProgress(this, read, length);
			}
		}
	}
}
