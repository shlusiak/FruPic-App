package de.saschahlusiak.frupic.grid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Observer;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.about.AboutActivity;
import de.saschahlusiak.frupic.app.App;
import de.saschahlusiak.frupic.app.FrupicRepository;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.preferences.FrupicPreferences;
import de.saschahlusiak.frupic.upload.UploadActivity;

public class GridActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener, SwipeRefreshLayout.OnRefreshListener {
	static private final String tag = GridActivity.class.getSimpleName();
	static private final int REQUEST_PICK_PICTURE = 1;

	public static final int FRUPICS_STEP = 100;

	private ViewPager viewPager;
	private ViewPagerAdapter viewPagerAdapter;

	private SwipeRefreshLayout swipeRefreshLayout;

	@Inject
	protected FrupicRepository repository;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		((App)getApplication()).appComponent.inject(this);

		setContentView(R.layout.grid_activity);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

        viewPager = findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(this);

		TabLayout tabLayout = findViewById(R.id.tabs);
		tabLayout.setupWithViewPager(viewPager);

		swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
		swipeRefreshLayout.setOnRefreshListener(this);

		findViewById(R.id.upload).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_PICK);
				intent.setType("image/*");
				startActivityForResult(Intent.createChooser(intent,
					getString(R.string.upload)), REQUEST_PICK_PICTURE);
			}
		});

		repository.getSynchronizing().observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean synchronizing) {
				swipeRefreshLayout.setRefreshing(synchronizing);
			}
		});
	}

	@Override
	protected void onDestroy() {
		FrupicDB db = new FrupicDB(this);
		if (db.open()) {
			db.updateFlags(null, Frupic.FLAG_NEW, false);
			db.close();
		}

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.grid_optionsmenu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		
		switch (item.getItemId()) {
		case R.id.mark_seen:
			// TODO
//			db.updateFlags(null, Frupic.FLAG_UNSEEN, false);
//			cursorChanged();
			invalidateOptionsMenu();
			return true;
			
		case R.id.upload:
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(Intent.createChooser(intent,
					getString(R.string.upload)), REQUEST_PICK_PICTURE);
			return true;
			
		case R.id.preferences:
			intent = new Intent(this, FrupicPreferences.class);
			startActivity(intent);
			return true;

		case R.id.openinbrowser:
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("http://frupic.frubar.net"));
			startActivity(intent);
			return true;
			
		case R.id.about:
			intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Intent intent;
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_PICK_PICTURE:
			if (resultCode == RESULT_OK) {
				if (data != null) {
					Uri uri = data.getData();
					intent = new Intent(this, UploadActivity.class);
					intent.setAction(Intent.ACTION_SEND);
					intent.putExtra(Intent.EXTRA_STREAM, uri);

					startActivity(intent);
				}
			}
			break;
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

	}

	@Override
	public void onPageSelected(int position) {
		Log.d(tag, "onPageSelected: " + position);
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		swipeRefreshLayout.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
	}

	@Override
	public void onRefresh() {
		FrupicDB db = new FrupicDB(this);
		if (db.open()) {
			db.updateFlags(null, Frupic.FLAG_NEW, false);
			db.close();
		}

		repository.synchronizeAsync(0, FRUPICS_STEP);
	}

	private class ViewPagerAdapter extends FragmentPagerAdapter {

		ViewPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0: return getString(R.string.all_frupics);
				case 1: return getString(R.string.unseen);
				case 2: return getString(R.string.starred);
				default: return "Fragment " + position;
			}
		}

		@NotNull
		@Override
		public Fragment getItem(int position) {
			GridFragment f;
			Bundle args = new Bundle();
			switch (position) {
				case 0:
					f = new GridFragment();
					args.putInt("nav", 0);
					break;
				case 1:
					f = new GridFragment();
					args.putInt("nav", 1);
					break;
				case 2:
					f = new GridFragment();
					args.putInt("nav", 2);
					break;
				default:
					throw new IllegalArgumentException("Fragment " + position + " not supported");
			}
			f.setArguments(args);
			return f;
		}

		@Override
		public int getCount() {
			return 3;
		}
	}
}