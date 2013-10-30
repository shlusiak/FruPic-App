package de.saschahlusiak.frupic.services;

import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;

public class FetchJob extends Job {
	Frupic frupic;
	FrupicFactory factory;
	int result;
	
	public FetchJob(Frupic frupic, FrupicFactory factory) {
		this.frupic = frupic;
		this.factory = factory;
	}

	@Override
	JobState run() {
		result = factory.fetch(frupic, true, null, httpClient);
		return JobState.JOB_SUCCESS;
	}
	
	public Frupic getFrupic() {
		return frupic;
	}
	
	public int getResult() {
		return result;
	}
}
