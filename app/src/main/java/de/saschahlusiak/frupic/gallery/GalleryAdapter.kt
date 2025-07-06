package de.saschahlusiak.frupic.gallery

import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.viewpager.widget.PagerAdapter
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.DownloadJob
import de.saschahlusiak.frupic.app.FrupicDownloadManager
import de.saschahlusiak.frupic.app.FrupicStorage
import de.saschahlusiak.frupic.app.JobStatus
import de.saschahlusiak.frupic.databinding.GalleryItemBinding
import de.saschahlusiak.frupic.model.Frupic
import java.io.*
import java.util.*

class GalleryAdapter(
    private val activity: GalleryActivity,
    private val showAnimations: Boolean,
    private val storage: FrupicStorage,
    private val downloadManager: FrupicDownloadManager
) : PagerAdapter() {
    private var list: List<Frupic> = emptyList()

    inner class ViewHolder(
        val binding: GalleryItemBinding,
        val frupic: Frupic
    ) {
        private val progressLayout = binding.progressLayout
        private val stopButton = binding.stopButton
        private val progressBar = binding.progressBar
        private val progressText = binding.progress
        private val image = binding.imageView
        private val video = binding.videoView

        init {
            progressBar.apply {
                isIndeterminate = false
                max = 100
                progress = 0
            }

            stopButton.setOnClickListener {
                stopDownloadClick()
            }

            newFrupic(frupic)
        }

        private fun newFrupic(frupic: Frupic) {
            if (showFrupic(binding.root, frupic)) {
                // loaded and shown, otherwise download
                progressLayout.visibility = View.GONE
            } else {
                progressLayout.visibility = View.VISIBLE
                image.visibility = View.INVISIBLE
                video.visibility = View.INVISIBLE

                val job = getDownloadJob(frupic)
                job.status.asLiveData().observe(activity, Observer { result ->
                    when (result) {
                        is JobStatus.InProgress -> {
                            progressBar.progress = 100 * result.progress / result.max
                            progressText.text = String.format(
                                "%dkb / %dkb (%d%%)",
                                result.progress / 1024,
                                result.max / 1024,
                                if (result.max > 0) result.progress * 100 / result.max else 0
                            )
                        }

                        JobStatus.Cancelled -> {
                            progressText.setText(R.string.cancelled)
                            stopButton.setImageResource(android.R.drawable.ic_menu_revert)
                        }

                        is JobStatus.Success,
                            JobStatus.Failed -> {
                            progressLayout.visibility = View.GONE
                            if (!showFrupic(binding.root, frupic)) {
                                image.visibility = View.VISIBLE
                                video.visibility = View.GONE
                                image.setImage(ImageSource.resource(R.drawable.broken_frupic))
                            }
                        }

                        JobStatus.Scheduled -> {
                            // FIXME
                        }
                    }
                })
            }
        }

        private fun getDownloadJob(frupic: Frupic): DownloadJob {
            val existing = downloadManager.getJobOrNull(frupic)
            if (existing != null) return existing

            progressText.setText(R.string.waiting_to_start)
            // TODO: replace with vector drawable
            stopButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)

            return DownloadJob(frupic).also {
                downloadManager.enqueue(it)
            }
        }

        private fun stopDownloadClick() {
            if (downloadManager.cancel(frupic)) {
                // nothing
            } else {
                newFrupic(frupic)
            }
        }
    }

    fun setList(list: List<Frupic>) {
        this.list = list
        notifyDataSetChanged()
    }

    private fun showFrupic(view: View, frupic: Frupic): Boolean {
        val v: GifMovieView = view.findViewById(R.id.videoView)
        val i: SubsamplingScaleImageView = view.findViewById(R.id.imageView)
        if (showAnimations && frupic.isAnimated) {
            v.visibility = View.VISIBLE
            i.visibility = View.GONE
            val file = storage.getFile(frupic)
            var stream: InputStream
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
                stream = FileInputStream(file)

//				stream = new BufferedInputStream(stream);
//				stream.mark(1);
                val bos = ByteArrayOutputStream()
                val buf = ByteArray(4096)
                while (stream.read(buf) > 0) {
                    bos.write(buf)
                }
                bos.flush()
                stream.close()
                stream = ByteArrayInputStream(bos.toByteArray())
                v.setStream(stream)
                return true
            } catch (e2: OutOfMemoryError) {
                System.gc()
                Log.e("OutOfMemoryError", "trying to load gif animation as Bitmap instead")
                /* fall-through to load Bitmap instead*/
            } catch (e1: FileNotFoundException) {
                return false
            } catch (e1: Exception) {
                e1.printStackTrace()
                return false
            }
        }

        /* fall-through, if loading animation failed */
        val file = storage.getFile(frupic)

        if (file.exists()) {
            val uri = Uri.fromFile(file)
            file.setLastModified(Date().time)
            i.visibility = View.VISIBLE
            v.visibility = View.GONE
            i.setImage(ImageSource.uri(uri))
            return true
        }

        return false
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val frupic = list[position]

        Log.w(tag, "instantiateItem($position)")
        val binding = GalleryItemBinding.inflate(activity.layoutInflater, container, false)
        binding.imageView.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF

        val l = View.OnClickListener { activity.toggleControls() }
        binding.imageView.setOnClickListener(l)
        binding.videoView.setOnClickListener(l)
        binding.root.setOnClickListener(l)
        val holder = ViewHolder(binding, frupic)

        binding.root.tag = holder

        container.addView(binding.root)
        return holder
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        Log.w(tag, "destroyItem($position)")
        val holder = `object` as ViewHolder
        downloadManager.cancel(holder.frupic)
        container.removeView(holder.binding.root)
    }

    override fun getItemPosition(`object`: Any): Int {
        val holder = `object` as ViewHolder
        val frupic = holder.frupic
        val found = list.firstOrNull {
            it.id == frupic.id
        } ?: POSITION_NONE
        return list.indexOf(found)
    }

    override fun getCount(): Int {
        return list.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === (`object` as ViewHolder).binding.root
    }

    companion object {
        private val tag = GalleryAdapter::class.java.simpleName
    }
}