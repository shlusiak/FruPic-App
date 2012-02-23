package de.saschahlusiak.frupic.model;


import java.io.InputStream;
import java.net.HttpURLConnection;

import java.net.URL;
import java.util.ArrayList;

import java.util.Stack;

import de.saschahlusiak.frupic.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class FruPicContainer {
	class CacheItem {
		String url;
		Bitmap bitmap;
	}
	
	ArrayList<CacheItem> cache;
	int cachesize;

	public class Job {
		String url;
		View view;

		Job(String url, View view) {
			this.url = url;
			this.view = view;
		}
	}
	
	public void clear() {
		cache.clear();
	}

	Stack<Job> jobs = new Stack<Job>();

	void postJob(Job job) {
		synchronized (jobs) {
			for (Job j : jobs) {
				if (j.url.equals(job.url)) {
					j.view = job.view;
					return;
				}
			}
			Log.w("Jobs", "post job: " + job.url);
			jobs.add(job);
			jobs.notify();
		}
	}

	class Loader extends Thread {
		public void run() {
			while (!isInterrupted()) {
				final Job j;
				synchronized (jobs) {
					while (jobs.isEmpty())
						try {
							jobs.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							return;
						}
					j = jobs.pop();
				}
				if (j.view != null)
					j.view.post(new Runnable() {

						@Override
						public void run() {
							ProgressBar p = (ProgressBar) j.view
									.findViewById(R.id.progressBar);
							if (p == null)
								return;
							p.setVisibility(View.VISIBLE);
						}
					});
				

				Bitmap b;
				try {
					URL url = new URL(j.url);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setUseCaches(true);
					
					Object response = connection.getContent();
					if (response instanceof Bitmap) {
					  b = (Bitmap)response;
					} else {
						b = BitmapFactory.decodeStream((InputStream)response);
					}
				} catch (Exception e) {
					e.printStackTrace();
					if (j.view != null)
					j.view.post(new Runnable() {

						@Override
						public void run() {
							ProgressBar p = (ProgressBar) j.view
									.findViewById(R.id.progressBar);
							if (p == null)
								return;
							p.setVisibility(View.INVISIBLE);
						}
					});
					continue;
				}
				final Bitmap b2 = b;
				b = null;

				synchronized (cache) {
					while (cache.size() >= cachesize) {
						cache.remove(0);
					}
					CacheItem item = new CacheItem();
					item.url = j.url;
					item.bitmap = b2;
					cache.add(item);
				}
				Log.w("Job", "job done (cache: #" + cache.size() + ": " + j.url);

				if (j.view != null) {
					j.view.post(new Runnable() {

						@Override
						public void run() {
							if (j.view.getTag() == null) {
								Log.e("JOB", "Not updating view; tag == null");
								return;
							}
							if (j.view.getTag().equals(j.url)) {
								((ImageView) j.view
										.findViewById(R.id.imageView))
										.setImageBitmap(b2);
								ProgressBar p = (ProgressBar) j.view
									.findViewById(R.id.progressBar);
								if (p != null)
									p.setVisibility(View.INVISIBLE);
							}
							else
								Log.w("JOB", "BASE CHANGED");
						}
					});
				}
			}
		}

	}

	Loader loader[];

	public FruPicContainer(int cachesize, int loaders) {
		cache = new ArrayList<CacheItem>(cachesize);
		this.cachesize = cachesize;
		loader = new Loader[loaders];
		for (int i = 0; i < loader.length; i++) {
			loader[i] = new Loader();
			loader[i].start();
		}
	}

	public void fetchImage(Frupic frupic, View view) {
		String url;
		if (frupic == null)
			return;
		
		url = frupic.getUrl();

		synchronized (cache) {
			for (CacheItem item: cache) {
				if (item.url.equals(url)) {
					if (view != null) {
						Log.w("Jobs", "using cache: " + url);

						((ImageView) view.findViewById(R.id.imageView)).setImageBitmap(item.bitmap);
						ProgressBar p = (ProgressBar) view.findViewById(R.id.progressBar);
						if (p != null)
								p.setVisibility(View.INVISIBLE);
						view.setTag(null);
					}
					return;
				}
			}
		}
		if (view != null) {
			ImageView image = (ImageView)view.findViewById(R.id.imageView);
			image.setImageResource(R.drawable.skylime);
			view.setTag(url);
		}
		// new ImageLoadTask().execute(view);
		postJob(new Job(url, view));
	}
}
