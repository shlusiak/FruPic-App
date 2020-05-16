package de.saschahlusiak.frupic.preferences;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import de.saschahlusiak.frupic.R;

public class FrupicCachePreferences extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_cache);
	}
}
