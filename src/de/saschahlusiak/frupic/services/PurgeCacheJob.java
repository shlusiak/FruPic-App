package de.saschahlusiak.frupic.services;

import de.saschahlusiak.frupic.cache.FileCacheUtils;

public class PurgeCacheJob extends Job {
	FileCacheUtils cacheUtils;
	
	public PurgeCacheJob(FileCacheUtils cacheUtils) {
		this.cacheUtils = cacheUtils;
	}

	@Override
	JobState run() {
		cacheUtils.pruneCache();
		return null;
	}
}
