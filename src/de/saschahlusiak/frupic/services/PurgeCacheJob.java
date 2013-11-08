package de.saschahlusiak.frupic.services;

import de.saschahlusiak.frupic.model.FrupicFactory;

public class PurgeCacheJob extends Job {
	FrupicFactory factory;
	
	public PurgeCacheJob(FrupicFactory factory) {
		this.factory = factory;
	}

	@Override
	JobState run() {
		factory.getFileCache().pruneCache();
		return null;
	}
}
