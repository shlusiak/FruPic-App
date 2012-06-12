package de.saschahlusiak.frupic.utils;

import java.io.File;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;

public class DownloadTask extends AsyncTask<Void, Integer, Void> {
	String filename;
	Context context;
	ProgressTaskActivityInterface activity;
	String error = null;
	Frupic frupic;
	FrupicFactory factory;

	public DownloadTask(Frupic frupic, FrupicFactory factory) {
		this.frupic = frupic;
		this.factory = factory;
		filename = frupic.getFileName(false);
	}
	
	public void setActivity(Context context, ProgressTaskActivityInterface activity) {
		this.context = context;
		this.activity = activity;
	}
	
	public Frupic getFrupic() {
		return frupic;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		if (activity != null)
			activity.updateProgressDialog(values[0], values[1]);
	}

	@Override
	protected Void doInBackground(Void... params) {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other
			// states, but all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		if (!mExternalStorageAvailable || !mExternalStorageWriteable) {
			cancel(false);
			if (context != null)
				error = context.getString(R.string.error_no_storage);
			return null;
		}		
		
		if (!frupic.getCachedFile(factory).exists()) {
			if (!factory.fetchFrupicImage(frupic, false,
					new FrupicFactory.OnFetchProgress() {
						@Override
						public void OnProgress(int read, int length) {
							publishProgress(read, length);
						}
					})) 
			{
				if (context != null)
				error = context.getString(R.string.error_fetching);
				cancel(false);
				return null;
			}
		}
			
			
		File path = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		File file = new File(path, frupic.getFileName(false));
		path.mkdirs();

		if (FrupicFactory.copyImageFile(frupic.getCachedFile(factory),
				file)) {
			// Tell the media scanner about the new file so that it is
			// immediately available to the user.
			MediaScannerConnection
					.scanFile(
							context,
							new String[] { file.toString() },
							null,
							new MediaScannerConnection.OnScanCompletedListener() {
								public void onScanCompleted(
										String path, Uri uri) {
									Log.i("ExternalStorage", "Scanned "
											+ path + ":");
									Log.i("ExternalStorage", "-> uri="
											+ uri);
								}
							});
		} else {
			if (context != null)
				error = context.getString(R.string.error_saving);
			cancel(false);
		}
		

		return null;
	}

	protected void onCancelled() {
		if (activity != null)
			activity.dismiss();
		if ((error != null) && (context != null))
			Toast.makeText(context, error, Toast.LENGTH_LONG).show();
	}

	protected void onPostExecute(Void result) {
		if (activity != null) {
			activity.dismiss();
			activity.success();
		}
	}

}
