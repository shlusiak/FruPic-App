package de.saschahlusiak.frupic.grid;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.services.FetchThumbnailJob;
import de.saschahlusiak.frupic.services.Job;
import de.saschahlusiak.frupic.services.Job.OnJobListener;
import de.saschahlusiak.frupic.services.Job.Priority;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.CursorAdapter;
import android.widget.ImageView;


public class FruPicGridAdapter extends CursorAdapter {
	private static final String tag = FruPicGridAdapter.class.getSimpleName();
	FruPicGrid activity;
	FrupicFactory factory;
	LruCache<Integer, Bitmap> cache;
	
	public class ViewHolder implements OnJobListener {
		Frupic frupic;
		ImageView image1, image2, imageLabel;
		FetchThumbnailJob job;
		
		ViewHolder(View convertView) {
			image1 = (ImageView)convertView.findViewById(R.id.imageView1);
			image2 = (ImageView)convertView.findViewById(R.id.imageView2);
			imageLabel = (ImageView)convertView.findViewById(R.id.imageLabel);
			job = null;
		}
		
		void setFrupic(Frupic frupic) {
			if (frupic.hasFlag(Frupic.FLAG_FAV)) {
				imageLabel.setVisibility(View.VISIBLE);
				imageLabel.setImageResource(R.drawable.star_label);
			} else if (frupic.hasFlag(Frupic.FLAG_NEW)) {
				imageLabel.setVisibility(View.VISIBLE);
				imageLabel.setImageResource(R.drawable.new_label);
			} else
				imageLabel.setVisibility(View.INVISIBLE);
			
			if ((this.frupic != null) && (this.frupic.getId() == frupic.getId()))
				return;
			this.frupic = frupic;
			if (job != null)
				job.cancel();
			job = null;
			
			Bitmap b = cache.get(frupic.id);
			if (b != null) {
				/* bitmap is in cache but view is apparently reused.
				 * view could be before, after or in transition. set to "after"
				 */
				image1.clearAnimation();
				image2.clearAnimation();
				image1.setVisibility(View.INVISIBLE);
				image2.setVisibility(View.VISIBLE);
				setImage(b);
				return;
			}

			/* Item is not in cache, set up waiting and start task */
			image1.setImageResource(R.drawable.frupic);
			image2.setImageResource(R.drawable.frupic);

			image1.clearAnimation();
			image2.clearAnimation();
			image1.setVisibility(View.VISIBLE);
			image2.setVisibility(View.INVISIBLE);

			if (activity.jobManagerConnection.jobManager == null) {
				this.frupic = null;
				return;
			}

			job = new FetchThumbnailJob(frupic, factory);
			job.addJobListener(this);
			activity.jobManagerConnection.jobManager.post(job, Priority.PRIORITY_HIGH);
		}
		
		void setImage(Bitmap b) {
			image2.setImageBitmap(b);
		}
		
		void startFadeAnimation() {
			Animation a1, a2;
			a1 = new AlphaAnimation(1.0f, 0.0f);
			a1.setDuration(400);
			a1.setFillAfter(true);
			a1.setInterpolator(new LinearInterpolator());

			a2 = new AlphaAnimation(0.0f, 1.0f);
			a2.setDuration(400);
			a2.setStartOffset(0);
			a1.setInterpolator(new LinearInterpolator());

			image1.clearAnimation();
			image2.clearAnimation();
			image1.startAnimation(a1);
			image2.startAnimation(a2);
			image2.setVisibility(View.VISIBLE);
		}
		
		void startRotateAnimation() {
			Animation a1, a2;
			a1 = new ScaleAnimation(1.0f, 0.0f, 1.0f, 1.0f,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			a1.setDuration(130);
			a1.setFillAfter(true);
			a1.setInterpolator(new AccelerateDecelerateInterpolator());

			a2 = new ScaleAnimation(0.0f, 1.0f, 1.0f, 1.0f,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			a2.setDuration(130);
			a2.setStartOffset(130);
			a2.setInterpolator(new AccelerateDecelerateInterpolator());

			image1.clearAnimation();
			image2.clearAnimation();
			image1.startAnimation(a1);
			image2.startAnimation(a2);
			image2.setVisibility(View.VISIBLE);
		}

		@Override
		public void OnJobStarted(Job job) {
			Animation a = new AlphaAnimation(1.0f, 0.4f);
			a.setDuration(200);
			a.setRepeatMode(Animation.REVERSE);
			a.setRepeatCount(Animation.INFINITE);
			a.setInterpolator(new AccelerateDecelerateInterpolator());
			image1.startAnimation(a);
		}

		@Override
		public void OnJobDone(Job job) {
			FetchThumbnailJob fj = (FetchThumbnailJob)job;
			Frupic frupic = fj.getFrupic();
			if (frupic != this.frupic)
				return; 
			
			if (fj.isFailed()) {
				image1.clearAnimation();
				image1.setImageResource(R.drawable.broken_frupic);
				Log.e(tag, "fetchThumb failed");
			} else {
				Bitmap b = fj.getBitmap();
				cache.put(frupic.id, b);
				setImage(b);
				
				if (fj.isFromCache())
					startFadeAnimation();
				else
					startRotateAnimation();
			}
			job = null;
		}

		@Override
		public void OnJobProgress(Job job, int progress, int max) {
			/* ignore, because preview FetchJob does not have progress */
		}
	}
	
	FruPicGridAdapter(FruPicGrid activity, FrupicFactory factory, int cacheSize) {
		super(activity.getBaseContext(), null, false);
		this.activity = activity;
		this.factory = factory;
		this.cache = new LruCache<>(cacheSize);
	}

	@Override
	public void bindView(View convertView, Context context, Cursor cursor) {
		ViewHolder v;

		v = (ViewHolder)convertView.getTag();
		Frupic frupic = new Frupic(cursor);
		v.setFrupic(frupic);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewHolder v;
		View convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
		v = new ViewHolder(convertView);
		convertView.setTag(v);
		return convertView;
	}
}
