package de.saschahlusiak.frupic.utils;

import java.util.ArrayList;

import de.saschahlusiak.frupic.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class UploadActivity extends Activity implements OnClickListener {
	private static final String tag = UploadActivity.class.getSimpleName();

	ArrayList<Uri> imageUri;
	CheckBox image_scale;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent() == null) {
			finish();
			return;
		}
		
		if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
			imageUri = new ArrayList<Uri>();
			imageUri.add((Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM));
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction())) {
			Object o = getIntent().getExtras().get(Intent.EXTRA_STREAM);
			imageUri = (ArrayList<Uri>)o;
		} else {
			finish();
			return;
		}
		setContentView(R.layout.upload_activity);

		if (imageUri.size() == 1) {
			((TextView) findViewById(R.id.url)).setText(getString(
					R.string.upload_file_name, getFileName(imageUri.get(0))));
		} else {
			((TextView) findViewById(R.id.url)).setText(getString(
					R.string.upload_file_names, imageUri.size()));			
		}

		findViewById(android.R.id.closeButton).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						finish();
					}
				});

		findViewById(R.id.upload).setOnClickListener(this);

		image_scale = (CheckBox) findViewById(R.id.image_resize_checkbox);
		image_scale.setChecked(true);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (prefs.getString("username", "") != "")
			((EditText) findViewById(R.id.username)).setText(prefs.getString(
					"username", null));
	}
	
	String getFileName(Uri uri) {
		String fileName = null;
		String scheme = imageUri.get(0).getScheme();
		if (scheme.equals("file")) {
			fileName = imageUri.get(0).getLastPathSegment();
		} else if (scheme.equals("content")) {
			String[] proj = { MediaStore.Images.Media.DISPLAY_NAME };
			Cursor cursor = getContentResolver().query(imageUri.get(0), proj, null,
					null, null);
			if (cursor != null && cursor.getCount() != 0) {
				int columnIndex = cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
				cursor.moveToFirst();
				fileName = cursor.getString(columnIndex);
			}
			if (cursor != null)
				cursor.close();
		}
		if (fileName == null)
			fileName = "[Unknown]";
		return fileName;
	}

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

		String tags = ((EditText) findViewById(R.id.tags)).getText().toString();
		if (tags.length() <= 0)
			tags = "via:android";

		for (Uri uri: imageUri) {
			Intent intent = new Intent(this, UploadService.class);
			intent.putExtra("scale", ((CheckBox)findViewById(R.id.image_resize_checkbox)).isChecked());
			intent.putExtra("username", username);
			intent.putExtra("tags", tags);
			intent.putExtra("filename", uri);
			intent.putExtra("uri", uri);
			startService(intent);
		}
		finish();
	}
}
