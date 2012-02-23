package de.saschahlusiak.frupic.model;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class FrupicFactory {
	public static Frupic[] getFrupics(String username, int offset, int limit,
			boolean thumbs) throws IOException {
		InputStream in = null;
		String queryResult = "";

		String s = "http://api.freamware.net/2.0/get.picture?";
		if (username != null)
			s += "username=" + username + "&";
		s = s + "offset=" + offset + "&limit=" + limit;
		URL url = new URL(s);
		HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
		HttpURLConnection httpConn = (HttpURLConnection) urlConn;
		httpConn.setAllowUserInteraction(false);
		httpConn.connect();
		in = httpConn.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(in);
		ByteArrayBuffer baf = new ByteArrayBuffer(50);
		int read = 0;
		int bufSize = 512;
		byte[] buffer = new byte[bufSize];
		while (true) {
			read = bis.read(buffer);
			if (read == -1) {
				break;
			}
			baf.append(buffer, 0, read);
		}
		queryResult = new String(baf.toByteArray());

		JSONArray array;
		try {
			array = new JSONArray(queryResult);
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

					pics[i].thumb = thumbs;
					pics[i].id = data.getInt("id");
					pics[i].url = data.getString("url");
					pics[i].date = data.getString("date");
					pics[i].username = data.getString("username");
				}
			}
			return pics;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Frupic[] getFrupics(int offset, int limit) throws IOException {
		return getFrupics(null, offset, limit, false);
	}

	public static Frupic[] getThumbPics(int offset, int limit) throws IOException {
		return getFrupics(null, offset, limit, true);
	}

}