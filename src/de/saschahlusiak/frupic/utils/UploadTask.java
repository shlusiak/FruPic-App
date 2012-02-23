package de.saschahlusiak.frupic.utils;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import de.saschahlusiak.frupic.R;

public class UploadTask extends AsyncTask<Void, Integer, Void> {
	private final String FruPicApi = "http://api.freamware.net/2.0/upload.picture";
	private final String tag = UploadTask.class.getSimpleName();

	ProgressTaskActivityInterface activity;
	Context context;
	byte imageData[];
	String username;
	String frupicURL;
	String error;
	String tags;

	public UploadTask(byte imageData[], String username, String tags)
			throws FileNotFoundException {
		this.username = username;
		this.frupicURL = null;
		this.error = null;
		this.tags = tags;
		this.imageData = imageData;
	}

	public void setActivity(Context context,
			ProgressTaskActivityInterface activity) {
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
	protected Void doInBackground(Void... params) {
		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		publishProgress(0, 5);

		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "ForeverFrubarIWantToBe";

		try {
			URL url = new URL(FruPicApi);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);

			conn.setUseCaches(false);
			conn.setChunkedStreamingMode(0);

			publishProgress(1, 5);

			// begin the header
			// Log.d(TAG, "beginning with header");
			conn.setRequestMethod("POST");

			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type",
					"multipart/form-data;boundary=" + boundary);

			OutputStream os = conn.getOutputStream();
			dos = new DataOutputStream(os);
			publishProgress(2, 5);

			// Tags
			dos.writeBytes(lineEnd + twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data;name='tags';");
			dos.writeBytes(lineEnd + lineEnd + tags + ";" + lineEnd
					+ lineEnd + twoHyphens + boundary + lineEnd);

			publishProgress(3, 5);

			if (!username.equals("")) {
				dos.writeBytes(lineEnd + twoHyphens + boundary + lineEnd);
				dos
						.writeBytes("Content-Disposition: form-data;name='username';");
				dos.writeBytes(lineEnd + lineEnd + username + ";" + lineEnd
						+ lineEnd + twoHyphens + boundary + lineEnd);
			}

			dos.writeBytes("Content-Disposition: form-data;" + "name='file';"
					+ "filename='frup0rn.png'" + lineEnd);
			dos.writeBytes(lineEnd);

			publishProgress(4, 5);

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
			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			// close the stream
			// Log.d(TAG, "closing the stream");
			dos.flush();
			dos.close();
		} catch (IOException ioe) {
			conn.disconnect();
			ioe.printStackTrace();
			cancel(false);
			error = context.getString(R.string.cannot_connect);
			return null;
		}

		// listening to the Server Response
		// Log.d(TAG, "listening to the server");
		DataInputStream dis;
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
			cancel(false);
			error = context.getString(R.string.error_reading_response);
			return null;
			// Log.e(TAG, "Exception" , ioex);
		}
		Log.i(tag, "Upload successful: " + frupicURL);

		error = null;
		return null;
	}

	protected void onCancelled() {
		if (activity != null)
			activity.dismiss();

		if (context != null) {
			if (error != null)
				Toast.makeText(context, error, Toast.LENGTH_LONG).show();
			else
				Toast.makeText(context, "Generic error", Toast.LENGTH_LONG).show();
		}
	}

	protected void onPostExecute(Void result) {
		if (activity != null) {
			activity.success();
			activity.dismiss();
		}
	}
}
