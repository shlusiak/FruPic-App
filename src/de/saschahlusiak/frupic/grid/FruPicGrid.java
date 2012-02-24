package de.saschahlusiak.frupic.grid;

import java.io.File;
import java.net.UnknownHostException;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.gallery.FruPicGallery;
import de.saschahlusiak.frupic.model.*;
import de.saschahlusiak.frupic.preferences.FrupicPreferences;
import de.saschahlusiak.frupic.utils.DetailsActivity;
import de.saschahlusiak.frupic.utils.DownloadTask;
import de.saschahlusiak.frupic.utils.ProgressTaskActivityInterface;
import de.saschahlusiak.frupic.utils.UploadActivity;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class FruPicGrid extends Activity implements OnItemClickListener, OnScrollListener {
	static private final String tag = FruPicGrid.class.getSimpleName();
	static private final int REQUEST_PICK_PICTURE = 1;
	static private final int DIALOG_PROGRESS = 1;

	GridView grid;
	FruPicGridAdapter adapter;
	FrupicFactory factory;
	ProgressDialog progressDialog;
	int base, count;
	int lastVisibleStart, lastVisibleCount;
	static DownloadTask downloadTask = null;

	public final int FRUPICS = 20;
	public final int FRUPICS_STEP = 20;

	RefreshIndexTask refreshTask = null; 
	FetchPreviewTask fetchTask = null;
	
	/* TODO: Add a star to new and unseen frupics? */
	class RefreshIndexTask extends AsyncTask<Void, Void, Frupic[]> {
		String error;

		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			Log.d(tag, "RefreshIndexTask.onPreExecute");
			error = null;
			super.onPreExecute();
		}

		@Override
		protected Frupic[] doInBackground(Void... arg0) {
			Frupic pics[];
			try {
				pics = factory.fetchFrupicIndex(null, base, count);
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
			if (error != null)
				Toast.makeText(FruPicGrid.this, error, Toast.LENGTH_LONG).show();
			if (fetchTask == null)
				setProgressBarIndeterminateVisibility(false);
			super.onCancelled();
			refreshTask = null;
		}

		@Override
		protected void onPostExecute(Frupic result[]) {
			if (result != null)
				adapter.setFrupics(result);
				
			int visibleItemCount = grid.getLastVisiblePosition() - grid.getFirstVisiblePosition();
			int firstVisibleItem = grid.getFirstVisiblePosition();
			
			Log.d(tag, "RefreshIndexTask.onPostExecute");
			if (visibleItemCount > 0) {
				/* Base changed, start a new FetchTask */
				if (fetchTask != null) {
					Log.d(tag, "Got FruPic list, restart FetchTask");
					fetchTask.cancel(false);
				}
				
				lastVisibleCount = visibleItemCount;
				lastVisibleStart = firstVisibleItem;			
				
				Frupic v[] = new Frupic[visibleItemCount];
				for (int i = 0; i < visibleItemCount; i++)
					v[i] = adapter.getItem(firstVisibleItem + i);
				fetchTask = new FetchPreviewTask();
				fetchTask.execute(v);
			}
			if (fetchTask == null)
				setProgressBarIndeterminateVisibility(false);
			
			super.onPostExecute(result);
			refreshTask = null;
		}
	}

	/* TODO: Add a progressbar to grid items that are being loading right now? */
	class FetchPreviewTask extends AsyncTask<Frupic, Frupic, Void> {
		FetchPreviewTask() {
		}

		@Override
		protected void onPreExecute() {
			Log.d(tag, "FetchPreviewTask.onPreExecute");
			setProgressBarIndeterminateVisibility(true);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Frupic... frupics) {
			factory.pruneCache();
			if (isCancelled())
				return null;

			for (Frupic f : frupics) {
				if (f == null)
					return null; /* TODO: Find out why this might happen */
				
				if (factory.fetchThumb(f))
					publishProgress(f);
				
				if (isCancelled())
					return null;
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Frupic... values) {
			adapter.notifyDataSetChanged();
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onCancelled() {
			if (refreshTask == null)
				setProgressBarIndeterminateVisibility(false);

			fetchTask = null;
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Void result) {
			Log.d(tag, "FetchPreviewTask.onPostExecute");
			if (refreshTask == null)
				setProgressBarIndeterminateVisibility(false);
			fetchTask = null;
			super.onPostExecute(result);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Frupic frupics[];
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.grid_activity);

		factory = new FrupicFactory(this, 45);

		adapter = new FruPicGridAdapter(this, factory);
		grid = (GridView) findViewById(R.id.gridView);
		grid.setAdapter(adapter);
		grid.setOnItemClickListener(this);
		grid.setOnScrollListener(this);
		registerForContextMenu(grid);
		
		frupics = factory.fetchFrupicIndexFromCache();
		adapter.setFrupics(frupics);
		if (fetchTask == null && frupics != null) {
			/* this loads all frupics in the index */
			/* TODO: maybe find out exactly how many to load */
			fetchTask = new FetchPreviewTask();
			fetchTask.execute(frupics);
		}
		base = 0;
		count = FRUPICS;
	}

	@Override
	protected void onDestroy() {
		/* delete all temporary external cache files created from "Share Image" */
		File dir = getExternalCacheDir();
		if (dir != null) {
			String files[] = dir.list();
			for (String file : files) {
				if (new File(dir, file).delete())
					Log.d(tag, "deleted cache file " + file);
				else
					Log.w(tag, "unable to delete cache file " + file);
			}
		}

		super.onDestroy();
	}

	ProgressTaskActivityInterface downloadProgress = new ProgressTaskActivityInterface() {
		
		@Override
		public void updateProgressDialog(int progress, int max) {
			progressDialog.setMax(max);
			progressDialog.setProgress(progress);			
		}
		
		@Override
		public void success() {
			Toast.makeText(FruPicGrid.this,
					getString(R.string.frupic_downloaded_toast, downloadTask.getFrupic().getFileName(false)),
					Toast.LENGTH_SHORT).show();			
		}
		
		@Override
		public void dismiss() {
			dismissDialog(DIALOG_PROGRESS);
		}
	};
	
	@Override
	protected void onStart() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		factory.setCacheSize(Integer.parseInt(prefs.getString("cache_size",
				"16777216")));
		refreshTask = new RefreshIndexTask();
		refreshTask.execute();
		
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

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {
		Intent intent = new Intent(this, FruPicGallery.class);
		intent.putExtra("index", position);
		intent.putExtra("id", id);		
		startActivity(intent);
	}


	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (firstVisibleItem + visibleItemCount > count - FRUPICS_STEP) {
			count += FRUPICS_STEP;
			if (refreshTask != null)
				refreshTask.cancel(true);
			refreshTask = new RefreshIndexTask();
			refreshTask.execute();
			return;
		}

		if (visibleItemCount <= 0)
			return;

		if (lastVisibleCount != visibleItemCount || lastVisibleStart != firstVisibleItem) {
			Frupic v[] = new Frupic[visibleItemCount];
			for (int i = 0; i < visibleItemCount; i++)
				v[i] = adapter.getItem(firstVisibleItem + i);
			if (fetchTask != null) {
				Log.d(tag, "Cancelling running FetchTask");
				fetchTask.cancel(false);
				fetchTask = null;
			}
			if (fetchTask == null) {
				fetchTask = new FetchPreviewTask();
				fetchTask.execute(v);
				lastVisibleCount = visibleItemCount;
				lastVisibleStart = firstVisibleItem;			
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView arg0, int arg1) {

	}
	
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
		case R.id.refresh:
			// adapter.setFrupics(null);
			// adapter.clearCache();

			refreshTask = new RefreshIndexTask();
			refreshTask.execute();
			return true;

		case R.id.upload:
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(Intent.createChooser(intent,
					getString(R.string.upload)), REQUEST_PICK_PICTURE);
			return true;

		case R.id.gotowebsite:
			intent = new Intent("android.intent.action.VIEW", Uri
					.parse("http://frupic.frubar.net"));
			startActivity(intent);
			return true;

		case R.id.preferences:
			intent = new Intent(this, FrupicPreferences.class);
			startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(android.view.ContextMenu menu, View v,
			android.view.ContextMenu.ContextMenuInfo menuInfo) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contextmenu, menu);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Frupic frupic = (Frupic) adapter.getItem((int) info.position);

		menu.setHeaderTitle("#" + frupic.getId());
		// menu.findItem(R.id.cache_now).setEnabled(! new
		// File(factory.getCacheFileName(frupic, false)).exists());
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Frupic frupic;
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		frupic = (Frupic) adapter.getItem((int) info.position);
		Intent intent;

		if (frupic == null)
			return false;

		switch (item.getItemId()) {
		case R.id.openinbrowser:
			intent = new Intent("android.intent.action.VIEW", Uri.parse(frupic.getUrl()));
			startActivity(intent);
			return true;

		case R.id.details:
			intent = new Intent(this, DetailsActivity.class);
			intent.putExtra("frupic", frupic);
			startActivity(intent);
			return true;

		case R.id.share_link:
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, frupic.getUrl());
			intent.putExtra(Intent.EXTRA_SUBJECT, "FruPic #" + frupic.getId());
			startActivity(Intent.createChooser(intent,
					getString(R.string.share_link)));
			return true;

		case R.id.share_picture:
			File out = getExternalCacheDir();
			if (out == null) {
				Toast.makeText(this, R.string.error_no_storage, Toast.LENGTH_SHORT).show();
				return true;
			}
			/* The created external files are deleted somewhen in onDestroy() */

			/* TODO: If file is not in cache yet, download it first or show message */
			out = new File(out, frupic.getFileName(false));
			if (FrupicFactory.copyImageFile(frupic.getCachedFile(this), out)) {
				intent = new Intent(Intent.ACTION_SEND);
				intent.setType("image/?");
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out));
				startActivity(Intent.createChooser(intent,
						getString(R.string.share_picture)));
			}

			return true;

		case R.id.cache_now:
			downloadTask = new DownloadTask(frupic);
			showDialog(DIALOG_PROGRESS);
			downloadTask.setActivity(this, downloadProgress);
			downloadTask.execute();

			return true;

		default:
			return super.onContextItemSelected(item);
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

}