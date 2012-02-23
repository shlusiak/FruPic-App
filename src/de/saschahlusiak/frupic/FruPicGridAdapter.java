package de.saschahlusiak.frupic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class FruPicGridAdapter extends FruPicAdapter {
	FruPicGridAdapter(Context context) {
		super(context, 3, 40);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v;
		if (convertView == null) {
			v = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
//			v.setAdjustViewBounds(false);
//			v.setPadding(10, 10, 10, 10);
		} else {
			v = convertView;
		}
	
		getFruPic(v, position);
		return v;
	}
	
	

}
