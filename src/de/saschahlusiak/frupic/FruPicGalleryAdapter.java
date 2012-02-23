package de.saschahlusiak.frupic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FruPicGalleryAdapter extends FruPicAdapter {
	FruPicGalleryAdapter(Context context) {
		super(context, 1, 4);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v;
		if (convertView == null) {
			v = LayoutInflater.from(context).inflate(R.layout.gallery_item, parent, false);
//			v.setAdjustViewBounds(false);
//			v.setPadding(10, 10, 10, 10);
		} else {
			v = convertView;
		}
		v.setTag(null);
		TextView t = (TextView)v.findViewById(R.id.textView1);
		t.setText(getItem(position).getUrl());
	
		getFruPic(v, position);
		getFruPic(null, position - 1);
		getFruPic(null, position + 1);
		return v;
	}

}
