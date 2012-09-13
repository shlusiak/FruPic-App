package de.saschahlusiak.frupic.gallery;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.model.FrupicFactory.OnFetchProgress;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class GalleryPagerAdapter extends PagerAdapter {
	private final static String tag = GalleryPagerAdapter.class.getSimpleName();

	FrupicFactory factory;
	FruPicGallery context;
	Cursor cursor;
	
	class FetchTask extends Thread implements OnFetchProgress {
		Frupic frupic;
		View view;
		boolean cancelled = false;
		ProgressBar progress;

		FetchTask(final View view, final Frupic frupic) {
			this.view = view;
			this.frupic = frupic;
			final ImageButton stopButton = (ImageButton)view.findViewById(R.id.stopButton);
			
			progress = (ProgressBar)view.findViewById(R.id.progressBar);
			progress.setIndeterminate(false);
			progress.setMax(100);
			progress.setProgress(0);
			progress.setVisibility(View.VISIBLE);
			
			stopButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
			stopButton.setVisibility(View.VISIBLE);
			stopButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (isCancelled()) {
						Thread t = new FetchTask(view, frupic);
						view.setTag(t);
						t.start();
					} else {
						progress.setProgress(0);
						stopButton.setImageResource(android.R.drawable.ic_menu_revert);
						cancel();
					}
				}
			});
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

			ret = factory.fetchFull(frupic, this);
			if (isCancelled())
				return;

			view.post(new Runnable() {
				@Override
				public void run() {
					ImageView i = (ImageView)view.findViewById(R.id.imageView);
					
					Bitmap b = factory.getFullBitmap(frupic);

					if (b != null) {
						i.setImageBitmap(b);
					} else {
						i.setImageResource(R.drawable.broken_frupic);
					}
					progress.setVisibility(View.GONE);
					view.findViewById(R.id.stopButton).setVisibility(View.GONE);
				}
			});
		}

		@Override
		public void OnProgress(final int read, final int length) {
			view.post(new Runnable() {
				@Override
				public void run() {
					if (isCancelled())
						return;
					progress.setProgress((100 * read) / length);
				}
			});
		}
	}
	
	public GalleryPagerAdapter(FruPicGallery context, FrupicFactory factory) {
		this.context = context;
		this.factory = factory;
	}

	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		Log.w(tag, "instantiateItem(" + position + ")");
		cursor.moveToPosition(position);

		Frupic frupic = new Frupic(cursor);

		View view = LayoutInflater.from(context).inflate(R.layout.gallery_item, container, false);
		ImageView i = (ImageView)view.findViewById(R.id.imageView);
		context.registerForContextMenu(i);
		i.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				context.showControls();
			}
		});
		
		Bitmap b = factory.getFullBitmap(frupic);

		if (b == null) {
			i.setImageResource(R.drawable.frupic);
			Thread t = new FetchTask(view, frupic);
			view.setTag(t);
			t.start();
		} else {
			view.findViewById(R.id.progressBar).setVisibility(View.GONE);
			view.findViewById(R.id.stopButton).setVisibility(View.GONE);
			i.setImageBitmap(b);
		}
		
		container.addView(view);
		return view;
	}
	
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		View view = (View)object;
		FetchTask t = (FetchTask)view.getTag();
		if (t != null)
			t.cancel();
		container.removeView(view);
	}

	@Override
	public int getCount() {
		return cursor.getCount();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

}
