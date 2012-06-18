package de.saschahlusiak.frupic.model;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.util.Log;

class FrupicCache {
	static private final String tag = FrupicCache.class.getSimpleName();
	
	class CacheItem {
		String url;
		Bitmap bitmap;
	}
	int memcount;
	
	ArrayList<CacheItem> cache;
	
	FrupicCache(int memcount) {
		this.memcount = memcount;
		cache = new ArrayList<CacheItem>(memcount);
	}
	
	synchronized void clear() {
		cache.clear();
	}
	
	synchronized Bitmap get(String url) {
		for (CacheItem c: cache)
			if (c.url.equals(url)) {
				/* we should push the requested file back to the top so it is purged last */
				cache.remove(c);
				cache.add(c);
				return c.bitmap;
			}
		return null;
	}
	
	synchronized Bitmap add(String url, Bitmap bitmap) {
		if (cache.size() >= memcount) {
			cache.remove(0);
		}
		CacheItem c = new CacheItem();
		c.url = url;
		c.bitmap = bitmap;
		cache.add(c);
//		Log.i(tag, "Memory cache size: " + cache.size());
		return bitmap;
	}

}
