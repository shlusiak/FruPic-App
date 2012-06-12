package de.saschahlusiak.frupic.grid;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;


public class FruPicGridAdapter extends BaseAdapter {
	Context context;
	Frupic[] frupics;
	FrupicFactory factory;
	private static final String tag = FetchPreviewTask.class.getSimpleName();
		
	public class FetchPreviewTask extends AsyncTask<Void, Frupic, Void> {
		Frupic frupic;

		FetchPreviewTask(Frupic frupic) {
			this.frupic = frupic;
		}

		@Override
		protected void onPreExecute() {
			
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... bla) {
			if (isCancelled())
				return null;

			int ret;

			publishProgress(frupic);
			ret = factory.fetchThumb(frupic);
			if (isCancelled())
				return null;

			switch (ret) {
			case FrupicFactory.NOT_AVAILABLE:
				Log.e(tag, "fetchThumb returned NOT_AVAILABLE");
			//	cancel(false);
				break;
			case FrupicFactory.FROM_CACHE:
				break;
			case FrupicFactory.FROM_FILE:
			case FrupicFactory.FROM_WEB:
				break;
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Frupic... values) {
			FruPicGridAdapter.ViewHolder holder = (FruPicGridAdapter.ViewHolder) frupic.getTag();
			if (holder.frupic == frupic && holder.task == this) {			
				Animation a = new AlphaAnimation(1.0f, 0.4f);
				a.setDuration(350);
				a.setRepeatMode(Animation.REVERSE);
				a.setRepeatCount(Animation.INFINITE);
				a.setInterpolator(new AccelerateDecelerateInterpolator());
				holder.image1.startAnimation(a);
			}
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onCancelled() {
			notifyDataSetChanged();
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Void result) {
			FruPicGridAdapter.ViewHolder holder = (FruPicGridAdapter.ViewHolder) frupic.getTag();
			if (holder.frupic == frupic && holder.task == this) {
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
			notifyDataSetChanged();

			super.onPostExecute(result);
		}
	}
	
	public class ViewHolder {
		Frupic frupic;
		ImageView image1, image2;
		FetchPreviewTask task;
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
		Bitmap b = factory.getThumbBitmap(frupic);

		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
			v = new ViewHolder();
			convertView.setTag(v);
			v.image1 = (ImageView)convertView.findViewById(R.id.imageView1);
			v.image2 = (ImageView)convertView.findViewById(R.id.imageView2);
			v.task = null;
			v.frupic = frupic;			
		} else {
			v = (ViewHolder)convertView.getTag();
			v.frupic = frupic;
			if (v.task != null) {
				if (v.task.frupic != frupic) {
					Log.d(tag, "reusing view with different frupic, killing task");
					v.task.cancel(false);
					v.task = null;
					if (b != null) {
						v.image1.clearAnimation();			
						v.image2.clearAnimation();
						v.image1.setVisibility(View.INVISIBLE);	
						v.image2.setVisibility(View.VISIBLE);	
					}
				}
			}
		}
		v.frupic.setTag(v);
		if (b == null && v.task == null) {
			/* Item is not in cache, set up waiting and start task */
			v.image1.setImageResource(R.drawable.frupic);
			v.image2.setImageResource(R.drawable.frupic);
			
			v.image1.clearAnimation();			
			v.image2.clearAnimation();
			v.image1.setVisibility(View.VISIBLE);
			v.image2.setVisibility(View.INVISIBLE);
			
			v.task = new FetchPreviewTask(frupic);
			/* TODO: THIS DOES NOT EXIST IN ANDROID 2
			 * Android 2 has also a Task limit of about 10, so an appropriate queueing mechanism needs to be implemented
			 */
			v.task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		}

				
		if (b != null)
			v.image2.setImageBitmap(b);		
		
		return convertView;
	}
}
