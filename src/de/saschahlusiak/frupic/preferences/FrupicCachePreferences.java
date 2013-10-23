package de.saschahlusiak.frupic.preferences;

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
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;

public class FrupicCachePreferences extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	Preference clear_cache;
	
	private class PruneCacheTask extends AsyncTask<Void,Void,Void> {
		ProgressDialog progress;
		
		@Override
		protected void onPreExecute() {
			progress = new ProgressDialog(getActivity());
			progress.setIndeterminate(true);
			progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progress.setMessage(getString(R.string.please_wait));
			progress.show();
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			FileCache fileCache = new FileCache(getActivity());
			fileCache.pruneCache(0);
			FrupicDB db = new FrupicDB(getActivity());
			db.open();
			db.clearAll(false);
			db.close();
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			progress.dismiss();
			new UpdateCacheInfoTask().execute();
			super.onPostExecute(result);
		}
	}
	
	private class UpdateCacheInfoTask extends AsyncTask<Void,Void,Void> {
		CacheInfo cacheInfo;
		int cacheSize;
		
		@Override
		protected void onPreExecute() {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			cacheSize = Integer.parseInt(prefs.getString("cache_size", "16777216"));
			clear_cache.setSummary(R.string.calculating);
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			cacheInfo = new FileCache(getActivity()).getCacheInfo();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (!isVisible())
				return;
			clear_cache.setSummary(getString(R.string.preferences_cache_clear_summary, 
					cacheInfo.getCount(), 
					(float)cacheInfo.getSize() / 1024.0f / 1024.0f, 
					100.0f * (float)cacheInfo.getSize() / (float)cacheSize));
			clear_cache.setEnabled(cacheInfo.getCount() > 0);
			super.onPostExecute(result);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_cache);
		
		clear_cache = findPreference("clear_cache");
		clear_cache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new PruneCacheTask().execute();
				return true;
			}
		});
	}
	
	@Override
	public void onResume() {
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
		onSharedPreferenceChanged(prefs, "cache_size");
		prefs.registerOnSharedPreferenceChangeListener(this);

		new UpdateCacheInfoTask().execute();
		
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
		if (key.equals("cache_size")) {
			ListPreference pref = (ListPreference)findPreference(key);
			pref.setSummary(pref.getEntry());
			new UpdateCacheInfoTask().execute();
		}
		if (key.equals("always_keep_starred")) {
			new UpdateCacheInfoTask().execute();
		}
	}	
}
