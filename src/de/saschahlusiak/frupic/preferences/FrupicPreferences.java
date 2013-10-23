package de.saschahlusiak.frupic.preferences;

import java.util.List;

import de.saschahlusiak.frupic.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class FrupicPreferences extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
		super.onBuildHeaders(target);
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
