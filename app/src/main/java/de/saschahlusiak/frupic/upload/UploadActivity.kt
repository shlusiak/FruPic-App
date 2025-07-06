package de.saschahlusiak.frupic.upload

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.PreparedImage
import de.saschahlusiak.frupic.app.UploadManager
import de.saschahlusiak.frupic.databinding.UploadActivityBinding
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class UploadActivity : AppCompatActivity(), View.OnClickListener {

    @Inject
    lateinit var manager: UploadManager

    private val viewModel: UploadActivityViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return UploadActivityViewModel(application, intent, manager) as T
            }
        }
    }

    private lateinit var binding: UploadActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

        binding = UploadActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent == null) {
            finish()
            return
        }

        with(binding) {
            viewModel.filenameText.observe(this@UploadActivity, Observer { filename.text = it })
            viewModel.okEnabled.observe(this@UploadActivity, Observer { upload.isEnabled = it })
            viewModel.preview.observe(this@UploadActivity, Observer { file ->
                Picasso.get()
                    .load(file)
                    .fit()
                    .centerInside()
                    .into(preview)
            })
            viewModel.inProgress.observe(this@UploadActivity, Observer { inProgress ->
                progress.visibility = if (inProgress) View.VISIBLE else View.GONE
                titleLabel.visibility = if (inProgress) View.INVISIBLE else View.VISIBLE
            })
            viewModel.sizeLabel.observe(this@UploadActivity, Observer { value ->
                fileSize.text = value
            })

            closeButton.setOnClickListener { finish() }
            upload.setOnClickListener(this@UploadActivity)

            imageResizeCheckbox.isChecked = viewModel.resizeImages
            imageResizeCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setResize(isChecked)
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)

            if (prefs.getString("username", "") !== "") username.setText(
                prefs.getString(
                    "username",
                    null
                )
            )
        }
    }

    override fun onClick(v: View) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val username = binding.username.text.toString()
        val tags = binding.tags.text.toString().ifBlank { "via:android" }

        prefs.edit()
            .putString("username", username)
            .apply()

        viewModel.viewModelScope.launch {
            viewModel.submitToService(username, tags)

            Toast.makeText(this@UploadActivity, R.string.uploading_toast, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

class UploadActivityViewModel(
    app: Application,
    intent: Intent,
    val manager: UploadManager
) : ViewModel() {
    private val tag = UploadActivityViewModel::class.simpleName
    private val context = app

    var resizeImages: Boolean = true
        private set

    /**
     * The source Uris
     */
    private var images = emptyList<PreparedImage>()

    val filenameText = MutableLiveData<String?>(null)
    val okEnabled = MutableLiveData(false)
    val inProgress = MutableLiveData(true)
    val preview = MutableLiveData<File>()
    val sizeLabel = MutableLiveData<String>()

    private var hasSubmitted = false

    init {
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

            preview.value = images.firstOrNull()?.original?.file

            // pre-emptively resize all pictures
            doResize()

            updateLabels()
            inProgress.value = false
            okEnabled.value = true
        }
    }

    fun setResize(resize: Boolean) {
        this.resizeImages = resize

        viewModelScope.launch {
            updateLabels()
        }
    }

    private suspend fun doResize() {
        // TODO: resize source images and update labels

//        delay(2000)
    }

    private suspend fun updateLabels() {
        filenameText.value = if (images.size == 1) {
            "\"${images[0].name}\""
        } else {
            context.getString(R.string.files, images.size)
        }

        val sizeTotal = images.sumBy {
            if (resizeImages)
                it.resized.await()?.size?.toInt() ?: 0
            else
                it.original.size.toInt()
        }

        var result = "${sizeTotal / 1024} kB"
        if (images.size == 1) {
            val first = images.first()
            val image =
                if (resizeImages) first.resized.await() ?: first.original else first.original
            result += " (${image.width}x${image.height})"
        }

        sizeLabel.value = result
    }

    override fun onCleared() {
        super.onCleared()
        if (!hasSubmitted) {
            GlobalScope.launch {
                images.forEach {
                    it.delete()
                }
            }
        }
    }

    suspend fun submitToService(username: String, tags: String) {
        hasSubmitted = true
        val images = this.images.map {
            val resized = it.resized.await()
            val original = it.original
            // whatever we are *not* submitting, we need to clean up here
            // TODO: this should probably live in the UploadManager instead
            if (resizeImages && resized != null) {
                original.delete()
                resized
            } else {
                resized?.delete()
                it.original
            }
        }
        manager.submit(images, username, tags)
    }
}