package de.saschahlusiak.frupic.services;

import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.model.FrupicFactory.OnFetchProgress;

public class FetchJob extends Job implements OnFetchProgress {
	Frupic frupic;
	FrupicFactory factory;
	boolean thumb;
	int result;
	
	public FetchJob(Frupic frupic, boolean thumb, FrupicFactory factory) {
		this.frupic = frupic;
		this.factory = factory;
		this.thumb = thumb;
	}

	@Override
	JobState run() {
		result = factory.fetch(frupic, thumb, this);
		return JobState.JOB_SUCCESS;
	}
	
	public Frupic getFrupic() {
		return frupic;
	}
	
	public int getResult() {
		return result;
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
