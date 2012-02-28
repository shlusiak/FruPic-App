package de.saschahlusiak.frupic.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.saschahlusiak.frupic.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UploadActivity extends Activity implements OnClickListener {
	private static final String tag = UploadActivity.class.getSimpleName();
	private final static int destWidth = 800;
	private final static int destHeight = 800;
	private final static int DIALOG_UPLOAD_PROGRESS = 1;

	/*
	 * over 512 kb the image is automatically scaled down
	 */
	private final static int ORIGINAL_SIZE_LIMIT = 1024 * 512;

	private byte[] imageDataOriginal = null;
	private byte[] imageDataResized = null;
	Point original_image_size = null;
	Point resized_image_size = null;

	Uri imageUri;
	String fileName;
	CheckBox image_scale;
	static UploadTask uploadTask;
	ProgressDialog progressDialog;

	class ConvertTask extends AsyncTask<Uri, Void, Void> {

		@Override
		protected void onPreExecute() {
			((TextView) findViewById(R.id.imagesize))
					.setText(R.string.scale_size_calculating);
			((TextView) findViewById(R.id.filesize)).setText("");

			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Uri... params) {
			InputStream is = null;
			int orientation = 0;
			try {
				Cursor cursor = UploadActivity.this
						.getContentResolver()
						.query(params[0],
								new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
								null, null, null);

				if (cursor.getCount() == 1) {
					cursor.moveToFirst();
					orientation = cursor.getInt(0);
				}
				cursor.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			try {
				is = UploadActivity.this.getContentResolver().openInputStream(
						params[0]);

				/* Copy image to memory to know size in bytes */
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				int nRead;
				byte[] data = new byte[16384];
				while ((nRead = is.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}
				buffer.flush();
				is.close();

				imageDataOriginal = buffer.toByteArray();
				buffer.close();

				
				Options options = new Options();
				options.inJustDecodeBounds = true;
				options.inInputShareable = true;
				options.inPurgeable = true;
				BitmapFactory.decodeByteArray(imageDataOriginal, 0,
						imageDataOriginal.length, options);
				original_image_size = new Point(options.outWidth,
						options.outHeight);

				options.inJustDecodeBounds = false;
				options.inInputShareable = true;
				options.inPurgeable = true;
				options.inSampleSize = 1;

				Boolean scaleByHeight = Math
						.abs(options.outHeight - destHeight) >= Math
						.abs(options.outWidth - destWidth);
				if (options.outHeight * options.outWidth * 2 >= 16384) {
					// Load, scaling to smallest power of 2 that'll get it <= desired dimensions
					double sampleSize = scaleByHeight ? options.outHeight
							/ destHeight : options.outWidth / destWidth;
					options.inSampleSize = (int) Math.pow(2d, Math.floor(Math
							.log(sampleSize)
							/ Math.log(2d)));
					Log.i(tag, "img (" + options.outWidth + "x"
							+ options.outHeight + "), sample "
							+ options.inSampleSize);
				}

				Bitmap b = BitmapFactory.decodeByteArray(imageDataOriginal, 0, imageDataOriginal.length, options);
				if (b == null) {
					cancel(false);
					return null;
				}
				
				/* If original image has orientation information (Exif), rotate our scaled image, which has
				 * otherwise lost the orientation				 
				 */
				if (orientation != 0) {
					Matrix matrix = new Matrix();
					matrix.preRotate(orientation);
					b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
				}
				
				buffer = new ByteArrayOutputStream();
				if (b.compress(CompressFormat.JPEG, 90, buffer) == true) {
					imageDataResized = buffer.toByteArray();
					resized_image_size = new Point(b.getWidth(), b.getHeight());
					buffer.close();
				} else {
					imageDataResized = null;
					resized_image_size = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					if (is != null)
						is.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				cancel(false);
				return null;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			updateImageSize();
			findViewById(R.id.upload).setEnabled(true);
			image_scale.setEnabled(true);

			if ((imageDataOriginal != null)
					&& (imageDataOriginal.length > ORIGINAL_SIZE_LIMIT)) {
				image_scale.setChecked(true);
			} else {
				image_scale.setChecked(true);
			}
		}
	}

	public void updateImageSize() {
		if (imageDataOriginal == null)
			return;

		if (!image_scale.isChecked()) {
			/* Original size */
			((TextView) findViewById(R.id.imagesize))
					.setText((original_image_size == null) ? getString(R.string.scale_size_unknown)
							: String.format("%d x %d", original_image_size.x,
									original_image_size.y));
			((TextView) findViewById(R.id.filesize))
					.setText((imageDataOriginal == null) ? getString(R.string.scale_size_unknown)
							: "(" + imageDataOriginal.length / 1024 + " kb)");
		} else {
			/* Resized */
			((TextView) findViewById(R.id.imagesize))
					.setText((resized_image_size == null) ? getString(R.string.scale_size_unknown)
							: String.format("%d x %d", resized_image_size.x,
									resized_image_size.y));

			((TextView) findViewById(R.id.filesize))
					.setText((imageDataResized == null) ? getString(R.string.scale_size_unknown)
							: "(" + imageDataResized.length / 1024 + " kb)");
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fileName = "";

		if (getIntent() == null) {
			finish();
			return;
		}
		if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
			finish();
			return;
		}
		
		setContentView(R.layout.upload_activity);
		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		// getWindow().setAttributes((android.view.WindowManager.LayoutParams)
		// params);

		imageUri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);
		updateImageSize();
		new ConvertTask().execute(imageUri);

		String scheme = imageUri.getScheme();
		if (scheme.equals("file")) {
			fileName = imageUri.getLastPathSegment();
		} else if (scheme.equals("content")) {
			String[] proj = { MediaStore.Images.Media.DISPLAY_NAME };
			Cursor cursor = getContentResolver().query(imageUri, proj, null,
					null, null);
			if (cursor != null && cursor.getCount() != 0) {
				int columnIndex = cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
				cursor.moveToFirst();
				fileName = cursor.getString(columnIndex);
			}
		}
		if (fileName == null)
			fileName = "[Unknown]";
		((TextView) findViewById(R.id.url)).setText(getString(
				R.string.upload_file_name, fileName));

		findViewById(android.R.id.closeButton).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						finish();
					}
				});

		findViewById(R.id.upload).setEnabled(false);

		findViewById(R.id.upload).setOnClickListener(this);

		image_scale = (CheckBox) findViewById(R.id.image_resize_checkbox);
		image_scale.setEnabled(false);
		image_scale.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				updateImageSize();
			}
		});

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (prefs.getString("username", "") != "")
			((EditText) findViewById(R.id.username)).setText(prefs.getString(
					"username", null));
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onStart() {
		if (uploadTask != null)
			uploadTask.setActivity(this, progressUpdate);
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		if (uploadTask != null)
			uploadTask.setActivity(null, null);
		super.onStop();
	}
	
	
	protected android.app.Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_UPLOAD_PROGRESS:
			progressDialog = new ProgressDialog(this);
			progressDialog.setTitle(getString(R.string.please_wait));
			progressDialog.setMessage(getString(R.string.uploading_message,	fileName));
			progressDialog.setCancelable(true);
			progressDialog.setIndeterminate(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					uploadTask.cancel(true);
				}
			});
			return progressDialog;
			
		default:
			return super.onCreateDialog(id);
		}

	};
	
	ProgressTaskActivityInterface progressUpdate = new ProgressTaskActivityInterface() {
		
		@Override
		public void updateProgressDialog(int progress, int max) {
			progressDialog.setMax(max);
			progressDialog.setProgress(progress);
		}
		
		@Override
		public void success() {
			Toast.makeText(UploadActivity.this, R.string.uploading_successful_toast, Toast.LENGTH_SHORT).show();
		}
		
		@Override
		public void dismiss() {
			dismissDialog(DIALOG_UPLOAD_PROGRESS);
			finish();
		}
	};

	@Override
	public void onClick(View v) {

		/* store set username to prefs */
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		EditText edit = (EditText) findViewById(R.id.username);
		String username = edit.getText().toString();

		Editor e = prefs.edit();
		e.putString("username", username);
		e.commit();

		try {
			byte imageData[];
			if (! image_scale.isChecked())
				imageData = imageDataOriginal;
			else
				imageData = imageDataResized;
			
			String tags = ((EditText) findViewById(R.id.tags)).getText().toString();
			if (tags.length() <= 0)
				tags = "via:android";

			uploadTask = new UploadTask(imageData, username, tags);
			uploadTask.setActivity(this, progressUpdate);
			showDialog(DIALOG_UPLOAD_PROGRESS);
			uploadTask.execute();

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

	}
}
