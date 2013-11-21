package de.saschahlusiak.frupic.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.model.Frupic;

public class RefreshJob extends Job {
	static final String INDEX_URL = "http://api.freamware.net/2.0/get.picture";

	int base, count;
	
    FrupicDB db;
    String error;
    int lastNewCount = 0;

    RefreshJob(Context context) {
    	db = new FrupicDB(context);
    }
	
	public void setRange(int base, int count) {
		if (base < 0)
			base = 0;
		this.base = base;
		this.count = count;
	}
	
	public String getError() {
		return error;
	}
	
	private String fetchURL(String url) throws IOException {
		InputStream in = null;
		HttpResponse resp;
		String result = null;
		
		resp = httpClient.execute(new HttpGet(url));

		final StatusLine status = resp.getStatusLine();
		if (status.getStatusCode() != 200) {
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
	JobState run() {
		Frupic pics[];
		error = null;
		db.open();
		lastNewCount = 0;
		try {
			pics = fetchFrupicIndex(null, base, count);
			if (pics != null) {
				db.addFrupics(pics);
				for (int i = 0; i < pics.length; i++)
					if (pics[i] != null)
						lastNewCount++;
			}
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
			if (error != null)
				return JobState.JOB_FAILED;
		}

		return JobState.JOB_SUCCESS;
	}
	
	public int getLastCount() {
		return lastNewCount;
	}
}
