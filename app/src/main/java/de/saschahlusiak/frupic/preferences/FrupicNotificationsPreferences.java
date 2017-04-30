package de.saschahlusiak.frupic.preferences;

import de.saschahlusiak.frupic.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

public class FrupicNotificationsPreferences extends PreferenceFragment implements OnSharedPreferenceChangeListener {	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_notifications);
	}
	
	@Override
	public void onResume() {
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
		onSharedPreferenceChanged(prefs, "autorefresh");
		prefs.registerOnSharedPreferenceChangeListener(this);
		super.onResume();
	}
	
	@Override
	public void onPause() {
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("autorefresh")) {
			ListPreference pref = (ListPreference)findPreference("autorefresh");
			pref.setSummary(pref.getEntry());
		}
	}
}
