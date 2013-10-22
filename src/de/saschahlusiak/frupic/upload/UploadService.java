package de.saschahlusiak.frupic.upload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.grid.FruPicGrid;
import android.annotation.TargetApi;
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

public class UploadService extends IntentService {
	private static final String tag = UploadService.class.getSimpleName();
	private static int nId = 1;
	
	private final String FruPicApi = "http://api.freamware.net/2.0/upload.picture";
	
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


	String uploadImage(byte[] imageData, String username, String tags) {
		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		DataInputStream dis;

		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "ForeverFrubarIWantToBe";

		try {
			URL url = new URL(FruPicApi);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);

			conn.setUseCaches(false);
			conn.setRequestMethod("POST");

			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type",
					"multipart/form-data;boundary=" + boundary);
			
			
			// Tags
			String header = lineEnd + twoHyphens + boundary + lineEnd +
							"Content-Disposition: form-data;name='tags';" +
							lineEnd + lineEnd + tags + ";" + lineEnd +
							lineEnd + twoHyphens + boundary + lineEnd;
			if (!username.equals("")) {
				header += lineEnd + twoHyphens + boundary + lineEnd;
				header += "Content-Disposition: form-data;name='username';";
				header += lineEnd + lineEnd + username + ";" + lineEnd +
						  lineEnd + twoHyphens + boundary + lineEnd;
			}
			header += "Content-Disposition: form-data;" + "name='file';" +
					  "filename='frup0rn.png'" + lineEnd + lineEnd;
			
			String footer = lineEnd + twoHyphens + boundary + twoHyphens + lineEnd;
			
			conn.setFixedLengthStreamingMode(header.length() + footer.length() + imageData.length);
			

			OutputStream os = conn.getOutputStream();
			
			dos = new DataOutputStream(os);

			dos.writeBytes(header);

			InputStream is = new ByteArrayInputStream(imageData);
			int size = imageData.length;
			int nRead;
			byte data[] = new byte[16384];

			int written = 0;
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				dos.write(data, 0, nRead);
				written += nRead;
				dos.flush();
				os.flush();
				
				/* TODO: support cancel */
				/* if (isCancelled()) {
					dos.close();
					conn.disconnect();
					return null;
				} */
				updateNotification(true, (float)written / (float)size);
			}

			// Log.d(TAG, "FINISHED sending the image");

			// send multipart form data necesssary after file data...
			dos.writeBytes(footer);

			dos.flush();
			dos.close();
		} catch (IOException ioe) {
			conn.disconnect();
			ioe.printStackTrace();
			return getString(R.string.cannot_connect);
		}

		// listening to the Server Response
		// Log.d(TAG, "listening to the server");
		try {
			dis = new DataInputStream(conn.getInputStream());

			String str = "";
			String output = "";

			while ((str = dis.readLine()) != null) {
				output = output + str;
				// Log.d(TAG, output);

				// save the url to the image
				// frupicURL = output;
				// Log.d(TAG, "the image url is: "+FruPic.imageURL);
			}
			dis.close();
		} catch (IOException ioex) {
			ioex.printStackTrace();
			return getString(R.string.error_reading_response);
			// Log.e(TAG, "Exception" , ioex);
		}
//		Log.i(tag, "Upload successful: " + frupicURL);

		return null;
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
		
		/* TODO: watch network state to pause/restart upload */
		String error = uploadImage(imageData, userName, tags);
		
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
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	synchronized void updateNotification(boolean ongoing, float progress) {
		builder.setContentTitle(getString(R.string.upload_notification_title));
		if (ongoing) {
			builder.setSmallIcon(R.drawable.frupic);
			builder.setContentText(getString(R.string.upload_notification_progress, current + 1, max));
			if (Build.VERSION.SDK_INT >= 14 && max > 0) {
				float perc = (((float)current + progress) / (float)max);
				builder.setProgress(100, (int)(perc * 100.0f), false);
			}
			builder.setAutoCancel(false);
			builder.setOngoing(ongoing);
			/* TODO: provide intent to see progress dialog and support for cancel */
		} else {
			if (failed == 0) {
				builder.setContentTitle(getString(R.string.upload_notification_title_finished));
				builder.setSmallIcon(R.drawable.frupic_success);
				builder.setContentText(getString(R.string.upload_notification_success, max));
			} else {
				builder.setContentTitle(getString(R.string.upload_notification_title_finished));
				/* TODO: set icon for failed uploads */
				builder.setSmallIcon(R.drawable.frupic);
				builder.setContentText(getString(R.string.upload_notification_failed, max - failed, failed));
			}
			if (Build.VERSION.SDK_INT >= 14)
				builder.setProgress(0, 0, false);
			builder.setAutoCancel(true);
			builder.setOngoing(false);
		}
		/* TODO: set progress dialog intent when ongoing */
		Intent intent = new Intent(this, FruPicGrid.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);
		
		mNotificationManager.notify(nId, builder.getNotification());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(tag, "onStartCommand");
		max++;

		updateNotification(true, 0.0f);
		
		return super.onStartCommand(intent, flags, startId);
	}
}
