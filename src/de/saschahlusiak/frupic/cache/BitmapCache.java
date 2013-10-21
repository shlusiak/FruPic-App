package de.saschahlusiak.frupic.cache;

import java.util.ArrayList;
import android.graphics.Bitmap;

public class BitmapCache {
	class CacheItem {
		String url;
		Bitmap bitmap;
	}
	int memcount;
	
	ArrayList<CacheItem> cache;
	
	public BitmapCache(int memcount) {
		this.memcount = memcount;
		cache = new ArrayList<CacheItem>(memcount);
	}
	
	synchronized public void clear() {
		cache.clear();
	}
	
	synchronized public Bitmap get(String url) {
		for (CacheItem c: cache)
			if (c.url.equals(url)) {
				/* we should push the requested file back to the top so it is purged last */
				cache.remove(c);
				cache.add(c);
				return c.bitmap;
			}
		return null;
	}
	
	synchronized public Bitmap add(String url, Bitmap bitmap) {
		if (cache.size() >= memcount) {
			cache.remove(0);
		}
		CacheItem c = new CacheItem();
		c.url = url;
		c.bitmap = bitmap;
		cache.add(c);
		return bitmap;
	}

}
