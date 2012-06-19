package de.saschahlusiak.frupic.gallery;

import java.io.File;
import java.net.UnknownHostException;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.detail.DetailDialog;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.utils.DownloadTask;
import de.saschahlusiak.frupic.utils.ProgressTaskActivityInterface;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;

public class FruPicGallery extends Activity implements ViewPager.OnPageChangeListener {
	private static final String tag = FruPicGallery.class.getSimpleName();
	private static final int DIALOG_PROGRESS = 1;
	
	ViewPager pager;
	GalleryPagerAdapter adapter;
	FrupicFactory factory;
	ProgressBar progressBar;
	ProgressBar progressActivity;
	boolean prefetch_images;
	public final int FRUPICS = 9;
	static DownloadTask downloadTask = null;
	ProgressDialog progressDialog;
	Cursor cursor;
	FrupicDB db;
	
	
	class FetchTask extends AsyncTask<Frupic, Integer, Void> {
		@Override
		protected void onPreExecute() {
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setMax(100);
			Log.i(tag, "FetchTask.onPreExecute");
			super.onPreExecute();
		}
		
		@Override
		protected Void doInBackground(final Frupic... frupics) {
			factory.pruneCache();
			if (isCancelled())
				return null;

			for (int i = 0; i < frupics.length; i++) {
				if (frupics[i] == null)
					continue;
				final int index = i;
				int ret;
				ret = factory.fetchFull(frupics[i], new FrupicFactory.OnFetchProgress() {
					
					@Override
					public void OnProgress(int read, int length) {
						if (!isCancelled())
							publishProgress(0, index, frupics.length, read, length, frupics[index].getId());
					}
				});
				switch (ret) {
				case FrupicFactory.NOT_AVAILABLE:
					Log.i(tag, "(fetchFull returned false)");
					break;
				case FrupicFactory.FROM_CACHE:
					break;
				case FrupicFactory.FROM_FILE:
				case FrupicFactory.FROM_WEB:
					publishProgress(1, index, frupics.length, 1, 1, frupics[index].getId());
					break;
				}				
				if (isCancelled())
					return null;
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			/* first value indicates if download is complete (>0) or pending (==0) */
			int p = (int)((float)values[1] / (float)values[2] * 100.0f);
			int sp = p + (int)((float)values[3] / (float)values[4] / (float)values[2] * 100.0f);
			
			progressBar.setProgress(p);
			progressBar.setSecondaryProgress(sp);

			/* XXX: This dataSetChanged() sometimes screws up fling of the gallery */
			/* only update on first item, which is the currently visible one */
//			if ((values[0] == 1) && (values[5] == gallery.getSelectedItemId())) {
//				adapter.notifyDataSetChanged();
//				Log.i(tag, "FetchTask::notifyDataSetChanged");
//			}
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onCancelled() {
			Log.i(tag, "FetchTask.onCancelled");
			super.onCancelled();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			Log.i(tag, "FetchTask.onPostExecute");
			progressBar.setVisibility(View.INVISIBLE);			
			super.onPostExecute(result);
		}
	}
	
	FetchTask fetchTask;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        
        factory = new FrupicFactory(this, 8);
        factory.setTargetSize(display.getWidth(), display.getHeight());
        
        adapter = new GalleryPagerAdapter(this, factory);
        pager = (ViewPager) findViewById(R.id.viewPager);
        pager.setOnPageChangeListener(this);
        pager.setAdapter(adapter);
 
 
        /* TODO: FIXME */
        db = new FrupicDB(this);
        db.open();
        
        cursor = db.getFrupics(null);
		startManagingCursor(cursor);
		adapter.setCursor(cursor);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		factory.setCacheSize(Integer.parseInt(prefs.getString("cache_size", "16777216")));
		prefetch_images = prefs.getBoolean("preload", true);
		
		registerForContextMenu(pager);
        
        if (savedInstanceState != null) {
        	pager.setCurrentItem(savedInstanceState.getInt("position"));
        	Log.d(tag, "onCreate, position=" + savedInstanceState.getInt("position"));
        } else {
        	pager.setCurrentItem(getIntent().getIntExtra("position", 0));
        }
        updateLabels();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putInt("position", cursor.getPosition());
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
		if (fetchTask != null && !fetchTask.isCancelled())
			fetchTask.cancel(true);
		fetchTask = null;
		
		if (downloadTask != null)
			downloadTask.setActivity(null, null);
		
		super.onStop();
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
	
	void updateLabels() {
		String tags;
		Frupic frupic;
		frupic = getCurrentFrupic();
		/* TODO: Display information about unavailable frupic */
		
		TextView t = (TextView)findViewById(R.id.url);
		t.setText(frupic.getUrl());
		t = (TextView)findViewById(R.id.username);
		t.setText(getString(R.string.gallery_posted_by, frupic.getUsername()));
		
		t = (TextView)findViewById(R.id.tags);
		tags = frupic.getTagsString();
		if (tags == null) {
			t.setVisibility(View.INVISIBLE);
		} else {	
			t.setText(tags);
			t.setVisibility(View.VISIBLE);
		}
	}
    
	public void onItemSelected(AdapterView<?> adapterview, View view, int position,	long id) {
		Log.d(tag, "position " + position);
		
		updateLabels();
		
		/* XXX: The View might be created with a dummy image, waiting for the task to fetch it's first
		 * image, on which the adapter will be notified of the change and the view will be updated.
		 * 
		 * There is a window when the job finishes, puts the file to cache but being cancelled before the adapter is triggered.
		 * The next job will assume the image existed before and that the view used it successfully, thus not triggering
		 * the adapter to not screw up the gallery. Result is the dummy image still showing till the view is recreated. 
		 */
		if (fetchTask != null) {
			fetchTask.cancel(true);
		}
		fetchTask = new FetchTask();
		
		/*
		if (prefetch_images)
			fetchTask.execute(adapter.getFrupic(position), adapter.getFrupic(position + 1), adapter.getFrupic(position - 1), adapter.getFrupic(position + 2));
		else
			fetchTask.execute(adapter.getFrupic(position)); */				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gallery_optionsmenu, menu);
		
		return super.onCreateOptionsMenu(menu);
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
			downloadTask = new DownloadTask(frupic, factory);
			showDialog(DIALOG_PROGRESS);
			downloadTask.setActivity(this, downloadProgress);
			downloadTask.execute();
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
	}
	
	Frupic getCurrentFrupic() {
		cursor.moveToPosition(pager.getCurrentItem());
		return new Frupic(cursor);
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
       updateLabels();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}