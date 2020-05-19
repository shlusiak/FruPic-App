package de.saschahlusiak.frupic.preferences;

import java.util.List;

import de.saschahlusiak.frupic.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.StatsSnapshot;

public class FrupicPreferences extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		Picasso picasso = Picasso.get();
		StatsSnapshot stats = picasso.getSnapshot();
		stats.dump();
	}
	
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
		super.onBuildHeaders(target);
	}
	
	@Override
	protected boolean isValidFragment(String fragmentName) {
		if (fragmentName.equals(FrupicCachePreferences.class.getName())) return true;
		if (fragmentName.equals(FrupicDisplayPreferences.class.getName())) return true;
		if (fragmentName.equals(FrupicUploadPreferences.class.getName())) return true;
		if (fragmentName.equals(FrupicAboutPreferences.class.getName())) return true;

		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return(true);
		}
		return super.onOptionsItemSelected(item);
	}
}
