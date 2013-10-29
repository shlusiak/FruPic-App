package de.saschahlusiak.frupic.grid;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.model.Frupic;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class RefreshService extends Service {
    private final IBinder mBinder = new RefreshServiceBinder();
    
	static final String INDEX_URL = "http://api.freamware.net/2.0/get.picture";
	static final String tag = RefreshService.class.getSimpleName();

    Handler handler;
    ArrayList<OnRefreshListener> refreshListener;
    FrupicDB db;
    boolean isRefreshing = false;
    DefaultHttpClient client = new DefaultHttpClient();
    ConnectivityManager cm;

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
		
		private String fetchURL(String url) throws IOException {
			InputStream in = null;
			HttpResponse resp;
			String result = null;
			
			resp = client.execute(new HttpGet(url));

			final StatusLine status = resp.getStatusLine();
			if (status.getStatusCode() != 200) {
				Log.d(tag, "HTTP error, invalid server status code: " + resp.getStatusLine());
				return null;
			}

			in = resp.getEntity().getContent();
		
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 1024;
			byte[] buffer = new byte[bufSize];
			while ((read = in.read(buffer)) > 0) {
				baf.append(buffer, 0, read);
			}
			in.close();
			result = new String(baf.toByteArray());
			return result;
			
		}
		
		private Frupic[] getFrupicIndexFromString(String string) {
			JSONArray array;
			try {
				array = new JSONArray(string);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
			if (array.length() < 1)
				return null;

			try {
				Frupic pics[] = new Frupic[array.length()];
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.optJSONObject(i);
					if (data != null) {
						pics[i] = new Frupic();

						pics[i].thumb_url = data.getString("thumb_url");
						pics[i].id = data.getInt("id");
						pics[i].full_url = data.getString("url");
						pics[i].date = data.getString("date");
						pics[i].username = data.getString("username");
						pics[i].flags |= Frupic.FLAG_NEW | Frupic.FLAG_UNSEEN;
						
						JSONArray tags = data.getJSONArray("tags");
						if ((tags != null) && (tags.length() > 0)) {
							pics[i].tags = new String[tags.length()];
							for (int j = 0; j < tags.length(); j++)
								pics[i].tags[j] = tags.getString(j);
						}
					}
				}
				return pics;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}	
		}

		public Frupic[] fetchFrupicIndex(String username, int offset, int limit)
				throws IOException {

			String s = INDEX_URL + "?";
			if (username != null)
				s += "username=" + username + "&";
			s = s + "offset=" + offset + "&limit=" + limit;
			
			String queryResult = fetchURL(s);
			if (queryResult == null || "".equals(queryResult))
				return null;
			
			if (Thread.interrupted())
				return null;
			
			return getFrupicIndexFromString(queryResult);
		}


		@Override
		public void run() {
			Frupic pics[];
			db = new FrupicDB(RefreshService.this);
			db.open();
			try {
				pics = fetchFrupicIndex(null, base, count);
				if (pics != null)
					db.addFrupics(pics);
			} catch (UnknownHostException u) {
				pics = null;
				error = "Unknown host";
			} catch (Exception e) {
				pics = null;
				error = "Connection error";
				e.printStackTrace();
			}
			db.close();
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
    	handler = new Handler();
    	refreshListener = new ArrayList<OnRefreshListener>();
    	
		db = new FrupicDB(this);
		db.open();
		
	    cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE); 
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
    	
    	NetworkInfo ni = cm.getActiveNetworkInfo();
    	if (ni == null)
    		return;
    	if (!ni.isConnected())
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
