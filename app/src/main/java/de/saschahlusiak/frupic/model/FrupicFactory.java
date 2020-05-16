package de.saschahlusiak.frupic.model;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.saschahlusiak.frupic.cache.FileCacheUtils;

public class FrupicFactory {
	static final private String tag = FrupicFactory.class.getSimpleName();

	private Context context;
	private FileCacheUtils fileCache;

	public interface OnFetchProgress {
		void OnProgress(int read, int length);
	}

	public FrupicFactory(Context context) {
		this.context = context;
		recreateFileCache();
	}
	
	private void recreateFileCache() {
		fileCache = new FileCacheUtils(context);
	}
	
	/**
	 * Returns a File object for the cached file on storage. File may not exist.
	 * @param frupic
	 * @return File for cached file
	 */
	public File getCacheFile(Frupic frupic) {
		return fileCache.getFile(frupic);
	}

	/**
	 * Downloads the given image into file cache
	 * 
	 * @param frupic
	 * @param progress
	 * @return true on success
	 */
	public boolean fetchFrupicImage(Frupic frupic, OnFetchProgress progress) throws InterruptedException {
		OutputStream myOutput = null;
		File tmpFile = null;
		int copied;
		
		try {
			URL url = new URL(frupic.getFullUrl());
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
					tmpFile = new File(fileCache.getFileName(frupic) + ".tmp." + i);
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
				tmpFile.renameTo(fileCache.getFile(frupic));
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