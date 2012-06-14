package de.saschahlusiak.frupic.grid;

import java.util.LinkedList;
import java.util.Queue;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;

public class PreviewFetchTask extends Thread {
	private static final String tag = PreviewFetchTask.class.getSimpleName();

	Frupic frupic;
	FrupicFactory factory;
	FruPicGridAdapter adapter;
	static Queue<PreviewFetchTask> queue = new LinkedList<PreviewFetchTask>();
	static PreviewFetchTask currentTask = null;
	Handler handler;
	boolean cancelled = false;

	PreviewFetchTask(FruPicGridAdapter adapter, FrupicFactory factory, Frupic frupic) {
		this.frupic = frupic;
		this.factory = factory;
		this.adapter = adapter;
		handler = new Handler();
		
		if (currentTask == null) {
			currentTask = this;
			start();
		} else {
			queue.add(this);
		}
	}
	
	public synchronized void cancel() {
		cancelled = true;
//		interrupt();
	}
	
	public synchronized boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void run() {
		if (isCancelled()) {
			startNext();
			return;
		}

		int ret;

		startLoadingAnimation();
		ret = factory.fetchThumb(frupic);
		if (isCancelled()) {
			startNext();
			return;
		}

		switch (ret) {
		case FrupicFactory.NOT_AVAILABLE:
			Log.e(tag, "fetchThumb returned NOT_AVAILABLE");
			// cancel(false);
			break;
		case FrupicFactory.FROM_CACHE:
			break;
		case FrupicFactory.FROM_FILE:
		case FrupicFactory.FROM_WEB:
			break;
		}
		onPostExecute();
	}

	void startLoadingAnimation() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				FruPicGridAdapter.ViewHolder holder = (FruPicGridAdapter.ViewHolder) frupic.getTag();
				if (holder.frupic == frupic
						&& holder.task == PreviewFetchTask.this) {
					Animation a = new AlphaAnimation(1.0f, 0.4f);
					a.setDuration(350);
					a.setRepeatMode(Animation.REVERSE);
					a.setRepeatCount(Animation.INFINITE);
					a.setInterpolator(new AccelerateDecelerateInterpolator());
					holder.image1.startAnimation(a);
				}
			}
		});
	}
	
	void startNext() {
		currentTask = queue.poll();
		if (currentTask != null)
			currentTask.start();
	}

	void onPostExecute() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				FruPicGridAdapter.ViewHolder holder = (FruPicGridAdapter.ViewHolder) frupic.getTag();
				if (holder.frupic == frupic && holder.task == PreviewFetchTask.this) {
					Animation a1, a2;
					a1 = new ScaleAnimation(1.0f, 0.0f, 1.0f, 1.0f,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					a1.setDuration(150);
					a1.setFillAfter(true);
					a1.setInterpolator(new AccelerateDecelerateInterpolator());
		
					a2 = new ScaleAnimation(0.0f, 1.0f, 1.0f, 1.0f,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					a2.setDuration(150);
					a2.setStartOffset(150);
					a2.setInterpolator(new AccelerateDecelerateInterpolator());
		
					holder.image1.clearAnimation();
					holder.image1.startAnimation(a1);
					holder.image2.startAnimation(a2);
					holder.image2.setVisibility(View.VISIBLE);
					holder.task = null;
				}
				adapter.notifyDataSetChanged();
			}
		});
		startNext();
	}
}
