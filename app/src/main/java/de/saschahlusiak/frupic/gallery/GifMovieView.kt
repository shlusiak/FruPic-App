package de.saschahlusiak.frupic.gallery

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.io.InputStream

@Deprecated("Prefer AnimatedImageDrawable")
class GifMovieView @JvmOverloads constructor(context: Context?, attr: AttributeSet? = null) : View(context, attr) {
    private var mMovie: Movie? = null
    private var mMoviestart: Long = 0

    init {
        /* FIXME:
		 *   figure out, why. hardware accelerated shows a black screen on SGS2 with Android 4.0.4
		 */
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setStream(stream: InputStream?) {
        if (stream == null) {
            mMovie = null
            Log.e(Companion.tag, "stream == null")
            return
        }
        mMovie = Movie.decodeStream(stream)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT)
        super.onDraw(canvas)

        val movie = mMovie ?: return
        canvas.translate(width / 2.toFloat(), height / 2.toFloat())
        var s: Float
        s = width.toFloat() / movie.width().toFloat()
        if (height.toFloat() / movie.height().toFloat() < s)
            s = height.toFloat() / movie.height().toFloat()

        if (s < 1.0f) canvas.scale(s, s)

        val now = SystemClock.uptimeMillis()
        if (mMoviestart == 0L) {
            mMoviestart = now
        }
        var relTime = 0
        if (movie.duration() > 0) relTime = ((now - mMoviestart) % movie.duration()).toInt()
        movie.setTime(relTime)
        movie.draw(canvas, -movie.width() / 2.0f, -movie.height() / 2.0f)

        /* limit animations to 20fps */this.postInvalidateDelayed(1000 / 20.toLong())
    }

    companion object {
        private val tag = GifMovieView::class.java.simpleName
    }
}