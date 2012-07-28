package de.saschahlusiak.frupic.gallery;

import java.io.File;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.detail.DetailDialog;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.utils.DownloadTask;
import de.saschahlusiak.frupic.utils.ProgressTaskActivityInterface;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
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
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class FruPicGallery extends Activity implements ViewPager.OnPageChangeListener {
	private static final String tag = FruPicGallery.class.getSimpleName();
	private static final int DIALOG_PROGRESS = 1;
	
	ViewPager pager;
	GalleryPagerAdapter adapter;
	FrupicFactory factory;
	public final int FRUPICS = 9;
	static DownloadTask downloadTask = null;
	ProgressDialog progressDialog;
	Cursor cursor;
	CheckBox star;
	FrupicDB db;
	boolean showFavs;
	View controls;
	Animation fadeAnimation;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        
        
        factory = new FrupicFactory(this, 8);
        factory.setTargetSize(display.getWidth(), display.getHeight());
        
        adapter = new GalleryPagerAdapter(this, factory);
        star = (CheckBox) findViewById(R.id.star);
        pager = (ViewPager) findViewById(R.id.viewPager);
        controls = findViewById(R.id.all_controls);
        fadeAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeAnimation.setDuration(400);
        fadeAnimation.setStartOffset(1500);
        fadeAnimation.setFillAfter(true);

        
        
        pager.setOnPageChangeListener(this);
        pager.setAdapter(adapter);
  
        db = new FrupicDB(this);
        db.open();
        
        if (savedInstanceState != null) {
        	showFavs = savedInstanceState.getBoolean("showFavs", false);
        } else {
        	showFavs = getIntent().getBooleanExtra("showFavs", false);
        }

        cursorChanged();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		factory.setCacheSize(Integer.parseInt(prefs.getString("cache_size", "16777216")));
        
        if (savedInstanceState != null) {
        	pager.setCurrentItem(savedInstanceState.getInt("position"));
        } else {
        	pager.setCurrentItem(getIntent().getIntExtra("position", 0));
        }
        updateLabels(getCurrentFrupic());
        
        /* TODO: changing the star currently changes the DB and the Cursor and thus disturbs the ViewPager.
         * Hide Star control for now */
        if (showFavs)
        	star.setVisibility(View.GONE);
        star.setOnCheckedChangeListener(starChangedListener);
        findViewById(R.id.save).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startDownload();				
			}
		});
    }
    
    void showControls() {
    	controls.clearAnimation();
    	controls.startAnimation(fadeAnimation);
    }
    
    OnCheckedChangeListener starChangedListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			showControls();
			Frupic frupic = getCurrentFrupic();
			if (frupic.hasFlag(Frupic.FLAG_FAV) == isChecked)
				return;
			
			frupic.setFlags((frupic.getFlags() ^ Frupic.FLAG_FAV) & ~Frupic.FLAG_NEW);
			db.setFlags(frupic);
			cursorChanged();
		}
	};
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putInt("position", cursor.getPosition());
        outState.putBoolean("showFavs", showFavs);

    }
    
    @Override
    protected void onDestroy() {
    	db.close();
    	super.onDestroy();
    }
    
	@Override
	protected void onStart() {
        if (downloadTask != null)
        	downloadTask.setActivity(this, downloadProgress);
		
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (downloadTask != null)
			downloadTask.setActivity(null, null);
		
		super.onStop();
	}
	
	void cursorChanged() {
		if (showFavs)
			cursor = db.getFavFrupics();
		else
			cursor = db.getFrupics(null);
		
		startManagingCursor(cursor);
		adapter.setCursor(cursor);
	}

	ProgressTaskActivityInterface downloadProgress = new ProgressTaskActivityInterface() {
		
		@Override
		public void updateProgressDialog(int progress, int max) {
			progressDialog.setMax(max);
			progressDialog.setProgress(progress);			
		}
		
		@Override
		public void success() {
			Toast.makeText(FruPicGallery.this,
				getString(R.string.frupic_downloaded_toast, downloadTask.getFrupic().getFileName(false)),
				Toast.LENGTH_SHORT).show();			
		}
		
		@Override
		public void dismiss() {
			dismissDialog(DIALOG_PROGRESS);
		}
	};

	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			progressDialog = new ProgressDialog(this);
			progressDialog.setTitle(getString(R.string.please_wait));
			progressDialog.setMessage(getString(R.string.fetching_image_message,
					downloadTask.getFrupic().getFileName(false)));
			progressDialog.setCancelable(true);
			progressDialog.setIndeterminate(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					downloadTask.cancel(true);
				}
			});
			return progressDialog;
		default:
			return super.onCreateDialog(id);
		}
	}
	
	void updateLabels(Frupic frupic) {
		String tags;
		/* TODO: Display information about unavailable frupic */
		
		TextView t = (TextView)findViewById(R.id.url);
		t.setText(frupic.getUrl());
		t = (TextView)findViewById(R.id.username);
		t.setText(getString(R.string.gallery_posted_by, frupic.getUsername()));
		
		star.setChecked(frupic.hasFlag(Frupic.FLAG_FAV));
		
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
		
		return super.onCreateOptionsMenu(menu);
	}
	
	void startDownload() {
		Frupic frupic = getCurrentFrupic();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			/* Make sure, destination directory exists */
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();

			DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			DownloadManager.Request req = new DownloadManager.Request(Uri.parse(frupic.getFullUrl()));
		
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				req.allowScanningByMediaScanner();
				req.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			}
			
			req.setTitle(frupic.getFileName(false));
			req.setDescription("Frupic " + frupic.getId());
			req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, frupic.getFileName(false));
			dm.enqueue(req);
		}else {
			downloadTask = new DownloadTask(frupic, factory);
			showDialog(DIALOG_PROGRESS);
			downloadTask.setActivity(this, downloadProgress);
			downloadTask.execute();
		}		
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
//			intent = new Intent(this, DetailsActivity.class);
//			intent.putExtra("frupic", frupic);
//			startActivity(intent);
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
			
		default:
			return true;
		}
	}
	
	@Override
	public void onCreateContextMenu(android.view.ContextMenu menu, View v,
			android.view.ContextMenu.ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gallery_optionsmenu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Frupic frupic = getCurrentFrupic();
		menu.setHeaderTitle("#" + frupic.getId());
	}
	
	Frupic getCurrentFrupic() {
		cursor.moveToPosition(pager.getCurrentItem());
		Frupic frupic = new Frupic(cursor);
		return frupic;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
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
		
		showControls();
	    updateLabels(frupic);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}