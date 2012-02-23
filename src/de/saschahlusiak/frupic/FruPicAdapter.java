package de.saschahlusiak.frupic;



import de.saschahlusiak.frupic.model.FruPicContainer;
import de.saschahlusiak.frupic.model.Frupic;

import android.content.Context;

import android.view.View;
import android.widget.BaseAdapter;



public abstract class FruPicAdapter extends BaseAdapter {
	Context context;
	FruPicContainer container;
	
	Frupic frupics[];
	
	/*
	http://api.freamware.net/2.0/get.picture?username=wiedi&offset=207&limit=1
	*/

	
	FruPicAdapter(Context context, int loaders, int cachesize) {
		this.context = context;
		container = new FruPicContainer(cachesize, loaders);
		frupics = null;
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

	protected void getFruPic(View view, int position) {
		if (frupics == null)
			return;

		container.fetchImage(getItem(position), view);
	}
	
	public void clearCache() {
		container.clear();
		notifyDataSetChanged();
	}
}
