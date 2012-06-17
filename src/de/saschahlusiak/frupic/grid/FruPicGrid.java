package de.saschahlusiak.frupic.grid;

import java.io.File;
import java.net.UnknownHostException;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.detail.DetailDialog;
import de.saschahlusiak.frupic.gallery.FruPicGallery;
import de.saschahlusiak.frupic.model.*;
import de.saschahlusiak.frupic.preferences.FrupicPreferences;
import de.saschahlusiak.frupic.utils.DownloadTask;
import de.saschahlusiak.frupic.utils.ProgressTaskActivityInterface;
import de.saschahlusiak.frupic.utils.UploadActivity;
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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
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
	static DownloadTask downloadTask = null;
	Menu optionsMenu;
	View mRefreshIndeterminateProgressView;
	FrupicDB db;

	public final int FRUPICS_STEP = 60;

	RefreshIndexTask refreshTask = null; 
	
	class RefreshIndexTask extends AsyncTask<Void, Void, Frupic[]> {
		String error;
		int base, count;
		
		public RefreshIndexTask(int base, int count) {
			this.base = base;
			this.count = count;
		}

		@Override
		protected void onPreExecute() {
			Log.d(tag, "RefreshIndexTask.onPreExecute");
			error = null;
			setProgressActionView(true);
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
				e.printStackTrace();
				cancel(false);
			}

			return pics;
		}

		@Override
		protected void onCancelled() {
			if (error != null)
				Toast.makeText(FruPicGrid.this, error, Toast.LENGTH_LONG).show();
			setProgressActionView(false);
			super.onCancelled();
			refreshTask = null;
		}

		@Override
		protected void onPostExecute(Frupic result[]) {
			if (result != null) {
				db.addFrupics(result);
				cursorChanged();
			}
			setProgressActionView(false);

			super.onPostExecute(result);
			refreshTask = null;
		}
	}

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		}

		setContentView(R.layout.grid_activity);

		factory = new FrupicFactory(this, 300);

		db = new FrupicDB(this);
		db.open();
//		db.clearAll();
		
		adapter = new FruPicGridAdapter(this, factory);
		grid = (GridView) findViewById(R.id.gridView);
		grid.setAdapter(adapter);
		grid.setOnItemClickListener(this);
		grid.setOnScrollListener(this);
		registerForContextMenu(grid);				
		
		cursorChanged();
	}

	@Override
	protected void onDestroy() {
		/* delete all temporary external cache files created from "Share Image" */
		/* TODO: Make sure the cached files are gone for good! */
		db.markAllSeen();
		db.close();
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
		factory.updateCacheDirs();
		
		refreshTask = new RefreshIndexTask(0, FRUPICS_STEP); /* TODO: this might skip valuable frupics */
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
		
		if (downloadTask != null)
			downloadTask.setActivity(null, null);
		
		super.onStop();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		Intent intent = new Intent(this, FruPicGallery.class);
		intent.putExtra("position", position);
		startActivity(intent);
	}


	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (firstVisibleItem + visibleItemCount > adapter.getCount() - FRUPICS_STEP) {
			if (refreshTask == null) {
				refreshTask = new RefreshIndexTask(adapter.getCount(), FRUPICS_STEP);
				refreshTask.execute();
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int state) {
		if (state == SCROLL_STATE_IDLE) {
			int firstVisibleItem = grid.getFirstVisiblePosition();
			int visibleItemCount = grid.getLastVisiblePosition() - firstVisibleItem + 1;
			if (firstVisibleItem + visibleItemCount > adapter.getCount() - FRUPICS_STEP) {

				if (refreshTask != null)
					refreshTask.cancel(true);
				refreshTask = new RefreshIndexTask(adapter.getCount(), FRUPICS_STEP);
				refreshTask.execute();
			}
			
			Thread t = new Thread() {
				public void run() {					
					factory.pruneCache();
				};
			};
			t.start();
		}
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
		this.optionsMenu = menu;

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		switch (item.getItemId()) {
		case R.id.refresh:
			// adapter.setFrupics(null);
			// adapter.clearCache();

			refreshTask = new RefreshIndexTask(0, adapter.getCount());
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
		Frupic frupic = new Frupic((Cursor)adapter.getItem((int) info.position));

		menu.setHeaderTitle("#" + frupic.getId());
		menu.findItem(R.id.star).setChecked(frupic.hasFlag(Frupic.FLAG_FAV));
		// menu.findItem(R.id.cache_now).setEnabled(! new
		// File(factory.getCacheFileName(frupic, false)).exists());
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Frupic frupic = new Frupic((Cursor)adapter.getItem((int) info.position));
		Intent intent;

		switch (item.getItemId()) {
		case R.id.star:
			frupic.setFlags(frupic.getFlags() ^ Frupic.FLAG_FAV);
			db.setFlags(frupic);
			cursorChanged();
			return true;
		case R.id.openinbrowser:
			intent = new Intent("android.intent.action.VIEW", Uri.parse(frupic.getUrl()));
			startActivity(intent);
			return true;

		case R.id.details:
//			intent = new Intent(this, DetailsActivity.class);
//			intent.putExtra("frupic", frupic);
//			startActivity(intent);
			DetailDialog.create(this, frupic, factory).show();
			
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
			if (FrupicFactory.copyImageFile(frupic.getCachedFile(factory, false), out)) {
				intent = new Intent(Intent.ACTION_SEND);
				intent.setType("image/?");
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out));
				startActivity(Intent.createChooser(intent,
						getString(R.string.share_picture)));
			}

			return true;

		case R.id.cache_now:
			downloadTask = new DownloadTask(frupic, factory);
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
	
	void setProgressActionView(boolean refreshing) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			setProgressBarIndeterminateVisibility(refreshing);			
			return;
		}
		if (optionsMenu == null)
			return;
        final MenuItem refreshItem = optionsMenu.findItem(R.id.refresh);
        if (refreshItem != null) {
            if (refreshing) {
                if (mRefreshIndeterminateProgressView == null) {
                    LayoutInflater inflater = (LayoutInflater)
                            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    mRefreshIndeterminateProgressView = inflater.inflate(
                            R.layout.actionbar_indeterminate_progress, null);
                }

                refreshItem.setActionView(mRefreshIndeterminateProgressView);
            } else {
                refreshItem.setActionView(null);
            }
        }
	}
	
	void cursorChanged() {
		Cursor cursor = db.getFrupics(null);
		adapter.changeCursor(cursor);
	}
}