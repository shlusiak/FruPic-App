package de.saschahlusiak.frupic.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import de.saschahlusiak.frupic.db.FrupicDB;
import de.saschahlusiak.frupic.model.Frupic;

public class RefreshJob extends Job {
	static final String INDEX_URL = "https://api.freamware.net/2.0/get.picture";

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
		StringBuilder sb = new StringBuilder();

		in = new URL(url).openStream();
	
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String r;
		while ((r = reader.readLine()) != null) {
			sb.append(r);
		}
		in.close();
		return sb.toString();		
	}
	
	private Frupic[] getFrupicIndexFromString(String string) {
		JSONArray array;
		try {
			array = new JSONArray(string);
			Log.d(RefreshJob.class.getSimpleName(), "loaded index with " + array.length() + " frupics");
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

	public Frupic[] fetchFrupicIndex(String username, int offset, int limit) throws IOException {
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
		} catch (IOException e) {
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
