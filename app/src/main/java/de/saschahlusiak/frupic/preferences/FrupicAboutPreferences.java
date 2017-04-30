package de.saschahlusiak.frupic.preferences;

import de.saschahlusiak.frupic.R;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class FrupicAboutPreferences extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_about);		
	}
}
