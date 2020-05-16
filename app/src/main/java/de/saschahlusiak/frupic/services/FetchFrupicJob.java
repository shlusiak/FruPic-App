package de.saschahlusiak.frupic.services;

import android.util.Log;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.model.FrupicFactory.OnFetchProgress;

@Deprecated
public class FetchFrupicJob extends Job implements OnFetchProgress {
	static final String tag = FetchFrupicJob.class.getSimpleName();
	Frupic frupic;
	FrupicFactory factory;
	
	FetchFrupicJob(Frupic frupic, FrupicFactory factory) {
		this.frupic = frupic;
		this.factory = factory;
	}

	@Override
	public JobState run() {
		try {
			Log.d(tag, "fetching #" + frupic.id);
			factory.fetchFrupicImage(frupic, false, this);
		}
		catch (InterruptedException e) {
			Log.d(tag, "cancelled #" + frupic.id);

			return JobState.JOB_CANCELLED;
		}
		Log.d(tag, "fetched #" + frupic.id);

		return JobState.JOB_SUCCESS;
	}
	
	void setFrupic(Frupic frupic) {
		this.frupic = frupic;
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
