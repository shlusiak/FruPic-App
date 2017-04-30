package de.saschahlusiak.frupic.services;

import java.io.File;

import android.graphics.Bitmap;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.model.FrupicFactory.OnFetchProgress;

public class FetchThumbnailJob extends Job implements OnFetchProgress {
	Frupic frupic;
	FrupicFactory factory;
	Bitmap bitmap;
	boolean cached;
	
	public FetchThumbnailJob(Frupic frupic, FrupicFactory factory) {
		this.frupic = frupic;
		this.factory = factory;
	}

	@Override
	JobState run() {
		File file = factory.getCacheFile(frupic, true);
		if (file.exists() && file.canRead())
			this.cached = true;
		
		bitmap = factory.getThumbnail(frupic, this);
		if (bitmap == null)
			return JobState.JOB_FAILED;
		
		return JobState.JOB_SUCCESS;
	}
	
	public Frupic getFrupic() {
		return frupic;
	}
	
	public Bitmap getBitmap() {
		return bitmap;
	}
	
	public boolean isFromCache() {
		return cached;
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
