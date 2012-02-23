package de.saschahlusiak.frupic.utils;

import android.content.Context;
import android.os.AsyncTask;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;

public class FetchTask extends AsyncTask<Void, Integer, Void> {
	String filename;
	ProgressTaskActivityInterface activity;
	Frupic frupic;
	Context context;

	public FetchTask(Frupic frupic) {
		this.frupic = frupic;
		this.activity = null;
	}

	public void setActivity(Context context, ProgressTaskActivityInterface activity) {
		this.context = context;
		this.activity = activity;
	}

	protected void onPreExecute() {
		filename = frupic.getFileName(false);
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		activity.updateProgressDialog(values[0], values[1]);
	}

	@Override
	protected Void doInBackground(Void... params) {
		if (!frupic.getCachedFile(context).exists()) {
			if (!FrupicFactory.fetchFrupicImage(context, frupic, false,
					new FrupicFactory.OnFetchProgress() {

						@Override
						public void OnProgress(int read, int length) {
							publishProgress(read, length);
						}
					})) {
				cancel(false);
				return null;
			}
		}

		return null;
	}

	protected void onCancelled() {
		if (activity != null)
			activity.dismiss();
	}

	protected void onPostExecute(Void result) {
		if (activity != null) {
			activity.dismiss();
			activity.success();
		}
	}
}
