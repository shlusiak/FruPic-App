package de.saschahlusiak.frupic.preferences;

import java.util.List;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.cache.FileCache;
import de.saschahlusiak.frupic.cache.FileCache.CacheInfo;
import de.saschahlusiak.frupic.db.FrupicDB;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class FrupicPreferences extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
		super.onBuildHeaders(target);
	}
	
}
