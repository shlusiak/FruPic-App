package de.saschahlusiak.frupic.cache;

import java.util.ArrayList;
import android.graphics.Bitmap;

public class BitmapCache<Key> {
	class CacheItem {
		Key key;
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
	
	synchronized public Bitmap get(Key key) {
		for (CacheItem c: cache)
			if (c.key.equals(key)) {
				/* we should push the requested file back to the top so it is purged last */
				cache.remove(c);
				cache.add(c);
				return c.bitmap;
			}
		return null;
	}
	
	synchronized public Bitmap add(Key key, Bitmap bitmap) {
		if (cache.size() >= memcount) {
			cache.remove(0);
		}
		CacheItem c = new CacheItem();
		c.key= key;
		c.bitmap = bitmap;
		cache.add(c);
		return bitmap;
	}

}
