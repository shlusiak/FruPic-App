package de.saschahlusiak.frupic.gallery;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.detail.DetailDialog;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;

public class GalleryActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {
	private static final String tag = GalleryActivity.class.getSimpleName();
	
	private ViewPager pager;
	private GalleryAdapter adapter;
	private View controls;
	private Animation fadeAnimation;
	private Menu menu;
	
	private GalleryViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.gallery_activity);

		viewModel = new ViewModelProvider(this).get(GalleryViewModel.class);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        controls = findViewById(R.id.all_controls);

        pager = findViewById(R.id.viewPager);
        fadeAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeAnimation.setDuration(200);
        fadeAnimation.setFillAfter(true);
        fadeAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				findViewById(R.id.url).setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				/* hide URL view, because with alpha of 0, it's still clickable */
				findViewById(R.id.url).setVisibility(View.INVISIBLE);
			}
		});

        pager.addOnPageChangeListener(this);

		if (savedInstanceState == null) {
			viewModel.setStarred(getIntent().getBooleanExtra("starred", false));
			viewModel.setPosition(getIntent().getIntExtra("position", 0));
		}

		adapter = new GalleryAdapter(this, prefs.getBoolean("animatedgifs", true));
        pager.setAdapter(adapter);

		viewModel.getJobManager().observe(this, jobManager -> {
        	adapter.setJobManager(jobManager);
		});

        viewModel.getCursor().observe(this, cursor -> {
        	adapter.setCursor(cursor);
			pager.setCurrentItem(viewModel.getPosition(), false);
		});

        viewModel.getCurrentFrupic().observe(this, frupic -> {
        	updateLabels(frupic);
		});

        viewModel.getLastUpdated().observe(this, updated -> {
        	viewModel.reloadData();
		});
	}

    /**
     * toggles visibility of the controls
     */
    void toggleControls() {
    	if (getSupportActionBar().isShowing()) {
    		getSupportActionBar().hide();
    		controls.startAnimation(fadeAnimation);
    	} else {
    		getSupportActionBar().show();
    		controls.clearAnimation();
			findViewById(R.id.url).setVisibility(View.VISIBLE);
    	}
    }

    @Override
    protected void onDestroy() {
    	adapter.setJobManager(null);

    	super.onDestroy();
    }
	
	private void updateLabels(Frupic frupic) {
		String tags;
		/* TODO: Display information about unavailable frupic */
		
		TextView t = findViewById(R.id.url);
		t.setText(frupic.getUrl());
		t = findViewById(R.id.username);
		t.setText(getString(R.string.gallery_posted_by, frupic.getUsername()));
		
		getSupportActionBar().setTitle(String.format("#%d", frupic.id));
		
		if (menu != null) {
			MenuItem item = menu.findItem(R.id.star);
			item.setIcon(frupic.hasFlag(Frupic.FLAG_FAV) ? R.drawable.star_label : R.drawable.star_empty);
			item.setChecked(frupic.hasFlag(Frupic.FLAG_FAV));
		}
		
		t = findViewById(R.id.tags);
		tags = frupic.getTagsString();
		if (tags.isEmpty()) {
			t.setVisibility(View.INVISIBLE);
		} else {	
			t.setText(tags);
			t.setVisibility(View.VISIBLE);
		}
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gallery_optionsmenu, menu);
		this.menu = menu;
		
		return super.onCreateOptionsMenu(menu);
	}
	
	private void startDownload() {
		Frupic frupic = viewModel.getCurrentFrupic().getValue();
		if (frupic == null) return;
		
		/* Make sure, destination directory exists */
		Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();

		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		DownloadManager.Request req = new DownloadManager.Request(Uri.parse(frupic.getFullUrl()));
		req.setVisibleInDownloadsUi(true);
	
		req.allowScanningByMediaScanner();
		req.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		
		req.setTitle(frupic.getFileName());
		req.setDescription("Frupic " + frupic.id);
		req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, frupic.getFileName());
		dm.enqueue(req);
		
		Toast.makeText(this, getString(R.string.fetching_image_message, frupic.getFileName()), Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Frupic frupic;
		Intent intent;

		frupic = viewModel.getCurrentFrupic().getValue();
		if (frupic == null) return false;
		
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
			
		case R.id.openinbrowser:
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse(frupic.getUrl()));
			startActivity(intent);
			return true;

		case R.id.details:
			DetailDialog.create(this, viewModel.getManager(), frupic).show();
			return true;
			
		case R.id.download:
			startDownload();
			return true;

		case R.id.share_link:
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, frupic.getUrl());
			intent.putExtra(Intent.EXTRA_SUBJECT, "FruPic #" + frupic.id);
			startActivity(Intent.createChooser(intent, getString(R.string.share_link)));
			return true;
			
		case R.id.share_picture:
			File out = getExternalCacheDir();
			if (out == null) {
				Toast.makeText(this, R.string.error_no_storage, Toast.LENGTH_SHORT).show();
				return true;
			}
			/* The created external files are deleted in onDestroy() */

			/* TODO: If file is not in cache yet, download it first or show message */
			out = new File(out, frupic.getFileName());
			FrupicFactory factory = new FrupicFactory(this);
			if (factory.copyImageFile(frupic, out)) {
				intent = new Intent(Intent.ACTION_SEND);
				intent.setType("image/?");
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out));
				startActivity(Intent.createChooser(intent, getString(R.string.share_picture)));
			}
			return true;

		case R.id.star:
			viewModel.toggleFrupicStarred(frupic);
			updateLabels(frupic);
			return true;

		case R.id.copy_to_clipboard:
			ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(frupic.getUrl());
			Toast.makeText(GalleryActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
			return true;
			
		default:
			return true;
		}
	}
	
	@Override
	public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
		android.view.MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gallery_optionsmenu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Frupic frupic = viewModel.getCurrentFrupic().getValue();
		menu.setHeaderTitle("#" + frupic.id);
		menu.findItem(R.id.star).setChecked(frupic.hasFlag(Frupic.FLAG_FAV));
	}

	@Override
	public boolean onContextItemSelected(@NotNull android.view.MenuItem item) {
		return onOptionsItemSelected(item);
	}
	
	@Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		
    }

    @Override
    public void onPageSelected(int position) {
    	Log.i(tag, "onPageSelected(" + position + ")");
    	viewModel.setPosition(position);

		if (!getSupportActionBar().isShowing()) {
			toggleControls();
	    }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    	
    }
}