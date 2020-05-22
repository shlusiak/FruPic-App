package de.saschahlusiak.frupic.upload

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.squareup.picasso.Picasso
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.UploadImage
import de.saschahlusiak.frupic.app.UploadManager
import kotlinx.android.synthetic.main.upload_activity.*
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

class UploadActivity : AppCompatActivity(R.layout.upload_activity), View.OnClickListener {

    private val viewModel: UploadActivityViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return UploadActivityViewModel(application, intent) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

        if (intent == null) {
            finish()
            return
        }

        viewModel.filenameText.observe(this, Observer { filename.text = it })
        viewModel.okEnabled.observe(this, Observer { upload.isEnabled = it })
        viewModel.preview.observe(this, Observer { file ->
            Picasso.get()
                .load(file)
                .fit()
                .centerInside()
                .into(preview)
        })
        viewModel.inProgress.observe(this, Observer { inProgress ->
            progress.visibility = if (inProgress) View.VISIBLE else View.GONE
            title_label.visibility = if (inProgress) View.INVISIBLE else View.VISIBLE
        })
        viewModel.size.observe(this, Observer { totalSize ->
            fileSize.text = "${totalSize / 1024} kB"
        })

        closeButton.setOnClickListener { finish() }
        upload.setOnClickListener(this)

        image_resize_checkbox.isChecked = viewModel.resizeImages
        image_resize_checkbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setResize(isChecked)
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)

        if (prefs.getString("username", "") !== "") username.setText(prefs.getString("username", null))
    }

    override fun onClick(v: View) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val username = username.text.toString()
        val tags = tags.text.toString().ifBlank { "via:android" }

        prefs.edit()
            .putString("username", username)
            .apply()

        viewModel.submitToService(username, tags)

        Toast.makeText(this, R.string.uploading_toast, Toast.LENGTH_LONG).show()
        finish()
    }
}

class UploadActivityViewModel(app: Application, intent: Intent) : AndroidViewModel(app) {
    private val tag = UploadActivityViewModel::class.simpleName
    private val context = app

    var resizeImages: Boolean = true
        private set

    /**
     * The source Uris
     */
    private var images = emptyList<UploadImage>()

    val filenameText = MutableLiveData<String>(null)
    val okEnabled = MutableLiveData(false)
    val inProgress = MutableLiveData(true)
    val preview = MutableLiveData<File>()
    val size = MutableLiveData<Int>()

    private var hasSubmitted = false
    private val resized = CompletableDeferred<Boolean>()

    @Inject
    lateinit var manager: UploadManager

    init {
        (app as App).appComponent.inject(this)

        val sources = when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.extras?.get(Intent.EXTRA_STREAM) as? Uri
                uri?.let { listOf(it) } ?: emptyList()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("UNCHECKED_CAST")
                intent.extras?.get(Intent.EXTRA_STREAM) as? List<Uri> ?: emptyList()
            }
            else -> emptyList()
        }

        viewModelScope.launch {
            okEnabled.value = false
            inProgress.value = true
            filenameText.value = null

            // copy all images from the source to a cache directory
            images = manager.prepareForUpload(sources)

            preview.value = images.firstOrNull()?.file

            // pre-emptively resize all pictures
            doResize()
            resized.complete(true)

            updateLabels()
            inProgress.value = false
            okEnabled.value = true
        }
    }

    fun setResize(resize: Boolean) {
        this.resizeImages = resize

        viewModelScope.launch {
            resized.await()
            updateLabels()
        }
    }

    private suspend fun doResize() {
        // TODO: resize source images and update labels

//        delay(2000)

    }

    private fun updateLabels() {
        filenameText.value = if (images.size == 1) {
            "\"${images[0].name}\""
        } else {
            context.getString(R.string.files, images.size)
        }

        val sizeTotal = if (resizeImages) {
            0
        } else {
            images.sumBy { it.size.toInt() }
        }
        size.value = sizeTotal
    }

    override fun onCleared() {
        super.onCleared()
        if (!hasSubmitted) {
            images.forEach {
                it.cleanup()
            }
        }
    }

    fun submitToService(username: String, tags: String) {
        hasSubmitted = true
        manager.submit(images, resizeImages, username, tags)
    }
}