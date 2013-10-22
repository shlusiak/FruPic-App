package de.saschahlusiak.frupic.grid;

import java.net.UnknownHostException;
import java.util.ArrayList;

import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class RefreshService extends Service {
    private final IBinder mBinder = new RefreshServiceBinder();
    
    FrupicFactory factory;
    Handler handler;
    ArrayList<OnRefreshListener> refreshListener;
    FrupicDB db;
    boolean isRefreshing = false;
    
	class RefreshThread extends Thread {
		String error;
		int base, count;
		FrupicDB db;
		
		public RefreshThread(int base, int count) {
			if (base < 0)
				base = 0;
			this.base = base;
			this.count = count;
			this.error = null;
		}

		@Override
		public void run() {
			Frupic pics[];
			try {
				db = new FrupicDB(RefreshService.this);
				db.open();
				pics = factory.fetchFrupicIndex(null, base, count);
				db.addFrupics(pics);
				db.close();
			} catch (UnknownHostException u) {
				pics = null;
				error = "Unknown host";
			} catch (Exception e) {
				pics = null;
				error = "Connection error";
				e.printStackTrace();
			}
			if (pics == null) {
				if (error != null) handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(RefreshService.this, error, Toast.LENGTH_LONG).show();
					}
				});
				
				/* increasing the frupic index failed, so to prevent the gridview to fire up another
				 * RefreshIndexTask right away, we just add a delay until we can detect network connectivity
				 */
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			handler.post(new Runnable() {
				@Override
				public void run() {
					isRefreshing = false;
					for (OnRefreshListener listener: refreshListener) {
						listener.OnRefreshFinished();
					}
				}
			});
		}
	}

    
    
    
    
    public interface OnRefreshListener {
    	public void OnRefreshStarted();
    	public void OnRefreshFinished();
    }

    public class RefreshServiceBinder extends Binder {
        RefreshService getService() {
            return RefreshService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
    	super.onCreate();
    	factory = new FrupicFactory(this);
    	handler = new Handler();
    	refreshListener = new ArrayList<OnRefreshListener>();
    	
		db = new FrupicDB(this);
		db.open();
    }
    
    @Override
    public void onDestroy() {
    	db.close();
    	super.onDestroy();
    }
    
    public void registerRefreshListener(OnRefreshListener listener) {
    	refreshListener.add(listener);
    }
    
    public void unregisterRefreshListener(OnRefreshListener listener) {
    	refreshListener.remove(listener);
    }
    
    public void requestRefresh(int base, int count) {
    	if (isRefreshing)
    		return;
		for (OnRefreshListener listener: refreshListener) {
			listener.OnRefreshStarted();
		}
		isRefreshing = true;
    	new RefreshThread(base, count).start();
    }
    
    public boolean isRefreshing() {
    	return isRefreshing;
    }

}
