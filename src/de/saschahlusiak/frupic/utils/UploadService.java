package de.saschahlusiak.frupic.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.grid.FruPicGrid;
import de.saschahlusiak.frupic.utils.UploadTask.ProgressTaskActivityInterface;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

public class UploadService extends IntentService implements ProgressTaskActivityInterface {
	private static final String tag = UploadService.class.getSimpleName();
	private static int nId = 1;
	
	/* when scaling down, make largest side as small but bigger than these bounds */
	private final static int destWidth = 1024;
	private final static int destHeight = 1024;

	
	int current, max, failed;
	
	Notification.Builder builder;
	NotificationManager mNotificationManager;
			
	public UploadService() {
		super("UploadService");
	}
	
	@Override
	public void onCreate() {
		Log.d(tag, "onCreate");
		
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		builder = new Notification.Builder(this);
		
		max = 0;
		current = 0;
		failed = 0;
		
		updateNotification(false, 0.0f);
				
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		Log.d(tag, "onDestroy");
		updateNotification(false, 0.0f);
		super.onDestroy();
	}
	
	
	
	byte[] getImageData(Uri param, boolean scale) {
		InputStream is = null;
		
		int orientation = 0;
		/* first get the orientation for the image, neccessary when scaling the image,
		 * so the orientation is preserved */
		try {
			Cursor cursor = getContentResolver().query(
					param, new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
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
			is = getContentResolver().openInputStream(param);

			/* Copy image to memory to know size in bytes */
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();
			is.close();

			byte[] imageData= buffer.toByteArray();
			buffer.close();
			
			if (scale == false)
				return imageData;

			/* this will get the original image size in px */
			Options options = new Options();
			options.inJustDecodeBounds = true;
			options.inInputShareable = true;
			options.inPurgeable = true;
			BitmapFactory.decodeByteArray(imageData, 0,	imageData.length, options);

			options.inSampleSize = 1;
			
			/* scale image down to the smallest power of 2 that will fit into the desired dimensions */
			Boolean scaleByHeight = Math.abs(options.outHeight - destHeight) >= Math.abs(options.outWidth - destWidth);
			if (options.outHeight * options.outWidth * 2 >= 16384) {
				double sampleSize = scaleByHeight ? (float)options.outHeight / (float)destHeight : (float)options.outWidth / (float)destWidth;
				options.inSampleSize = (int) Math.pow(2.0d, Math.floor(Math.log(sampleSize) / Math.log(2.0d)));
				Log.i(tag, "img (" + options.outWidth + "x"	+ options.outHeight + "), sample "+ options.inSampleSize);
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


	
	
	
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(tag, "onHandleIntent");
		
		byte[] imageData;
		String userName = intent.getStringExtra("username");
		String tags = intent.getStringExtra("tags");
		String filename = intent.getStringExtra("filename");
		boolean scale = intent.getBooleanExtra("scale", true);
		updateNotification(true, 0.0f);
		
		imageData = getImageData((Uri)intent.getParcelableExtra("uri"), scale);
		/* TODO: handle error gracefully */
		if (imageData == null) {
			failed++;
			return;
		}
		
		UploadTask task = new UploadTask(imageData, userName, tags);
		task.setActivity(this, this);
		task.execute();
		
		String error = null;
		try {
			error = task.get();
			if (error != null) {
				Log.e(tag, "error: " + error);
				failed++;
				/* TODO: handle error gracefully */
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			failed++;
			/* TODO: handle error gracefully */
		}
		
		updateNotification(true, 1.0f);
		current++;
	}
	
	synchronized void updateNotification(boolean ongoing, float progress) {
		builder.setSmallIcon(R.drawable.frupic);
		builder.setContentTitle("Uploading to FruPic");
		if (ongoing) {
			builder.setContentText("Uploading " + (current + 1) + " of " + max);
			if (Build.VERSION.SDK_INT >= 14 && max > 0) {
				float perc = (((float)current + progress) / (float)max);
				builder.setProgress(100, (int)(perc * 100.0f), false);
			}
			builder.setAutoCancel(false);
			builder.setOngoing(ongoing);
		} else {
			if (failed == 0)
				builder.setContentText("" + max + " images uploaded");
			else
				builder.setContentText("" + (max - failed) + " images successful, " + failed + " failed");
				
			if (Build.VERSION.SDK_INT >= 14)
				builder.setProgress(0, 0, false);
			builder.setAutoCancel(true);
			builder.setOngoing(false);
		}
		Intent intent = new Intent(this, FruPicGrid.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);
		
		mNotificationManager.notify(nId, builder.getNotification());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String filename = intent.getStringExtra("filename");
		
		Log.d(tag, "onStartCommand");
		max++;

		updateNotification(true, 0.0f);
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void updateProgressDialog(int progress, int max) {
		updateNotification(true, (float)progress / (float)max);
	}
}
