package de.saschahlusiak.frupic.utils;

import java.io.File;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public class DetailsActivity extends Activity {
	Frupic frupic;

	static FetchTask fetchTask = null;
	ProgressDialog progressDialog;
	static final int DIALOG_PROGRESS = 1;



	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.details_activity);
		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes(
				(android.view.WindowManager.LayoutParams) params);

		frupic = (Frupic) getIntent().getSerializableExtra("frupic");
		setTitle("Frupic #" + frupic.getId());

		update();
	}
	
	ProgressTaskActivityInterface progressUpdater = new ProgressTaskActivityInterface() {
		
		@Override
		public void updateProgressDialog(int progress, int max) {
			progressDialog.setMax(max);
			progressDialog.setProgress(progress);
		}
		
		@Override
		public void success() {
			update();
		}
		
		@Override
		public void dismiss() {
			dismissDialog(DIALOG_PROGRESS);
		}
	};
	
	@Override
	protected void onStart() {
		if (fetchTask != null)
			fetchTask.setActivity(this, progressUpdater);
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (fetchTask != null)
			fetchTask.setActivity(null, null);
		super.onStop();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			progressDialog = new ProgressDialog(this);
			progressDialog.setTitle(getString(R.string.please_wait));
			progressDialog.setMessage(getString(R.string.fetching_image_message,
					frupic.getFileName(false)));
			progressDialog.setCancelable(true);
			progressDialog.setIndeterminate(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					fetchTask.cancel(true);
				}
			});
			return progressDialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	void update() {
		((TextView) findViewById(R.id.posted_by)).setText(frupic.getUsername());
		((TextView) findViewById(R.id.tags)).setText(frupic.getTagsString());
		((TextView) findViewById(R.id.date)).setText(frupic.getDate());
		((TextView) findViewById(R.id.url)).setText(frupic.getFullUrl());

		File f = frupic.getCachedFile(this);
		if (f.exists()) {
			Options options = new Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(FrupicFactory.getCacheFileName(this,
					frupic, false), options);

			((TextView) findViewById(R.id.size)).setText(String.format(
					"%d x %d", options.outWidth, options.outHeight));
			((TextView) findViewById(R.id.filesize)).setText(f.length() / 1024
					+ " kb");
			findViewById(R.id.cache_now).setVisibility(View.GONE);

		} else {
			((TextView) findViewById(R.id.size))
					.setText(R.string.details_not_available);
			((TextView) findViewById(R.id.filesize))
					.setText(R.string.details_not_available);
			findViewById(R.id.cache_now).setVisibility(View.VISIBLE);
			findViewById(R.id.cache_now).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							showDialog(DIALOG_PROGRESS);
							fetchTask = new FetchTask(frupic);
							fetchTask.setActivity(DetailsActivity.this, progressUpdater);
							fetchTask.execute();
						}
					});
		}
	}
}
