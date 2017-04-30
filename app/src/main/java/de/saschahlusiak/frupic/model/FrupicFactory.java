package de.saschahlusiak.frupic.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import de.saschahlusiak.frupic.cache.FileCacheUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class FrupicFactory {
	static final private String tag = FrupicFactory.class.getSimpleName();

	Context context;
	FileCacheUtils fileCache;

	public interface OnFetchProgress {
		void OnProgress(int read, int length);
	}

	public FrupicFactory(Context context) {
		this.context = context;
		recreateFileCache();
	}
	
	public void recreateFileCache() {
		fileCache = new FileCacheUtils(context);
	}
	
	/**
	 * Returns a File object for the cached file on storage. File may not exist.
	 * @param frupic
	 * @param thumb
	 * @return File for cached file
	 */
	public File getCacheFile(Frupic frupic, boolean thumb) {
		return fileCache.getFile(frupic, thumb);
	}
	
	/**
	 * Either decodes the Bitmap from file cache or downloads it first and then decodes it
	 * @param frupic
	 * @param onProgress
	 * @return loaded Bitmap or null on error
	 */
	public Bitmap getThumbnail(Frupic frupic, OnFetchProgress onProgress) {
		String filename = fileCache.getFileName(frupic, true);
		Bitmap b;

		File f = new File(filename);
		
		/* Fetch file from the Interweb, unless cached locally */
		if (!f.exists()) {
			try {
				if (! fetchFrupicImage(frupic, true, onProgress)) {
					return null;
				}
			} catch (InterruptedException e) {
				return null;
			}
//			Log.d(tag, "Downloaded file " + frupic.id);
		}
		
		if (Thread.interrupted())
			return null;
		
		/* touch file, so it is purged from cache last */
		f.setLastModified(new Date().getTime());

		b = BitmapFactory.decodeFile(filename);

		return b;
	}

	/**
	 * Downloads the given image into file cache
	 * 
	 * @param frupic
	 * @param fetch_thumb
	 * @param progress
	 * @return true on success
	 */
	public boolean fetchFrupicImage(Frupic frupic, boolean fetch_thumb, OnFetchProgress progress) throws InterruptedException {
		OutputStream myOutput = null;
		File tmpFile = null;
		int copied;
		
		try {
			URL url = new URL(fetch_thumb ? frupic.thumb_url : frupic.full_url);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			if (Thread.interrupted())
				throw new InterruptedException();

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
			byte[] buffer = new byte[8192];
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
						Log.e(tag, "error removing partly downloaded file " + tmpFile.getName());
					}
					throw new InterruptedException();
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
					Log.e(tag, "error removing partly downloaded file " + tmpFile.getName());
				}
				throw new InterruptedException();
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

	/**
	 * copies a file from in to out
	 * 
	 * @param in
	 * @param out
	 * @return true on success
	 */
	public static synchronized boolean copyImageFile(File in, File out) {
		if (out.exists())
			return true;
		
		try {
			InputStream is = new FileInputStream(in);
			OutputStream os = new FileOutputStream(out);

			byte[] buffer = new byte[4096];
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
}