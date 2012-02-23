package de.saschahlusiak.frupic.model;
 
import java.io.BufferedInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

public class FrupicFactory {
	static final private String tag = FrupicFactory.class.getSimpleName();
	static final public int DEFAULT_CACHE_SIZE = 1024 * 1024 * 16; /* 10 MB */
	static final String INDEX_URL = "http://api.freamware.net/2.0/get.picture";

	Context context;
	FrupicCache cache;
	int targetWidth, targetHeight;
	int cachesize;

	public FrupicFactory(Context context, int cachesize) {
		this.context = context;
		this.cache = new FrupicCache(cachesize);
		targetWidth = 800;
		targetHeight = 800;
		this.cachesize = DEFAULT_CACHE_SIZE;
	}

	public void setTargetSize(int width, int height) {
		this.targetWidth = width;
		this.targetHeight = height;
	}

	public void setCacheSize(int cachesize) {
		this.cachesize = cachesize;
	}

	private String fetchURL(String url) {
		InputStream in = null;

		try {
			HttpURLConnection urlConn;
			urlConn = (HttpURLConnection) new URL(url).openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) urlConn;
			httpConn.setAllowUserInteraction(false);
			httpConn.connect();
			in = httpConn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(in);
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 1024;
			byte[] buffer = new byte[bufSize];
			while (true) {
				read = bis.read(buffer);
				if (read == -1) {
					break;
				}
				baf.append(buffer, 0, read);
			}
			return new String(baf.toByteArray());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return null;
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
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
	
	public Frupic[] fetchFrupicIndexFromCache() {
		try {
			InputStream is = context.openFileInput("index");
			if (is == null)
				return null;
			BufferedInputStream bis = new BufferedInputStream(is);
		
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 1024;
			byte[] buffer = new byte[bufSize];
			while (true) {
				read = bis.read(buffer);
				if (read == -1) {
					break;
				}
				baf.append(buffer, 0, read);
			}
			return getFrupicIndexFromString(new String(baf.toByteArray()));
		} catch (IOException e) {
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
		if (queryResult == null)
			return null;
		
		/* Always cache last query */
		PrintWriter pwr = new PrintWriter(context.openFileOutput("index", Context.MODE_WORLD_READABLE));
		pwr.write(queryResult);
		pwr.flush();
		pwr.close();		
		
		return getFrupicIndexFromString(queryResult);
	}
	
	public static class CacheInfo {
		int bytes;
		int files;
		CacheInfo(int bytes, int files) {
			this.bytes = bytes;
			this.files = files;
		}
		public int getCount() {
			return files;
		}
		public int getSize() {
			return bytes;
		}
	}

	public CacheInfo pruneCache() {
		return pruneCache(context, cachesize);
	}

	public static synchronized CacheInfo pruneCache(Context context, int limit) {
		File files[] = context.getCacheDir().listFiles((FilenameFilter) null);
		if (files.length == 0)
			return new CacheInfo(0, 0);

		int total;
		int number = 0;
		do {
			int oldest = -1;
			total = 0;
			number = 0;

			for (int i = 0; i < files.length; i++) {
				if (files[i] == null)
					continue;

				number++;
				if ((oldest < 0)
						|| (files[i].lastModified() < files[oldest]
								.lastModified()))
					oldest = i;
				total += files[i].length();
			}

			if (limit < 0)
				break;
			
			if (total > limit) {
				Log.i(tag, "purged " + files[oldest].getName()
						+ " from filesystem");
				if (files[oldest].delete())
					total -= files[oldest].length();
				number--;
				files[oldest] = null;
			}
		} while (total > limit);
		Log.d(tag, "left file cache populated with " + total + " bytes");
		return new CacheInfo(total, number);
	}

	public String getCacheFileName(Frupic frupic, boolean thumb) {
		return getCacheFileName(context, frupic, thumb);
	}

	public static String getCacheFileName(Context context, Frupic frupic,
			boolean thumb) {
		return context.getCacheDir() + File.separator
				+ frupic.getFileName(thumb);
	}

	private Bitmap decodeImageFile(String filename, int width, int height) {
		File file = new File(filename);
		if (!file.exists() || !file.canRead())
			return null;

		Options options = new Options();
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

		Bitmap b = BitmapFactory.decodeFile(filename, options);
		if (b == null) {
			Log.e("FruPic", "Error decoding image stream: " + file);
		}
		return b;
	}

	public interface OnFetchProgress {
		void OnProgress(int read, int length);
	}

	public static synchronized boolean fetchFrupicImage(Context context,
			Frupic frupic, boolean fetch_thumb, OnFetchProgress progress) {
		try {
			URL url = new URL(fetch_thumb ? frupic.thumb_url : frupic.full_url);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setUseCaches(true);

			Object response = connection.getContent();
			int maxlength = connection.getContentLength();
			int copied;

			InputStream myInput = (InputStream) response;
			OutputStream myOutput = new FileOutputStream(getCacheFileName(
					context, frupic, fetch_thumb));
			byte[] buffer = new byte[4096];
			int length;
			copied = 0;
			while ((length = myInput.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
				if (progress != null)
					progress.OnProgress(copied, maxlength);
				copied += length;
				if (Thread.interrupted()) {
					myOutput.flush();
					myInput.close();
					myOutput.close();
					frupic.getCachedFile(context).delete();
					Log.d(tag, "removed partly downloaded file "
							+ getCacheFileName(context, frupic, fetch_thumb));
					return false;
				}
			}
			myOutput.flush();
			myInput.close();
			myOutput.close();
			return true;
		} catch (Exception e) {
			frupic.getCachedFile(context).delete();
			Log.d(tag, "removed partly downloaded file " + getCacheFileName(context, frupic, fetch_thumb));
			e.printStackTrace();
			return false;
		}
	}

	public static synchronized boolean copyImageFile(File in, File out) {
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

	public synchronized boolean fetch(Frupic frupic, boolean thumb, int width, int height, OnFetchProgress onProgress) {
		String filename = getCacheFileName(frupic, thumb);

		/* If file already in memory cache, return */

		/*
		 * XXX: if file is always kept in memory and it's lastModified time is
		 * never updated, it may be pruned from file system while still in memory.
		 */
		if (cache.get(getCacheFileName(frupic, thumb)) != null)
			return false;	/* TODO: it probably was available before; don't notify again for success */

		File f = new File(filename);
		/* Fetch file from the Interweb, if not cached locally */
		if (!f.exists()) {
			if (! fetchFrupicImage(context, frupic, thumb, onProgress)) {
				return false;
			}
			Log.d(tag, "Downloaded file " + frupic.id);
		}
		/* touch file, so it is pruned last */
		f.setLastModified(new Date().getTime());
		if (Thread.interrupted())
			return false;

		/* Load downloaded file and add bitmap to memory cache */
		Bitmap b = decodeImageFile(filename, width, height);
		if ((b == null) || (Thread.interrupted())) {
			Log.d(tag, "Error loading to memory: " + frupic.id);
			return false;
		}
		Log.d(tag, "Loaded file to memory: " + frupic.id);
		cache.add(getCacheFileName(frupic, thumb), b);

		return true;
	}

	public boolean fetchThumb(Frupic frupic) {
		return fetch(frupic, true, targetWidth, targetHeight, null);
	}

	public boolean fetchFull(Frupic frupic, OnFetchProgress onProgress) {
		return fetch(frupic, false, targetWidth, targetHeight, onProgress);
	}

	public Bitmap getThumbBitmap(Frupic frupic) {
		return cache.get(getCacheFileName(frupic, true));
	}

	public Bitmap getFullBitmap(Frupic frupic) {
		return cache.get(getCacheFileName(frupic, false));
	}

}