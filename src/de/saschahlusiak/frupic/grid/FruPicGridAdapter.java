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
					v.task.cancel();
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
			
			v.task = new PreviewFetchTask(this, factory, frupic);
			/* TODO: THIS DOES NOT EXIST IN ANDROID 2
			 * Android 2 has also a Task limit of about 10, so an appropriate queueing mechanism needs to be implemented
			 */
//			v.task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		}

				
		if (b != null)
			v.image2.setImageBitmap(b);		
		
		return convertView;
	}
}
