
package de.saschahlusiak.frupic.gallery;

import java.io.InputStream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class GifMovieView extends View {
	private static final String tag = GifMovieView.class.getSimpleName();

    private Movie mMovie;

    private long mMoviestart;
    
    public GifMovieView(Context context) {
    	this(context, null);
    }
    
    public GifMovieView(Context context, AttributeSet attr) {
    	super(context, attr);

		/* FIXME:
		 *   figure out, why. hardware accelerated shows a black screen on SGS2 with Android 4.0.4
		 */
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    
    public void setStream(InputStream stream) {
    	if (stream == null) {
    		mMovie = null;
    		Log.e(tag, "stream == null");
    		return;
    	}
    	mMovie = Movie.decodeStream(stream);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        super.onDraw(canvas);
        if (mMovie == null)
        	return;
        
        canvas.translate(getWidth() / 2, getHeight() / 2);
       	float s;
       	s = (float)getWidth() / (float)mMovie.width();
       	if ((float)getHeight() / (float)mMovie.height() < s)
           	s =  (float)getHeight() / (float)mMovie.height();
       	if (s < 1.0f)
       		canvas.scale(s, s);
        
        final long now = SystemClock.uptimeMillis();

        if (mMoviestart == 0) { 
            mMoviestart = now;
        }

        int relTime = 0;
        if (mMovie.duration() > 0) 
        	relTime = (int)((now - mMoviestart) % mMovie.duration());
        
        mMovie.setTime(relTime);
        mMovie.draw(canvas, -mMovie.width() / 2, -mMovie.height() / 2);
        
        /* limit animations to 20fps */
        this.postInvalidateDelayed(1000 / 20);
    }
}
