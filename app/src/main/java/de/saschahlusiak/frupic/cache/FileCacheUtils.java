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
	
	private File internal_cachedir, external_cachedir;
	private boolean prefer_external_cache;

	public FileCacheUtils(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefer_external_cache = true;

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
}
