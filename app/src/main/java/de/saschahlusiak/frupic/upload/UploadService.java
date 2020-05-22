package de.saschahlusiak.frupic.upload;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.app.App;
import de.saschahlusiak.frupic.app.FreamwareApi;
import de.saschahlusiak.frupic.app.FrupicRepository;
import de.saschahlusiak.frupic.grid.GridActivity;

public class UploadService extends IntentService {
	private static final String tag = UploadService.class.getSimpleName();
	private final static int nId = 1; /* must be a unique notification id */

	private final String CHANNEL_UPLOAD = "upload";

	/* when scaling down, make largest side as small but bigger than these bounds */
	private final static int destWidth = 1024;
	private final static int destHeight = 1024;

	@Inject
	protected FreamwareApi api;

	@Inject
	protected FrupicRepository repository;

	int current, max, failed;

	NotificationCompat.Builder builder;
	NotificationManagerCompat mNotificationManager;
	PendingIntent pendingIntent;

	public UploadService() {
		super("UploadService");
	}

	@Override
	public void onCreate() {
		Log.d(tag, "onCreate");

		((App) getApplicationContext()).appComponent.inject(this);

		mNotificationManager = NotificationManagerCompat.from(this);
		builder = new NotificationCompat.Builder(this, CHANNEL_UPLOAD);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createChannel();
		}

		final Intent intent = new Intent(this, GridActivity.class);
		pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		max = 0;
		current = 0;
		failed = 0;

		updateNotification(true, 0.0f);

		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.d(tag, "onDestroy");
		updateNotification(false, 0.0f);
		repository.synchronizeAsync(0, 100);
		super.onDestroy();
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void createChannel() {
		NotificationChannel channel = new NotificationChannel(CHANNEL_UPLOAD, "Upload", NotificationManager.IMPORTANCE_LOW);
		mNotificationManager.createNotificationChannel(channel);
	}

	private byte[] getImageData(File file, int orientation, boolean scale) {
		InputStream is = null;


		try {
			is = new FileInputStream(file);

			/* Copy image to memory to know size in bytes */
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();
			is.close();

			byte[] imageData = buffer.toByteArray();
			buffer.close();

			if (!scale)
				return imageData;

			/* this will get the original image size in px */
			Options options = new Options();
			options.inJustDecodeBounds = true;
			options.inInputShareable = true;
			options.inPurgeable = true;
			BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);

			options.inSampleSize = 1;

			/* scale image down to the smallest power of 2 that will fit into the desired dimensions */
			boolean scaleByHeight = Math.abs(options.outHeight - destHeight) >= Math.abs(options.outWidth - destWidth);
			if (options.outHeight * options.outWidth * 2 >= 16384) {
				double sampleSize = scaleByHeight ? (float) options.outHeight / (float) destHeight : (float) options.outWidth / (float) destWidth;
				options.inSampleSize = (int) Math.pow(2.0d, Math.floor(Math.log(sampleSize) / Math.log(2.0d)));
				Log.i(tag, "img (" + options.outWidth + "x" + options.outHeight + "), sample " + options.inSampleSize);
			}

			options.inJustDecodeBounds = false;
			options.inInputShareable = true;
			options.inPurgeable = true;

			/* get a scaled version of our original image */
			Bitmap b = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
			if (b == null)
				return null;

			/* If original image has orientation information (Exif), rotate our scaled image, which has
			 * otherwise lost the orientation
			 */
			if (orientation != 0) {
				Matrix matrix = new Matrix();
				matrix.preRotate(orientation);
				b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
			}

			/* and get a buffer agaion from our new image */
			buffer = new ByteArrayOutputStream();
			if (b.compress(CompressFormat.JPEG, 90, buffer) == true) {
				imageData = buffer.toByteArray();
				buffer.close();
				return imageData;
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (is != null)
					is.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return null;
		}
	}

	private String uploadImage(byte[] imageData, String username, String tags) {
		final long[] lastUpdate = new long[]{0L};
		return api.uploadImageSync(imageData, username, tags, (written, size) -> {
			Log.d(tag, "Progress: " + written + "(" + size + ")");
			// we have to rate-limit updates to the notification otherwise the final one may not come through
			if (System.currentTimeMillis() - lastUpdate[0] > 500) {
				lastUpdate[0] = System.currentTimeMillis();
				updateNotification(true, (float) written / (float) size);
			}
			return null;
		});
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(tag, "onHandleIntent");

		final byte[] imageData;
		final String userName = intent.getStringExtra("username");
		final String tags = intent.getStringExtra("tags");
		final String filename = intent.getStringExtra("filename");
		final boolean scale = intent.getBooleanExtra("scale", true);
		final int orientation = intent.getIntExtra("orientation", 0);
		final String path = intent.getStringExtra("path");
		final File file = new File(path);

		updateNotification(true, 0.0f);

		imageData = getImageData(file, orientation, scale);
		/* TODO: handle error gracefully */
		if (imageData == null) {
			failed++;
			return;
		}

		/* TODO: watch network state to pause/restart upload */
		String error = uploadImage(imageData, userName, tags);

		file.delete();

		if (error != null) {
			Log.e(tag, "error: " + error);
			failed++;
			/* TODO: handle error gracefully */
		} else {
			Log.i(tag, "Upload successful: " + filename);
		}

		updateNotification(true, 1.0f);
		current++;
	}

	synchronized void updateNotification(boolean ongoing, float progress) {
		builder.setContentTitle(getString(R.string.upload_notification_title));
		builder.setColor(getColor(R.color.brand_yellow_bright));
		if (ongoing) {
			builder.setSmallIcon(R.drawable.frupic_notification_wait);
			builder.setContentText(getString(R.string.upload_notification_progress, current + 1, max));
			if (max > 0) {
				float perc = (((float) current + progress) / (float) max);
				builder.setProgress(100, (int) (perc * 100.0f), false);
			}
			builder.setAutoCancel(false);
			builder.setOngoing(true);
			/* TODO: provide intent to see progress dialog and support for cancel */
		} else {
			if (failed == 0) {
				builder.setContentTitle(getString(R.string.upload_notification_title_finished));
				builder.setSmallIcon(R.drawable.frupic_notification_success);
				builder.setContentText(getString(R.string.upload_notification_success, max));
				builder.setTicker(getString(R.string.upload_notification_success, max));
			} else {
				builder.setContentTitle(getString(R.string.upload_notification_title_finished));
				/* TODO: set icon for failed uploads */
				builder.setSmallIcon(R.drawable.frupic_notification_failed);
				builder.setContentText(getString(R.string.upload_notification_failed, max - failed, failed));
			}
			builder.setProgress(0, 0, false);
			builder.setAutoCancel(true);
			builder.setOngoing(false);
		}

		/* TODO: set progress dialog intent when ongoing */
		builder.setContentIntent(pendingIntent);

		mNotificationManager.notify(nId, builder.build());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(tag, "onStartCommand");
		max++;

		updateNotification(true, 0.0f);

		return super.onStartCommand(intent, flags, startId);
	}
}
