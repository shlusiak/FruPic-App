package de.saschahlusiak.frupic.gallery;

import de.saschahlusiak.frupic.R;

import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;


public class FruPicGalleryAdapter extends CursorAdapter {
	FrupicFactory factory;
	
	FruPicGalleryAdapter(Context context, FrupicFactory factory) {
		super(context, null, false);
		this.factory = factory;
	}
	
	public Frupic getFrupic(int position) {
		if (position < 0)
			return null;
		if (position > getCount())
			return null;
		return new Frupic((Cursor)getItem(position));
	}

	Frupic tmp = new Frupic();
	
	@Override
	public void bindView(View convertView, Context context, Cursor cursor) {
		tmp.fromCursor(cursor);
		
		ImageView i = (ImageView)convertView.findViewById(R.id.imageView);
		
		Bitmap b = factory.getFullBitmap(tmp);
		if (b != null)
			i.setImageBitmap(b);
		else 
			i.setImageResource(R.drawable.frupic);
	}

	@Override
	public View newView(Context context, Cursor arg1, ViewGroup parent) {
		View v;
		
		v = LayoutInflater.from(context).inflate(R.layout.gallery_item, parent, false);
		
		return v;
	}
}
