package de.saschahlusiak.frupic.grid;

import java.util.LinkedList;
import java.util.Queue;

import android.opengl.Visibility;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import de.saschahlusiak.frupic.grid.FruPicGridAdapter.ViewHolder;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;

public class PreviewFetchTask extends Thread {
	private static final String tag = PreviewFetchTask.class.getSimpleName();

	ViewHolder viewHolder;
	Frupic frupic;
	FrupicFactory factory;
	Handler handler;
	boolean cancelled = false;

	PreviewFetchTask(ViewHolder viewHolder, FrupicFactory factory, Frupic frupic) {
		this.viewHolder = viewHolder;
		this.factory = factory;
		this.frupic = frupic;
		handler = new Handler();
		
		start();
	}
	
	public synchronized void cancel() {
		if (cancelled)
			return;
		cancelled = true;
		interrupt();
	}
	
	public synchronized boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void run() {
		if (isCancelled()) {
			return;
		}

		final int ret;

		ret = factory.fetchThumb(frupic);
		if (isCancelled())
			return;
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				viewHolder.fetchComplete(frupic, ret);
			}
		});
	}
}
