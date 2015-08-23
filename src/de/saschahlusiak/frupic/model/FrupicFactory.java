package de.saschahlusiak.frupic.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import de.saschahlusiak.frupic.cache.BitmapCache;
import de.saschahlusiak.frupic.cache.FileCache;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.Uri;
import android.util.Log;

public class FrupicFactory {
	static final private String tag = FrupicFactory.class.getSimpleName();
	
	public static final int NOT_AVAILABLE = 0;
	public static final int FROM_CACHE = 1;
	public static final int FROM_FILE = 2;
	public static final int FROM_WEB = 3;
	
	private static final int NUM_CLIENTS = 5;

	Context context;
	BitmapCache cache;
	FileCache fileCache;
	int targetWidth, targetHeight;
	ArrayBlockingQueue<Object> tokens;

	public FrupicFactory(Context context, int bitmapCacheSize) {
		this.context = context;
		this.cache = new BitmapCache(bitmapCacheSize);
		targetWidth = 800;
		targetHeight = 800;
		tokens = new ArrayBlockingQueue<Object>(NUM_CLIENTS);
		for (int i = 0; i < NUM_CLIENTS; i++)
			tokens.add(new Object());
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


	
	private Bitmap decodeImageFile(String filename, int width, int height) {
		Bitmap b;
		File file;
		
		file = new File(filename);
		if (!file.exists() || !file.canRead())
			return null;

		b = BitmapFactory.decodeFile(filename);
		
		if (b == null) {
			Log.e("FruPic", "Error decoding image stream: " + file);
		}
		
		return b;
	}

	public interface OnFetchProgress {
		void OnProgress(int read, int length);
	}

	public boolean fetchFrupicImage(Frupic frupic, boolean fetch_thumb, OnFetchProgress progress) {
		OutputStream myOutput = null;
		File tmpFile = null;
		int copied;
		
		try {
			URL url = new URL(fetch_thumb ? frupic.thumb_url : frupic.full_url);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			if (Thread.interrupted())
				return false;

			long maxlength = connection.getContentLength();
			InputStream myInput = connection.getInputStream();
			
			int i = 0;
			synchronized (fileCache) {
				/* TODO: what to do when tmp file exists?
				 * wait till tmp file does not exist anymore? wait till
				 * frupic file exists and return successful?
				 * this just choses another tmp file and starts download
				 * resulting in two files being downloaded and then
				 * renamed to the frupic image file
				 */
				do {
					tmpFile = new File(fileCache.getFileName(frupic, fetch_thumb) + ".tmp." + i);
					i++;
				} while (tmpFile.exists());
			}

			myOutput = new FileOutputStream(tmpFile);
			byte[] buffer = new byte[16384];
			int length;
			copied = 0;
			while ((length = myInput.read(buffer)) > 0) {
				myOutput.write(buffer, 0, length);
				if (Thread.interrupted()) {
					myOutput.flush();
					myInput.close();
					myOutput.close();
					connection.disconnect();
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
			connection.disconnect();
			if (Thread.interrupted()) {
				if (!tmpFile.delete()) {
					Log.e(tag, "error removing partly downloaded file "
						+ tmpFile.getName());
				}
				return false;
			}
			synchronized(fileCache) {
				tmpFile.renameTo(fileCache.getFile(frupic, fetch_thumb));
			}
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
			
			if (tmpFile != null) {
				synchronized (fileCache) {
					if (!tmpFile.delete()) {
						Log.e(tag, "error removing partly downloaded file " + tmpFile.getName());
					}
				}
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
	public int fetch(Frupic frupic, boolean thumb, OnFetchProgress onProgress) {
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
		
		if (!thumb)
			return ret;

		/* Load downloaded file and add bitmap to memory cache */
		Bitmap b = decodeImageFile(filename, targetWidth, targetHeight);
		if ((b == null) || (Thread.interrupted())) {
			Log.d(tag, "Error loading to memory: " + frupic.id);
			return NOT_AVAILABLE;
		}
//		Log.d(tag, "Loaded file to memory: " + frupic.id);
		cache.add(fileCache.getFileName(frupic, thumb), b);

		return ret;
	}

	public int fetchFull(Frupic frupic, OnFetchProgress onProgress) {
		int ret;
		Object token;
		try {
			token = tokens.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return NOT_AVAILABLE;
		}
		ret = fetch(frupic, false, onProgress);
		tokens.add(token);
		return ret;
	}

	public Bitmap getThumbBitmap(Frupic frupic) {
		return cache.get(fileCache.getFileName(frupic, true));
	}

	public Bitmap getFullBitmap(Frupic frupic) {
		return cache.get(fileCache.getFileName(frupic, false));
	}
	
	public Uri getFullBitmapURI(Frupic frupic) {
		File file = fileCache.getFile(frupic, false);
		if (!file.exists()) return null;
		return Uri.fromFile(file);
	}
}