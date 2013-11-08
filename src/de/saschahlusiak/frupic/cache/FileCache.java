package de.saschahlusiak.frupic.cache;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.model.Frupic;

public class FileCache {
	private static final String tag = FileCache.class.getSimpleName();
	
	File internal_cachedir, external_cachedir;
	boolean prefer_external_cache;
	boolean always_keep_starred;
	int cachesize;
	Context context;

	public static class CacheInfo {
		int bytes;
		int files;
		int ignored;
		CacheInfo(int bytes, int files, int ignored) {
			this.bytes = bytes;
			this.files = files;
			this.ignored = ignored;
		}
		public int getCount() {
			return files;
		}
		public int getSize() {
			return bytes;
		}
		public int getIgnored() {
			return ignored;
		}
	}

	public FileCache(Context context) {
		this.context = context;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefer_external_cache = prefs.getBoolean("external_cache", true);
		always_keep_starred = prefs.getBoolean("always_keep_starred", true);
		cachesize = Integer.parseInt(prefs.getString("cache_size", "16777216"));

		this.internal_cachedir = context.getCacheDir();
		this.external_cachedir = context.getExternalCacheDir();
	}

	public String getFileName(Frupic frupic, boolean thumb) {
		if (external_cachedir != null && prefer_external_cache)
			return external_cachedir + File.separator + frupic.getFileName(thumb);
		else
			return internal_cachedir + File.separator + frupic.getFileName(thumb);			
	}
	
	public File getFile(Frupic frupic, boolean thumb) {
		return new File(getFileName(frupic, thumb));
	}
	
	public CacheInfo getCacheInfo() {
		return pruneCache(-1);
	}
	
	public CacheInfo pruneCache() {
		return pruneCache(cachesize);
	}
	
	public CacheInfo pruneCache(int limit) {
		int total;
		int number = 0;
		File files1[];
		File files2[] = null;
		HashMap<String, String> skipFiles = new HashMap<String, String>();
		long time = System.currentTimeMillis();
		synchronized(this) {
			files1 = internal_cachedir.listFiles((FilenameFilter) null);
		
			if (external_cachedir != null)
				files2 = external_cachedir.listFiles((FilenameFilter) null);
		}
		
		File files[] = new File[files1.length + ((files2 == null) ? 0 : files2.length)];
		System.arraycopy(files1, 0, files, 0, files1.length);
		if (files2 != null)
			System.arraycopy(files2, 0, files, files1.length, files2.length);		

		if (files.length == 0)
			return new CacheInfo(0, 0, 0);

		if (always_keep_starred) {
			FrupicDB db = new FrupicDB(context);
			db.open();
			Cursor cursor = db.getFrupics(null, Frupic.FLAG_FAV);
			if (cursor != null) {
				cursor.moveToFirst();
				Frupic frupic = new Frupic();
				do {
					frupic.fromCursor(cursor);
					String filename;
					
					filename = getFileName(frupic, false);
					skipFiles.put(filename, filename);
					
					filename = getFileName(frupic, true);
					skipFiles.put(filename, filename);
				} while (cursor.moveToNext());
				cursor.close();
			}
			db.close();
		}
		Log.d(tag, "counting files took " + (System.currentTimeMillis() - time) + " ms");
		time = System.currentTimeMillis();

		do {
			int oldest = -1;
			total = 0;
			number = 0;
			long oldestTimeStamp = 0;

			for (int i = 0; i < files.length; i++) {
				if (files[i] == null)
					continue;
				if (files[i].getName().contains(".tmp.")) {
					files[i] = null;
					continue;
				}
				if (skipFiles.containsKey(files[i].getAbsolutePath()))
					continue;

				number++;
				if ((oldest < 0) || (files[i].lastModified() < oldestTimeStamp)) {
					oldest = i;
					oldestTimeStamp = files[i].lastModified();
				}
				total += files[i].length();
			}

			if (limit < 0)
				break;
			
			if (total > limit) {
				Log.d(tag, "purged " + files[oldest].getName()
						+ " from filesystem");
				synchronized (this) {
					long l = files[oldest].length();
					if (files[oldest].delete())
						total -= l;
				}
				number--;
				files[oldest] = null;
			}
		} while (total > limit);
		Log.d(tag, "purged cache in " + (System.currentTimeMillis() - time) + " ms");
		
		Log.i(tag, "left file cache populated with " + total + " bytes, " + number + " files");
		/* FIXME */
		return new CacheInfo(total, number, 0);
	}

}
