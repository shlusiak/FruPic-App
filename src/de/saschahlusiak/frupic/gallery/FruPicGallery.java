package de.saschahlusiak.frupic.gallery;

import java.io.File;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.detail.DetailDialog;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.DownloadManager.Request;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class FruPicGallery extends Activity implements ViewPager.OnPageChangeListener {
	private static final String tag = FruPicGallery.class.getSimpleName();
	
	ViewPager pager;
	GalleryPagerAdapter adapter;
	FrupicFactory factory;
	ProgressDialog progressDialog;
	Cursor cursor;
	FrupicDB db;
	int navIndex;
	View controls;
	Animation fadeAnimation;
	Menu menu;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       	requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        
        setContentView(R.layout.gallery_activity);
        getActionBar().setDisplayShowHomeEnabled(false);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
        
        factory = new FrupicFactory(this, 8);
        factory.setTargetSize(display.getWidth(), display.getHeight());
        
        adapter = new GalleryPagerAdapter(this, factory, prefs.getBoolean("animatedgifs", true));
        
        controls = findViewById(R.id.all_controls);
        
        pager = (ViewPager) findViewById(R.id.viewPager);
        fadeAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeAnimation.setDuration(400);
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

        
        
        pager.setOnPageChangeListener(this);
  
        db = new FrupicDB(this);
        db.open();
        
        if (savedInstanceState != null) {
        	navIndex = savedInstanceState.getInt("navIndex", 0);
        } else {
        	navIndex = getIntent().getIntExtra("navIndex", 0);
        }

        cursorChanged();
        pager.setAdapter(adapter);


		factory.setCacheSize(Integer.parseInt(prefs.getString("cache_size", "16777216")));
        
        if (savedInstanceState != null) {
        	pager.setCurrentItem(savedInstanceState.getInt("position"));
        } else {
        	pager.setCurrentItem(getIntent().getIntExtra("position", 0));
        }
        updateLabels(getCurrentFrupic());
        
        
        pager.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (getActionBar().isShowing())
					toggleControls();
			}
		}, 2000);

        
        /* TODO: changing the star currently changes the DB and the Cursor and thus disturbs the ViewPager.
         * we have to disable the star menu item, wen showFavs is true */
    }
    
    /**
     * toggles visibility of the controls
     */
    void toggleControls() {
    	if (getActionBar().isShowing()) {
    		getActionBar().hide();
    		controls.startAnimation(fadeAnimation);
    	} else {
    		getActionBar().show();
    		controls.clearAnimation();
			findViewById(R.id.url).setVisibility(View.VISIBLE);
    	}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putInt("position", cursor.getPosition());
        outState.putInt("navIndex", navIndex);
    }
    
    @Override
    protected void onDestroy() {
    	db.close();
    	super.onDestroy();
    }
	
	void cursorChanged() {
		int mask = 0;
		if (navIndex == 2)
			mask |= Frupic.FLAG_FAV;
		if (navIndex == 1)
			mask |= Frupic.FLAG_NEW;
		cursor = db.getFrupics(null, mask);
		
		startManagingCursor(cursor);
		adapter.setCursor(cursor);
	}
	
	void updateLabels(Frupic frupic) {
		String tags;
		/* TODO: Display information about unavailable frupic */
		
		TextView t = (TextView)findViewById(R.id.url);
		t.setText(frupic.getUrl());
		t = (TextView)findViewById(R.id.username);
		t.setText(getString(R.string.gallery_posted_by, frupic.getUsername()));
		
		getActionBar().setTitle(String.format("#%d", frupic.getId()));
		
		if (menu != null) {
			MenuItem item = menu.findItem(R.id.star);
			item.setIcon(frupic.hasFlag(Frupic.FLAG_FAV) ? R.drawable.star_label : R.drawable.star_empty);
			item.setChecked(frupic.hasFlag(Frupic.FLAG_FAV));
		}
		
		t = (TextView)findViewById(R.id.tags);
		tags = frupic.getTagsString();
		if (tags == null) {
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
		
		if (navIndex == 2) /* FIXME */
			menu.findItem(R.id.star).setVisible(false);
		
		updateLabels(getCurrentFrupic());
		
		return super.onCreateOptionsMenu(menu);
	}
	
	void startDownload() {
		Frupic frupic = getCurrentFrupic();
		
		/* Make sure, destination directory exists */
		Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();

		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		DownloadManager.Request req = new DownloadManager.Request(Uri.parse(frupic.getFullUrl()));
		req.setVisibleInDownloadsUi(true);
	
		req.allowScanningByMediaScanner();
		req.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		
		req.setTitle(frupic.getFileName(false));
		req.setDescription("Frupic " + frupic.getId());
		req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, frupic.getFileName(false));
		dm.enqueue(req);
		
		Toast.makeText(this, getString(R.string.fetching_image_message, frupic.getFileName(false)), Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Frupic frupic;
		Intent intent;

		frupic = getCurrentFrupic();
		
		switch (item.getItemId()) {
		case R.id.openinbrowser:
			intent = new Intent("android.intent.action.VIEW", Uri.parse(frupic
					.getUrl()));
			startActivity(intent);
			return true;

		case R.id.details:
			DetailDialog.create(this, frupic, factory).show();
			return true;
			
		case R.id.cache_now:
			startDownload();
			return true;

		case R.id.share_link:
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, frupic.getUrl());
			intent.putExtra(Intent.EXTRA_SUBJECT, "FruPic #" + frupic.getId());
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
			out = new File(out, frupic.getFileName(false));
			if (FrupicFactory.copyImageFile(frupic.getCachedFile(factory, false), out)) {
				intent = new Intent(Intent.ACTION_SEND);
				intent.setType("image/?");
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out));
				startActivity(Intent.createChooser(intent, getString(R.string.share_picture)));
			}
			return true;

		case R.id.star:
			frupic.setFlags((frupic.getFlags() ^ Frupic.FLAG_FAV) & ~Frupic.FLAG_NEW);
			db.setFlags(frupic);
			cursorChanged();
			updateLabels(frupic);
			return true;

		case R.id.copy_to_clipboard:
			ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(frupic.getUrl());
			Toast.makeText(FruPicGallery.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
			return true;
			
		default:
			return true;
		}
	}
	
	@Override
	public void onCreateContextMenu(android.view.ContextMenu menu, View v,
			android.view.ContextMenu.ContextMenuInfo menuInfo) {
		android.view.MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gallery_optionsmenu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Frupic frupic = getCurrentFrupic();
		menu.setHeaderTitle("#" + frupic.getId());
		menu.findItem(R.id.star).setChecked(frupic.hasFlag(Frupic.FLAG_FAV));
		if (navIndex == 2) /* FIXME, we would like to be able to star/unstar all the time */
			menu.findItem(R.id.star).setVisible(false);
	}
	
	Frupic getCurrentFrupic() {
		cursor.moveToPosition(pager.getCurrentItem());
		Frupic frupic = new Frupic(cursor);
		return frupic;
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		return onOptionsItemSelected(item);
	}
	
	@Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		
    }

    @Override
    public void onPageSelected(int position) {
    	Log.i(tag, "onPageSelected(" + position + ")");
	    Frupic frupic;
		frupic = getCurrentFrupic();
		if (frupic.hasFlag(Frupic.FLAG_NEW)) {
			frupic.setFlags(frupic.getFlags() & ~Frupic.FLAG_NEW);
			db.setFlags(frupic);
		}

		if (!getActionBar().isShowing()) {
			toggleControls();
	        pager.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (getActionBar().isShowing())
						toggleControls();
				}
			}, 2000);
	    }
	    updateLabels(frupic);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}