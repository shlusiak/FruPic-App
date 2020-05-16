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

@Deprecated
public class FileCacheUtils {
	private static final String tag = FileCacheUtils.class.getSimpleName();
	
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

	public FileCacheUtils(Context context) {
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
	
	private static class CacheFileInfo {
		public long size;
		public long modified;
		
		public CacheFileInfo(long size, long modified) {
			this.size = size;
			this.modified = modified;
		}
	}
	
	public CacheInfo pruneCache(int limit) {
		int total;
		int number = 0;
		HashMap<String, String> skipFiles = new HashMap<String, String>();
		long time = System.currentTimeMillis();
		File files[];
		HashMap<File, CacheFileInfo> dateMap = new HashMap<File, CacheFileInfo>();
		
		synchronized(this) {
			File files1[] = null;
			File files2[] = null;
			files1 = internal_cachedir.listFiles((FilenameFilter) null);
			
			if (external_cachedir != null)
				files2 = external_cachedir.listFiles((FilenameFilter) null);
			
			// merge internal_cachedir and external_cachedir file lists
			files = new File[files1.length + ((files2 == null) ? 0 : files2.length)];
			System.arraycopy(files1, 0, files, 0, files1.length);
			if (files2 != null)
				System.arraycopy(files2, 0, files, files1.length, files2.length);		
		}

		if (files.length == 0)
			return new CacheInfo(0, 0, 0);

		if (always_keep_starred) {
			FrupicDB db = new FrupicDB(context);
			db.open();
			Cursor cursor = db.getFrupics(null, Frupic.FLAG_FAV);
			if (cursor != null) {
				cursor.moveToFirst();
				do {
					Frupic frupic = new Frupic(cursor);
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
		
		for (int i = 0; i < files.length; i++) {
			dateMap.put(files[i], new CacheFileInfo(files[i].length(), files[i].lastModified()));
		}

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
				if ((oldest < 0) || (dateMap.get(files[i]).modified < oldestTimeStamp)) {
					oldest = i;
					oldestTimeStamp = dateMap.get(files[i]).modified;
				}
				total += dateMap.get(files[i]).size;
			}

			if (limit < 0)
				break;

			if (total > limit) {
				synchronized (this) {
					long l = dateMap.get(files[oldest]).size;
					if (files[oldest].delete())
						total -= l;
				}
				Log.d(tag, "purged " + files[oldest].getName() + " from filesystem");
				number--;
				files[oldest] = null;
			}
		} while (total > limit);
		Log.d(tag, "purged cache in " + (System.currentTimeMillis() - time) + " ms");
		
		Log.i(tag, String.format("left file cache populated with %.1f Mb (%.1f %%), %d files", total / 1024.0f / 1024.0f, 100.0f * total / limit, number));
		/* FIXME */
		return new CacheInfo(total, number, 0);
	}
}
