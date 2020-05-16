package de.saschahlusiak.frupic.gallery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import de.saschahlusiak.frupic.R;
import de.saschahlusiak.frupic.cache.FileCacheUtils;
import de.saschahlusiak.frupic.model.Frupic;
import de.saschahlusiak.frupic.model.FrupicFactory;
import de.saschahlusiak.frupic.services.FetchFrupicJob;
import de.saschahlusiak.frupic.services.Job;
import de.saschahlusiak.frupic.services.Job.OnJobListener;
import de.saschahlusiak.frupic.services.Job.Priority;
import de.saschahlusiak.frupic.services.JobManager;
import android.database.Cursor;
import android.net.Uri;
import androidx.viewpager.widget.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GalleryAdapter extends PagerAdapter implements OnJobListener {
	private final static String tag = GalleryAdapter.class.getSimpleName();

	private GalleryActivity context;
	private Cursor cursor;
	private boolean showAnimations;
	private FrupicFactory factory;
	private JobManager jobManager;
	
	static class ViewHolder {
		public Frupic frupic;
		public Job job;
		public View view;
		
		public ViewGroup progressLayout;
		public ProgressBar progress;
		public TextView progressText;
	}

	public GalleryAdapter(GalleryActivity context, boolean showAnimations) {
		this.context = context;
		this.showAnimations = showAnimations;
		this.factory = new FrupicFactory(context);
	}
	
	public void setJobManager(JobManager jobManager) {
		if (jobManager == null && this.jobManager != null) {
			this.jobManager.removeAllJobListener(this);
		}
		this.jobManager = jobManager;
		
		notifyDataSetChanged();
	}

	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
		notifyDataSetChanged();
	}
	
	private boolean showFrupic(View view, Frupic frupic) {
		GifMovieView v = (GifMovieView)view.findViewById(R.id.videoView);
		SubsamplingScaleImageView i = (SubsamplingScaleImageView)view.findViewById(R.id.imageView);

		if (showAnimations && frupic.isAnimated()) {
			v.setVisibility(View.VISIBLE);
			i.setVisibility(View.GONE);
			String filename = new FileCacheUtils(context).getFileName(frupic);
            InputStream stream = null;
			try {
				/* Movie calls reset() which the InputStream must support.
				 * 
				 * FileInputStream does NOT
				 * BufferedInputStream does, but somehow we need to call mark(x) first, with x>0. WTF?
				 * ByteArrayInputStream does
				 * 
				 * I do not trust BufferedInputStream and because it will probably map the whole file anyway,
				 * I can just save it in a byte array.
				 */
				stream = new FileInputStream(filename);
				
//				stream = new BufferedInputStream(stream);
//				stream.mark(1);
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				byte buf[] = new byte[4096];
				while (stream.read(buf) > 0) {
					bos.write(buf);
				}
				bos.flush();
				stream.close();
				stream = new ByteArrayInputStream(bos.toByteArray());
				
				v.setStream(stream);
				return true;
			} catch (OutOfMemoryError e2) {
				stream = null;
				System.gc();
				Log.e("OutOfMemoryError", "trying to load gif animation as Bitmap instead");
				/* fall-through to load Bitmap instead*/
			} catch (FileNotFoundException e1) {
				return false;
			} catch (Exception e1) {
				e1.printStackTrace();
				return false;
			}			
		}
		
		/* fall-through, if loading animation failed */
		File file = factory.getCacheFile(frupic);
		Uri uri = null;
		if (file.exists()) {
			uri = Uri.fromFile(file);
			file.setLastModified(new Date().getTime());
			
			i.setVisibility(View.VISIBLE);
			v.setVisibility(View.GONE);
			i.setImage(ImageSource.uri(uri));
			
			return true;
		}
		return false;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		Log.w(tag, "instantiateItem(" + position + ")");
		cursor.moveToPosition(position);

		Frupic frupic = new Frupic(cursor);

		View view = LayoutInflater.from(context).inflate(R.layout.gallery_item, container, false);
		SubsamplingScaleImageView i = (SubsamplingScaleImageView)view.findViewById(R.id.imageView);
		GifMovieView v = (GifMovieView)view.findViewById(R.id.videoView);
		
		i.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
		
		// TODO: context menu on image view seems to be broken
		context.registerForContextMenu(view);
		context.registerForContextMenu(i);
		context.registerForContextMenu(v);
		
		OnClickListener l = new OnClickListener() {
			@Override
			public void onClick(View v) {
				context.toggleControls();
			}
		};
		
		i.setOnClickListener(l);
		v.setOnClickListener(l);
		view.setOnClickListener(l);
		
		final ViewHolder holder = new ViewHolder();
		holder.view = view;
		holder.progressLayout = (ViewGroup) view.findViewById(R.id.progressLayout);
		
		holder.progress = (ProgressBar)view.findViewById(R.id.progressBar);
		holder.progress.setIndeterminate(false);
		holder.progress.setMax(100);
		holder.progress.setProgress(0);
		holder.progressText = (TextView)view.findViewById(R.id.progress);
		
		final ImageButton stopButton = (ImageButton)holder.view.findViewById(R.id.stopButton);
		
		stopButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
		stopButton.setVisibility(View.VISIBLE);
		stopButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (holder.job.isCancelled()) {
					jobManager.post(holder.job, Priority.PRIORITY_HIGH);
				} else {
					holder.job.cancel();
					stopButton.setImageResource(android.R.drawable.ic_menu_revert);
				}
			}
		});
		
		holder.frupic = frupic;
		
		frupic.tag = holder;
		view.setTag(holder);

		if (!showFrupic(view, frupic)) {
			i.setVisibility(View.GONE);
			v.setVisibility(View.GONE);
			
			holder.progress.setVisibility(View.VISIBLE);
			holder.progressText.setVisibility(View.VISIBLE);
			holder.progressText.setText(R.string.waiting_to_start);
		
			if (jobManager != null) {
				holder.job = jobManager.getFetchJob(frupic, factory);
				holder.job.addJobListener(this);
				
				jobManager.post(holder.job, Priority.PRIORITY_HIGH);
			} else {
				Log.d(tag, "JobManager not available yet");
				holder.view = null;
			}	
		} else {
			view.findViewById(R.id.progressLayout).setVisibility(View.GONE);
		}
		
		container.addView(view);
		return holder;
	}
	
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		Log.w(tag, "destroyItem(" + position + ")");
		
		ViewHolder holder = (ViewHolder)object;
		holder.frupic.tag = null;
		if (holder.job != null)
			holder.job.cancel();
		
		container.removeView(holder.view);
	}
	
	@Override
	public int getItemPosition(Object object) {
		ViewHolder holder = (ViewHolder)object;
		if (holder.view == null)
			return POSITION_NONE;
		return POSITION_UNCHANGED;
	}

	@Override
	public int getCount() {
		if (cursor == null)
			return 0;
		return cursor.getCount();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((ViewHolder)object).view;
	}

	@Override
	public void OnJobStarted(final Job job) {
		Frupic frupic = ((FetchFrupicJob)job).getFrupic();
		final ViewHolder holder = (ViewHolder) frupic.tag;
		
		
	}

	@Override
	public void OnJobProgress(Job job, final int progress, final int max) {
		Frupic frupic = ((FetchFrupicJob)job).getFrupic();
		final ViewHolder holder = (ViewHolder) frupic.tag;
	
		if (holder == null)
			return;

		holder.view.post(new Runnable() {
			@Override
			public void run() {
				holder.progress.setProgress((100 * progress) / max);
				holder.progressText.setText(String.format("%dkb / %dkb (%d%%)", progress / 1024, max / 1024, (max > 0) ? progress * 100 / max : 0));
			}
		});
	}

	@Override
	public void OnJobDone(Job job) {
		Frupic frupic = ((FetchFrupicJob)job).getFrupic();
		ViewHolder holder = (ViewHolder) frupic.tag;
		
		Log.d(tag, "OnJobDone(" + frupic.id + ")");
		
		if (holder == null)
			return;
		
		if (job.isCancelled()) {
			holder.progressText.setText(R.string.cancelled);
		} else {
			holder.job.removeJobListener(this);
			holder.job = null;
			SubsamplingScaleImageView i = (SubsamplingScaleImageView) holder.view.findViewById(R.id.imageView);
			GifMovieView v = (GifMovieView)holder.view.findViewById(R.id.videoView);
			
			holder.progressLayout.setVisibility(View.GONE);
			
			if (!showFrupic(holder.view, frupic)) {
				i.setVisibility(View.VISIBLE);
				v.setVisibility(View.GONE);
				i.setImage(ImageSource.resource(R.drawable.broken_frupic));
			}
		}
	}
}
