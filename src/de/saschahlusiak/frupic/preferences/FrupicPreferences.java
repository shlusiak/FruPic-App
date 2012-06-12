package de.saschahlusiak.frupic.preferences;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.model.FrupicFactory.CacheInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class FrupicPreferences extends PreferenceActivity {
	Preference clear_cache;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		clear_cache = findPreference("clear_cache");
		clear_cache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				FrupicFactory factory = new FrupicFactory(FrupicPreferences.this);
				factory.pruneCache(new FrupicFactory(FrupicPreferences.this), 0);
				updateCachePreference();
				return true;
			}
		});
		updateCachePreference();
	}
	
	
	public void updateCachePreference() {
		FrupicFactory factory = new FrupicFactory(FrupicPreferences.this);		
		CacheInfo info = factory.pruneCache(new FrupicFactory(this), -1);
		clear_cache.setSummary(getString(R.string.preferences_cache_clear_summary, info.getCount(), info.getSize() / 1024));
		clear_cache.setEnabled(info.getCount() > 0);
	}
}
