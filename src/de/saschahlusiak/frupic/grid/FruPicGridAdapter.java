package de.saschahlusiak.frupic.grid;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;


public class FruPicGridAdapter extends BaseAdapter {
	private static final String tag = FruPicGridAdapter.class.getSimpleName();
	Context context;
	Frupic[] frupics;
	FrupicFactory factory;
	
	public class ViewHolder {
		Frupic frupic;
		ImageView image1, image2;
		PreviewFetchTask task;
		
		ViewHolder(View convertView) {
			image1 = (ImageView)convertView.findViewById(R.id.imageView1);
			image2 = (ImageView)convertView.findViewById(R.id.imageView2);
			task = null;
		}
		
		void setFrupic(Frupic frupic) {
			if ((this.frupic != null) && (this.frupic.getId() == frupic.getId()))
				return;
			this.frupic = frupic;
			if (task != null)
				task.cancel();
			task = null;
			
			Bitmap b = factory.getThumbBitmap(frupic);
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

			Animation a = new AlphaAnimation(1.0f, 0.6f);
			a.setDuration(250);
			a.setRepeatMode(Animation.REVERSE);
			a.setRepeatCount(Animation.INFINITE);
			a.setInterpolator(new AccelerateDecelerateInterpolator());
			image1.startAnimation(a);
			
			task = new PreviewFetchTask(this, factory, frupic);
		}
		
		void setImage(Bitmap b) {
			image2.setImageBitmap(b);
		}
		
		void fetchComplete(Frupic frupic, int ret) {
			if (frupic != this.frupic)
				return;
			Bitmap b = factory.getThumbBitmap(frupic);

			switch (ret) {
			case FrupicFactory.NOT_AVAILABLE:
				Log.e(tag, "fetchThumb returned NOT_AVAILABLE");
				break;
			case FrupicFactory.FROM_CACHE:
				startFadeAnimation();
				setImage(b);
				break;
			case FrupicFactory.FROM_FILE:
				setImage(b);
				startFadeAnimation();
				break;
			case FrupicFactory.FROM_WEB:
				setImage(b);
				startRotateAnimation();
				break;
			}
			task = null;
		}
		
		void startFadeAnimation() {
			Animation a1, a2;
			a1 = new AlphaAnimation(1.0f, 0.0f);
			a1.setDuration(600);
			a1.setFillAfter(true);
			a1.setInterpolator(new LinearInterpolator());

			a2 = new AlphaAnimation(0.0f, 1.0f);
			a2.setDuration(600);
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
			a1.setDuration(150);
			a1.setFillAfter(true);
			a1.setInterpolator(new AccelerateDecelerateInterpolator());

			a2 = new ScaleAnimation(0.0f, 1.0f, 1.0f, 1.0f,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			a2.setDuration(150);
			a2.setStartOffset(150);
			a2.setInterpolator(new AccelerateDecelerateInterpolator());

			image1.clearAnimation();
			image2.clearAnimation();
			image1.startAnimation(a1);
			image2.startAnimation(a2);
			image2.setVisibility(View.VISIBLE);
		}
	}
	
	FruPicGridAdapter(Context context, FrupicFactory factory) {
		this.context = context;
		this.frupics = null;
		this.factory = factory;
	}
	
	public void setFrupics(Frupic[] pics) {
		this.frupics = pics;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if (frupics == null)
			return 0;
		return frupics.length;
	}

	@Override
	public Frupic getItem(int position) {
		if (frupics == null)
			return null;
		if (position < 0)
			return null;
		if (position >= frupics.length)
			return null;
		return frupics[position];
	}

	@Override
	public long getItemId(int position) {
		if (frupics == null)
			return 0;
		return getItem(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder v;
		Frupic frupic = getItem(position);

		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
			v = new ViewHolder(convertView);
			convertView.setTag(v);
		} else {
			v = (ViewHolder)convertView.getTag();
		}
		v.setFrupic(frupic);
		
		return convertView;
	}
}
