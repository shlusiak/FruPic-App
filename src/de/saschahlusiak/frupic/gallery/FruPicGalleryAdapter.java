package de.saschahlusiak.frupic.gallery;

import de.saschahlusiak.frupic.R;

import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;


public class FruPicGalleryAdapter extends BaseAdapter {
	Context context;
	Frupic[] frupics;
	FrupicFactory factory;
	
	
	FruPicGalleryAdapter(Context context, FrupicFactory factory) {
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
		View v;
		if (convertView == null) {
			v = LayoutInflater.from(context).inflate(R.layout.gallery_item, parent, false);
		} else {
			v = convertView;
		}
		ImageView i = (ImageView)v.findViewById(R.id.imageView);
		
		Bitmap b = factory.getFullBitmap(getItem(position));
		if (b != null)
			i.setImageBitmap(b);
		else 
			i.setImageResource(R.drawable.frupic);
	
		
		return v;
	}
}
