package de.saschahlusiak.frupic.gallery;

import java.io.File;
import java.net.UnknownHostException;

import de.saschahlusiak.frupic.R;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

public class FruPicGallery extends Activity implements OnItemSelectedListener {
	private static final String tag = FruPicGallery.class.getSimpleName();
	private static final int DIALOG_PROGRESS = 1;
	
	OneFlingScrollGallery gallery;
	FruPicGalleryAdapter adapter;
	FrupicFactory factory;
	ProgressBar progressBar;
	ProgressBar progressActivity;
	int base;
	boolean prefetch_images;
	public final int FRUPICS = 9;
	static DownloadTask downloadTask = null;
	ProgressDialog progressDialog;
	
	
	class RefreshTask extends AsyncTask<Integer, Void, Frupic[]> {
		String error;
		int newbase;

		@Override
		protected void onPreExecute() {
			progressActivity.setVisibility(View.VISIBLE);
			error = null;
			Log.d(tag, "RefreshTask.onPreExecute");
			super.onPreExecute();
		}

		@Override
		protected Frupic[] doInBackground(Integer... args) {
			Frupic pics[];
			newbase = args[0];
			
			if (newbase < 0)
				newbase = 0;
			
			try {
				pics = factory.fetchFrupicIndex(null, newbase, args[1]);
			} catch (UnknownHostException u) {
				pics = null;
				error = "Unknown host";
				cancel(false);
			} catch (Exception e) {
				pics = null;
				error = "Connection error";
				cancel(false);
			}

			return pics;
		}

		@Override
		protected void onCancelled() {
			progressActivity.setVisibility(View.INVISIBLE);
			if (error != null)
				Toast.makeText(FruPicGallery.this, error, Toast.LENGTH_LONG).show();
			Log.d(tag, "RefreshTask.onCancelled");
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Frupic result[]) {
			progressActivity.setVisibility(View.INVISIBLE);
			Log.d(tag, "RefreshTask.onPostExecute");
			
			if (result != null) {
				long id = gallery.getSelectedItemId();
				if (base != newbase) {
					adapter.setFrupics(result);
					base = newbase;
					for (int i = 0; i < result.length; i++) {
						if (result[i].getId() == id) {
							gallery.setSelection(i, false);
							break;
						}
					}
				}
			}
			super.onPostExecute(result);
		}
	}

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
				
				if (factory.fetchFull(frupics[i], new FrupicFactory.OnFetchProgress() {
					
					@Override
					public void OnProgress(int read, int length) {
						if (!isCancelled())
							publishProgress(0, index, frupics.length, read, length, frupics[index].getId());
					}
				})) {
					publishProgress(1, index, frupics.length, 1, 1, frupics[index].getId());
				} else {
					Log.i(tag, "(fetchFull returned false)");
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
			if ((values[0] == 1) && (values[5] == gallery.getSelectedItemId())) {
				adapter.notifyDataSetChanged();
				Log.i(tag, "FetchTask::notifyDataSetChanged");
			}
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
	
	RefreshTask refreshTask;
	FetchTask fetchTask;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_activity);
        
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressActivity = (ProgressBar) findViewById(R.id.progressActivity);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        
        factory = new FrupicFactory(this, 8);
        factory.setTargetSize(display.getWidth(), display.getHeight());
        
        adapter = new FruPicGalleryAdapter(this, factory);
        gallery = (OneFlingScrollGallery) findViewById(R.id.gallery);
        gallery.setAdapter(adapter);
 
  
        Frupic frupics[] = factory.fetchFrupicIndexFromCache();
        adapter.setFrupics(frupics);
        
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		factory.setCacheSize(Integer.parseInt(prefs.getString("cache_size", "16777216")));
		prefetch_images = prefs.getBoolean("preload", true);

		/* This ensures that subsequent calls to gallery.setSelection() result in onItemSelected being called, which
		 * it self runs FetchTask and RefreshTask
		 */
		gallery.setOnItemSelectedListener(this);
		gallery.setCallbackDuringFling(true);
		gallery.setAnimationDuration(300);
		
		registerForContextMenu(gallery);
        
        if (savedInstanceState != null) {
        	base = savedInstanceState.getInt("base");
        	gallery.setSelection(savedInstanceState.getInt("position"), false);
        	Log.d(tag, "onCreate, base=" + base + ", position=" + savedInstanceState.getInt("position"));
        } else {
        	/* This makes the picture available but it does not have the right "range".
        	 * A RefreshTask is needed anyway to corrent this
        	 */
        	int index = getIntent().getIntExtra("index", 0);
        	base = 0;
        	gallery.setSelection(index, false);
        }
        updateLabels();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putInt("position", gallery.getSelectedItemPosition());
    	outState.putInt("base", base);
    }
    
	@Override
	protected void onStart() {      
        if (downloadTask != null)
        	downloadTask.setActivity(this, downloadProgress);
		
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (refreshTask != null && !refreshTask.isCancelled())
			refreshTask.cancel(true);
		refreshTask = null;
		
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
		Frupic frupic = (Frupic)gallery.getSelectedItem();
		/* TODO: Display information about unavailable frupic */
		if (frupic == null) 
			return;
		
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
    
	@Override
	public void onItemSelected(AdapterView<?> adapterview, View view, int position,	long id) {
		Log.d(tag, "position " + position + ", base " + base);
		
		updateLabels();
		
		if (position <= 0 || position >= FRUPICS - 1) {
			if (refreshTask != null && !refreshTask.isCancelled()) {
				refreshTask.cancel(true);
			}
	        refreshTask = new RefreshTask();
	        refreshTask.execute(base + position - FRUPICS / 2, FRUPICS);
		}
		
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
		
		if (prefetch_images)
			fetchTask.execute(adapter.getItem(position), adapter.getItem(position + 1), adapter.getItem(position - 1), adapter.getItem(position + 2));
		else
			fetchTask.execute(adapter.getItem(position));				
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gallery_optionsmenu, menu);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Frupic frupic = (Frupic) gallery.getSelectedItem();
		Intent intent;
		
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
			if (FrupicFactory.copyImageFile(frupic.getCachedFile(factory), out)) {
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
		Frupic frupic = (Frupic) adapter.getItem((int) info.position);

		menu.setHeaderTitle("#" + frupic.getId());
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return onOptionsItemSelected(item);
	}	
}