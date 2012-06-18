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

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.preference.PreferenceManager;
import android.util.Log;

public class FrupicFactory {
	static final private String tag = FrupicFactory.class.getSimpleName();
	static final public int DEFAULT_CACHE_SIZE = 1024 * 1024 * 16; /* 10 MB */
	static final String INDEX_URL = "http://api.freamware.net/2.0/get.picture";
	
	public static final int NOT_AVAILABLE = 0;
	public static final int FROM_CACHE = 1;
	public static final int FROM_FILE = 2;
	public static final int FROM_WEB = 3;

	Context context;
	FrupicCache cache;
	int targetWidth, targetHeight;
	int cachesize;
	File internal_cachedir, external_cachedir;
	boolean prefer_external_cache;
	DefaultHttpClient client;

	public FrupicFactory(Context context, int cachesize) {
		this.context = context;
		this.cache = new FrupicCache(cachesize);
		targetWidth = 800;
		targetHeight = 800;
		this.cachesize = DEFAULT_CACHE_SIZE;
		client = new DefaultHttpClient();
		updateCacheDirs();
	}
	
	public FrupicFactory(Context context) {
		this(context, 1);
	}
	
	public void updateCacheDirs() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefer_external_cache = prefs.getBoolean("external_cache", true);

		this.internal_cachedir = context.getCacheDir();
		this.external_cachedir = context.getExternalCacheDir();
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
		HttpResponse resp;

		synchronized (client) {
			try {
				resp = client.execute(new HttpGet(url));

				final StatusLine status = resp.getStatusLine();
				if (status.getStatusCode() != 200) {
					Log.d(tag, "HTTP error, invalid server status code: " + resp.getStatusLine());
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
				return new String(baf.toByteArray());
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
				return null;
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			}
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
					pics[i].flags |= Frupic.FLAG_NEW;
					
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
		return pruneCache(this, cachesize);
	}
	
	public static String getCacheFileName(FrupicFactory factory, Frupic frupic, boolean thumb) {
		if (factory.external_cachedir != null && factory.prefer_external_cache)
			return factory.external_cachedir + File.separator + frupic.getFileName(thumb);
		else
			return factory.internal_cachedir + File.separator + frupic.getFileName(thumb);			
	}

	public CacheInfo pruneCache(FrupicFactory factory, int limit) {
		int total;
		int number = 0;
		File files1[];
		File files2[] = null;
		synchronized(client) {
			files1 = factory.internal_cachedir.listFiles((FilenameFilter) null);
		
			if (factory.external_cachedir != null)
				files2 = factory.external_cachedir.listFiles((FilenameFilter) null);
		}
		
		File files[] = new File[files1.length + ((files2 == null) ? 0 : files2.length)];
		System.arraycopy(files1, 0, files, 0, files1.length);
		if (files2 != null)
			System.arraycopy(files2, 0, files, files1.length, files2.length);		
		
		if (files.length == 0)
			return new CacheInfo(0, 0);


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
				Log.d(tag, "purged " + files[oldest].getName()
						+ " from filesystem");
				synchronized (client) {
					if (files[oldest].delete())
						total -= files[oldest].length();
				}
				number--;
				files[oldest] = null;
			}
		} while (total > limit);
		
		Log.i(tag, "left file cache populated with " + total + " bytes, " + number + " files");
		return new CacheInfo(total, number);
	}

	public String getCacheFileName(Frupic frupic, boolean thumb) {
		return getCacheFileName(this, frupic, thumb);
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
		File tmpFile = new File(getCacheFileName(frupic, fetch_thumb) + ".tmp");
		try {
			synchronized (client) {
				if (Thread.interrupted())
					return false;
				HttpUriRequest req;
				
				req = new HttpGet(fetch_thumb ? frupic.thumb_url : frupic.full_url);
				resp = client.execute(req);
	
				final StatusLine status = resp.getStatusLine();
				if (status.getStatusCode() != 200) {
					Log.d(tag, "HTTP error, invalid server status code: " + resp.getStatusLine());
					resp.getEntity().consumeContent();
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
					if (!tmpFile.delete()) {
						Log.e(tag, "error removing partly downloaded file "
							+ tmpFile.getName());
					}
					return false;
				}
				tmpFile.renameTo(frupic.getCachedFile(this, fetch_thumb));
			}
			return true;
		} catch (Exception e) {
			synchronized (client) {
				if (myOutput != null) {
					try {
						myOutput.flush();
						myOutput.close();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					myOutput = null;
				}
				
				if (!tmpFile.delete()) {
					Log.e(tag, "error removing partly downloaded file " + tmpFile.getName());
				}
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
		String filename = getCacheFileName(frupic, thumb);
		int ret;

		/* If file already in memory cache, return */

		/*
		 * XXX: if file is always kept in memory and it's lastModified time is
		 * never updated, it may be pruned from file system while still in memory.
		 */
		if (cache.get(getCacheFileName(frupic, thumb)) != null)
			return FROM_CACHE;	/* the picture was available before; don't notify again */

		File f = new File(filename);
		/* Fetch file from the Interweb, unless cached locally */
		if (!f.exists()) {
			if (! fetchFrupicImage(frupic, thumb, onProgress)) {
				return NOT_AVAILABLE;
			}
			ret = FROM_WEB;
			Log.d(tag, "Downloaded file " + frupic.id);
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
		Log.d(tag, "Loaded file to memory: " + frupic.id);
		cache.add(getCacheFileName(frupic, thumb), b);

		return ret;
	}

	public int fetchThumb(Frupic frupic) {
		return fetch(frupic, true, targetWidth, targetHeight, null);
	}

	public int fetchFull(Frupic frupic, OnFetchProgress onProgress) {
		return fetch(frupic, false, targetWidth, targetHeight, onProgress);
	}

	public Bitmap getThumbBitmap(Frupic frupic) {
		return cache.get(getCacheFileName(frupic, true));
	}

	public Bitmap getFullBitmap(Frupic frupic) {
		return cache.get(getCacheFileName(frupic, false));
	}
	
	public void clearCache() {
		cache.clear();
	}

}