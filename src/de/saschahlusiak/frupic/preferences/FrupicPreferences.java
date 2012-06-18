package de.saschahlusiak.frupic.preferences;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.model.FrupicFactory.CacheInfo;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class FrupicPreferences extends PreferenceActivity {
	Preference clear_cache;
	
	class PruneCacheTask extends AsyncTask<Void,Void,Void> {
		ProgressDialog progress;
		
		@Override
		protected void onPreExecute() {
			progress = new ProgressDialog(FrupicPreferences.this);
			progress.setIndeterminate(true);
			progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progress.setMessage(getString(R.string.please_wait));
			progress.show();
			super.onPreExecute();
		}
		
		
		@Override
		protected Void doInBackground(Void... params) {
			FrupicFactory factory = new FrupicFactory(FrupicPreferences.this);
			factory.pruneCache(factory, 0);
			FrupicDB db = new FrupicDB(FrupicPreferences.this);
			db.open();
			db.clearAll(false);
			db.close();
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			progress.dismiss();
			updateCachePreference();
			super.onPostExecute(result);
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		clear_cache = findPreference("clear_cache");
		clear_cache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new PruneCacheTask().execute();
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
