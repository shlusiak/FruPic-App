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
import android.text.ClipboardManager;
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
import android.view.animation.Animation.AnimationListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
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
	ImageButton starButton, saveButton, clipboardButton;
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
        /* TODO: replace this by actionBar awesomeness. */
        controls = findViewById(R.id.all_controls);
        starButton = (ImageButton) findViewById(R.id.star);
        saveButton = (ImageButton) findViewById(R.id.save);
        clipboardButton = (ImageButton) findViewById(R.id.copy_to_clipboard);
        
        pager = (ViewPager) findViewById(R.id.viewPager);
        fadeAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeAnimation.setDuration(400);
        fadeAnimation.setStartOffset(2000);
        fadeAnimation.setFillAfter(true);
        fadeAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				if (!showFavs)
					starButton.setVisibility(View.VISIBLE);
		        saveButton.setVisibility(View.VISIBLE);
		        clipboardButton.setVisibility(View.VISIBLE);
				findViewById(R.id.url).setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				starButton.setVisibility(View.INVISIBLE);
				saveButton.setVisibility(View.INVISIBLE);
				clipboardButton.setVisibility(View.INVISIBLE);
				findViewById(R.id.url).setVisibility(View.INVISIBLE);
			}
		});

        
        
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
        	starButton.setVisibility(View.GONE);
        starButton.setOnClickListener(starClickedListener);
        findViewById(R.id.save).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showControls();
				startDownload();				
			}
		});
        findViewById(R.id.copy_to_clipboard).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showControls();
				Frupic frupic = getCurrentFrupic();
				
				ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(frupic.getUrl());				
				Toast.makeText(FruPicGallery.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();				
			}
		});
    }
    
    void showControls() {
    	controls.clearAnimation();
    	controls.startAnimation(fadeAnimation);
    }
    
    OnClickListener starClickedListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			showControls();
			Frupic frupic = getCurrentFrupic();
			
			frupic.setFlags((frupic.getFlags() ^ Frupic.FLAG_FAV) & ~Frupic.FLAG_NEW);
			db.setFlags(frupic);
			cursorChanged();
			
			starButton.setImageResource(frupic.hasFlag(Frupic.FLAG_FAV) ? R.drawable.star_label : R.drawable.star_empty);
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
		
		
		starButton.setImageResource(frupic.hasFlag(Frupic.FLAG_FAV) ? R.drawable.star_label : R.drawable.star_empty);
		
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
			req.setVisibleInDownloadsUi(true);
		
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				req.allowScanningByMediaScanner();
				req.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			} else
				req.setShowRunningNotification(true);
			
			req.setTitle(frupic.getFileName(false));
			req.setDescription("Frupic " + frupic.getId());
			req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, frupic.getFileName(false));
			dm.enqueue(req);
			
			Toast.makeText(this, getString(R.string.fetching_image_message, frupic.getFileName(false)), Toast.LENGTH_SHORT).show();
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