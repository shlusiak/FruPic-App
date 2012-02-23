package de.saschahlusiak.frupic.gallery;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Gallery;

public class OneFlingScrollGallery extends Gallery {

	public OneFlingScrollGallery(Context context) {
		this(context, null);
	}

	public OneFlingScrollGallery(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		return super.onFling(e1, e2, velocityX / 2.5f, velocityY);
	}
}