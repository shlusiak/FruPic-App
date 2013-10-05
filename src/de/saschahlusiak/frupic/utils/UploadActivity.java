package de.saschahlusiak.frupic.utils;

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

	Uri imageUri;
	String fileName;
	CheckBox image_scale;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fileName = "";

		if (getIntent() == null) {
			finish();
			return;
		}
		/* TODO: support ACTION_SEND_MULTIPLE */
		/* TODO: possibly use a service instead of AsyncTask */
		if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
			finish();
			return;
		}
		
		setContentView(R.layout.upload_activity);

		imageUri = (Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM);

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
			if (cursor != null)
				cursor.close();
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

		findViewById(R.id.upload).setOnClickListener(this);

		image_scale = (CheckBox) findViewById(R.id.image_resize_checkbox);
		image_scale.setChecked(true);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (prefs.getString("username", "") != "")
			((EditText) findViewById(R.id.username)).setText(prefs.getString(
					"username", null));
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
			
		Intent intent = new Intent(this, UploadService.class);
		intent.putExtra("scale", ((CheckBox)findViewById(R.id.image_resize_checkbox)).isChecked());
		intent.putExtra("username", username);
		intent.putExtra("tags", tags);
		intent.putExtra("filename", fileName);
		intent.putExtra("uri", imageUri);
		startService(intent);
		finish();
	}
}
