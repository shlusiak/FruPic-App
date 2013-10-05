package de.saschahlusiak.frupic.utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import de.saschahlusiak.frupic.R;

public class UploadTask extends AsyncTask<Void, Integer, String> {
	private final String FruPicApi = "http://api.freamware.net/2.0/upload.picture";
	private final String tag = UploadTask.class.getSimpleName();

	public interface ProgressTaskActivityInterface {
		void updateProgressDialog(int progress, int max);
	}
	
	ProgressTaskActivityInterface activity;
	Context context;
	byte imageData[];
	String username;
	String frupicURL;
	String error;
	String tags;

	public UploadTask(byte imageData[], String username, String tags) {
		this.username = username;
		this.frupicURL = null;
		this.error = null;
		this.tags = tags;
		this.imageData = imageData;
	}

	public void setActivity(Context context, ProgressTaskActivityInterface activity) {
		this.context = context;
		this.activity = activity;
	}

	protected void onPreExecute() {
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		if (activity != null)
			activity.updateProgressDialog(values[0], values[1]);
	}

	@Override
	protected String doInBackground(Void... params) {
		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		DataInputStream dis;
		publishProgress(0, 1);

		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "ForeverFrubarIWantToBe";

		try {
			URL url = new URL(FruPicApi);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);

			conn.setUseCaches(false);
		//	conn.setChunkedStreamingMode(0);

			// begin the header
			// Log.d(TAG, "beginning with header");
			conn.setRequestMethod("POST");

			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type",
					"multipart/form-data;boundary=" + boundary);
			
			
			// Tags
			String header = lineEnd + twoHyphens + boundary + lineEnd +
							"Content-Disposition: form-data;name='tags';" +
							lineEnd + lineEnd + tags + ";" + lineEnd +
							lineEnd + twoHyphens + boundary + lineEnd;
			if (!username.equals("")) {
				header += lineEnd + twoHyphens + boundary + lineEnd;
				header += "Content-Disposition: form-data;name='username';";
				header += lineEnd + lineEnd + username + ";" + lineEnd +
						  lineEnd + twoHyphens + boundary + lineEnd;
			}
			header += "Content-Disposition: form-data;" + "name='file';" +
					  "filename='frup0rn.png'" + lineEnd + lineEnd;
			
			String footer = lineEnd + twoHyphens + boundary + twoHyphens + lineEnd;
			
//			conn.setRequestProperty("Content-Length", "" + (header.length() + footer.length() + imageData.length));
			conn.setFixedLengthStreamingMode(header.length() + footer.length() + imageData.length);
			

			OutputStream os = conn.getOutputStream();
			
			dos = new DataOutputStream(os);

			dos.writeBytes(header);

			InputStream is = new ByteArrayInputStream(imageData);
			int size = imageData.length;
			int nRead;
			byte data[] = new byte[16384];

			publishProgress(0, size);

			int written = 0;
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				dos.write(data, 0, nRead);
				written += nRead;
				dos.flush();
				os.flush();
				if (isCancelled()) {
					dos.close();
					conn.disconnect();
					return null;
				}
				publishProgress(written, size);
			}

			// Log.d(TAG, "FINISHED sending the image");

			// send multipart form data necesssary after file data...
			dos.writeBytes(footer);

			// close the stream
			// Log.d(TAG, "closing the stream");
			dos.flush();
			dos.close();
		} catch (IOException ioe) {
			conn.disconnect();
			ioe.printStackTrace();
			error = context.getString(R.string.cannot_connect);
			return error;
		}

		// listening to the Server Response
		// Log.d(TAG, "listening to the server");
		try {
			dis = new DataInputStream(conn.getInputStream());

			String str = "";
			String output = "";

			while ((str = dis.readLine()) != null) {
				output = output + str;
				// Log.d(TAG, output);

				// save the url to the image
				frupicURL = output;
				// Log.d(TAG, "the image url is: "+FruPic.imageURL);
			}
			dis.close();
		} catch (IOException ioex) {
			ioex.printStackTrace();
			error = context.getString(R.string.error_reading_response);
			return error;
			// Log.e(TAG, "Exception" , ioex);
		}
		Log.i(tag, "Upload successful: " + frupicURL);

		error = null;
		return null;
	}

}
