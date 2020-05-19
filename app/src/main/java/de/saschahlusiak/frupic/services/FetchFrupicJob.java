package de.saschahlusiak.frupic.services;

import android.util.Log;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.model.FrupicFactory.OnFetchProgress;

@Deprecated
public class FetchFrupicJob extends Job implements OnFetchProgress {
	private static final String tag = FetchFrupicJob.class.getSimpleName();
	private Frupic frupic;
	private FrupicFactory factory;
	
	FetchFrupicJob(Frupic frupic, FrupicFactory factory) {
		this.frupic = frupic;
		this.factory = factory;
	}

	@Override
	public JobState run() {
		Log.d(tag, "fetching #" + frupic.id);
		if (!factory.fetchFrupicImage(frupic, this)) {
			Log.d(tag, "failed #" + frupic.id);
			return JobState.JOB_FAILED;
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
