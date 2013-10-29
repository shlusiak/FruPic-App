package de.saschahlusiak.frupic.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.saschahlusiak.frupic.cache.BitmapCache;
import de.saschahlusiak.frupic.cache.FileCache;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

public class FrupicFactory {
	static final private String tag = FrupicFactory.class.getSimpleName();
	static final String INDEX_URL = "http://api.freamware.net/2.0/get.picture";
	
	public static final int NOT_AVAILABLE = 0;
	public static final int FROM_CACHE = 1;
	public static final int FROM_FILE = 2;
	public static final int FROM_WEB = 3;
	
	private static final int NUM_CLIENTS = 4;

	Context context;
	BitmapCache cache;
	FileCache fileCache;
	int targetWidth, targetHeight;
	ArrayBlockingQueue<DefaultHttpClient> clients;

	public FrupicFactory(Context context, int bitmapCacheSize) {
		this.context = context;
		this.cache = new BitmapCache(bitmapCacheSize);
		targetWidth = 800;
		targetHeight = 800;
		clients = new ArrayBlockingQueue<DefaultHttpClient>(NUM_CLIENTS);
		for (int i = 0; i < NUM_CLIENTS; i++)
			clients.add(new DefaultHttpClient());
		createFileCache();
	}
	
	public FrupicFactory(Context context) {
		this(context, 1);
	}

	public void setTargetSize(int width, int height) {
		this.targetWidth = width;
		this.targetHeight = height;
	}
	
	public void createFileCache() {
		fileCache = new FileCache(context);
	}
	
	public FileCache getFileCache() {
		return fileCache;
	}

	private String fetchURL(String url) throws IOException {
		InputStream in = null;
		HttpResponse resp;
		DefaultHttpClient client;
		String result = null;

		try {
			client = clients.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		
		try {
			resp = client.execute(new HttpGet(url));

			final StatusLine status = resp.getStatusLine();
			if (status.getStatusCode() != 200) {
				Log.d(tag, "HTTP error, invalid server status code: " + resp.getStatusLine());
				clients.add(client);
				return null;
			}

			in = resp.getEntity().getContent();
		
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 1024;
			byte[] buffer = new byte[bufSize];
			while ((read = in.read(buffer)) > 0) {
				baf.append(buffer, 0, read);
			}
			in.close();
			result = new String(baf.toByteArray());
		} finally {			
			clients.add(client);
		}
		return result;
		
	}
	
	private Frupic[] getFrupicIndexFromString(String string) {
		JSONArray array;
		try {
			array = new JSONArray(string);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		if (array.length() < 1)
			return null;

		try {
			Frupic pics[] = new Frupic[array.length()];
			for (int i = 0; i < array.length(); i++) {
				JSONObject data = array.optJSONObject(i);
				if (data != null) {
					pics[i] = new Frupic();

					pics[i].thumb_url = data.getString("thumb_url");
					pics[i].id = data.getInt("id");
					pics[i].full_url = data.getString("url");
					pics[i].date = data.getString("date");
					pics[i].username = data.getString("username");
					pics[i].flags |= Frupic.FLAG_NEW | Frupic.FLAG_UNSEEN;
					
					JSONArray tags = data.getJSONArray("tags");
					if ((tags != null) && (tags.length() > 0)) {
						pics[i].tags = new String[tags.length()];
						for (int j = 0; j < tags.length(); j++)
							pics[i].tags[j] = tags.getString(j);
					}
				}
			}
			return pics;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}	
	}

	public Frupic[] fetchFrupicIndex(String username, int offset, int limit)
			throws IOException {

		String s = INDEX_URL + "?";
		if (username != null)
			s += "username=" + username + "&";
		s = s + "offset=" + offset + "&limit=" + limit;
		
		String queryResult = fetchURL(s);
		if (queryResult == null || "".equals(queryResult))
			return null;
		
		if (Thread.interrupted())
			return null;
		
		return getFrupicIndexFromString(queryResult);
	}

	private Bitmap decodeImageFile(String filename, int width, int height) {
		Bitmap b;
		Options options = new Options();
		File file;
		
		file = new File(filename);
		if (!file.exists() || !file.canRead())
			return null;

		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);
		
		options.inJustDecodeBounds = false;
		options.inInputShareable = true;
		options.inPurgeable = true;
		options.inSampleSize = 1;

		Boolean scaleByHeight = Math.abs(options.outHeight - height) >= Math
				.abs(options.outWidth - width);
		if (options.outHeight * options.outWidth * 2 >= 16384) {
			// Load, scaling to smallest power of 2 that'll get it <= desired
			// dimensions
			double sampleSize = scaleByHeight ? options.outHeight / height
					: options.outWidth / width;
			options.inSampleSize = (int) Math.pow(2d, Math.floor(Math
					.log(sampleSize)
					/ Math.log(2d)));
			Log.i(tag, "img (" + options.outWidth + "x" + options.outHeight
					+ "), sample " + options.inSampleSize);
		}

		b = BitmapFactory.decodeFile(filename, options);
		
		if (b == null) {
			Log.e("FruPic", "Error decoding image stream: " + file);
		} else {
			
			try {
				ExifInterface exif = new ExifInterface(filename);
				Matrix matrix = new Matrix();
				
				switch(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					matrix.preRotate(90);
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					matrix.preRotate(180);
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					matrix.preRotate(270);
					break;
					
				default: 
					matrix = null;
					break;
				}

				if (matrix != null)
					b = Bitmap.createBitmap(b, 0, 0, options.outWidth, options.outHeight, matrix, true);
			} catch (IOException e) {
//				e.printStackTrace();
			}		
		}
		
		
		return b;
	}

	public interface OnFetchProgress {
		void OnProgress(int read, int length);
	}

	public boolean fetchFrupicImage(Frupic frupic, boolean fetch_thumb, OnFetchProgress progress) {
		OutputStream myOutput = null;
		HttpResponse resp;
		File tmpFile = new File(fileCache.getFileName(frupic, fetch_thumb) + ".tmp");
		DefaultHttpClient client;
		if (Thread.interrupted())
			return false;
		try {
			client = clients.take();
		} catch (InterruptedException e1) {
			return false;
		}
		try {
			HttpUriRequest req;
			
			req = new HttpGet(fetch_thumb ? frupic.thumb_url : frupic.full_url);
			resp = client.execute(req);

			final StatusLine status = resp.getStatusLine();
			if (status.getStatusCode() != 200) {
				Log.d(tag, "HTTP error, invalid server status code: " + resp.getStatusLine());
				resp.getEntity().consumeContent();
				clients.add(client);
				return false;
			}

			int copied;

			long maxlength = resp.getEntity().getContentLength();
			InputStream myInput = resp.getEntity().getContent();
			
			myOutput = new FileOutputStream(tmpFile);
			byte[] buffer = new byte[4096];
			int length;
			copied = 0;
			while ((length = myInput.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
				if (Thread.interrupted()) {
					resp.getEntity().consumeContent();
					clients.add(client);
					myOutput.flush();
					myInput.close();
					myOutput.close();
					if (!tmpFile.delete()) {
						Log.e(tag, "error removing partly downloaded file "
							+ tmpFile.getName());
					}
					return false;
				}
			
				if (progress != null)
					progress.OnProgress(copied, (int)maxlength);
				copied += length;
			}
			myOutput.flush();
			myInput.close();
			myOutput.close();
			if (Thread.interrupted()) {
				clients.add(client);
				if (!tmpFile.delete()) {
					Log.e(tag, "error removing partly downloaded file "
						+ tmpFile.getName());
				}
				return false;
			}
			synchronized(fileCache) {
				tmpFile.renameTo(fileCache.getFile(frupic, fetch_thumb));
			}
			clients.add(client);
			return true;
		} catch (Exception e) {
			if (myOutput != null) {
				try {
					myOutput.flush();
					myOutput.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				myOutput = null;
			}
			clients.add(client);
			
			if (!tmpFile.delete()) {
				Log.e(tag, "error removing partly downloaded file " + tmpFile.getName());
			}
			if (!tmpFile.delete()) {
				Log.e(tag, "error removing partly downloaded file "	+ tmpFile.getName());
			}
			e.printStackTrace();
			return false;
		}
	}

	public static synchronized boolean copyImageFile(File in, File out) {
		if (out.exists())
			return true;
		
		try {
			InputStream is = new FileInputStream(in);
			OutputStream os = new FileOutputStream(out);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
			os.flush();
			is.close();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param frupic
	 * @param thumb
	 * @param width
	 * @param height
	 * @param onProgress
	 * @return Did some fetching occur? Do visuals need to be updated?
	 */
	public int fetch(Frupic frupic, boolean thumb, int width, int height, OnFetchProgress onProgress) {
		String filename = fileCache.getFileName(frupic, thumb);
		int ret;

		/* If file already in memory cache, return */

		/*
		 * XXX: if file is always kept in memory and it's lastModified time is
		 * never updated, it may be pruned from file system while still in memory.
		 */
		if (cache.get(fileCache.getFileName(frupic, thumb)) != null)
			return FROM_CACHE;	/* the picture was available before; don't notify again */

		File f = new File(filename);
		/* Fetch file from the Interweb, unless cached locally */
		if (!f.exists()) {
			if (! fetchFrupicImage(frupic, thumb, onProgress)) {
				return NOT_AVAILABLE;
			}
			ret = FROM_WEB;
//			Log.d(tag, "Downloaded file " + frupic.id);
		} else
			ret = FROM_FILE;
		
		if (Thread.interrupted())
			return NOT_AVAILABLE;
		/* touch file, so it is purged from cache last */
		f.setLastModified(new Date().getTime());

		/* Load downloaded file and add bitmap to memory cache */
		Bitmap b = decodeImageFile(filename, width, height);
		if ((b == null) || (Thread.interrupted())) {
			Log.d(tag, "Error loading to memory: " + frupic.id);
			return NOT_AVAILABLE;
		}
//		Log.d(tag, "Loaded file to memory: " + frupic.id);
		cache.add(fileCache.getFileName(frupic, thumb), b);

		return ret;
	}

	public int fetchThumb(Frupic frupic) {
		return fetch(frupic, true, targetWidth, targetHeight, null);
	}

	public int fetchFull(Frupic frupic, OnFetchProgress onProgress) {
		return fetch(frupic, false, targetWidth, targetHeight, onProgress);
	}

	public Bitmap getThumbBitmap(Frupic frupic) {
		return cache.get(fileCache.getFileName(frupic, true));
	}

	public Bitmap getFullBitmap(Frupic frupic) {
		return cache.get(fileCache.getFileName(frupic, false));
	}
}