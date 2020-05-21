package de.saschahlusiak.frupic.upload

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.Images.Media
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import de.saschahlusiak.frupic.R
import kotlinx.android.synthetic.main.upload_activity.*
import kotlinx.coroutines.*

class UploadActivity : AppCompatActivity(), View.OnClickListener {

    private val viewModel: UploadActivityViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return UploadActivityViewModel(application, intent) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null) {
            finish()
            return
        }

        setContentView(R.layout.upload_activity)

        viewModel.labelText.observe(this, Observer { url.text = it })
        viewModel.okEnabled.observe(this, Observer { upload.isEnabled = it })

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
    private val sources: List<Uri>

    val labelText = MutableLiveData<String>(null)
    val okEnabled = MutableLiveData(false)

    private val resized = CompletableDeferred<Boolean>()

    init {
        sources = when (intent.action) {
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
            labelText.value = context.getString(R.string.please_wait)

            // copy all images from the source to a cache directory
            copyImages(sources)

            // pre-emtively resize all pictures
            doResize()
            resized.complete(true)

            updateLabels()
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

    private suspend fun copyImages(sources: List<Uri>) {

    }

    private suspend fun updateLabels() {
        labelText.value = if (sources.size == 1) {
            val filename = getFileName(sources[0])
            context.getString(R.string.upload_file_name, filename)
        } else {
            context.getString(R.string.upload_file_names, sources.size)
        }


    }

    private suspend fun getFileName(uri: Uri): String = withContext(Dispatchers.Default) {
        var fileName: String? = null

        when (uri.scheme) {
            "file" -> {
                fileName = uri.lastPathSegment
            }

            "content" -> {
                context.contentResolver.query(
                    uri,
                    arrayOf(Media.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.let { cursor ->
                    val columnIndex = cursor.getColumnIndexOrThrow(Media.DISPLAY_NAME)
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(columnIndex)
                    }
                    cursor.close()
                }
            }
        }

        fileName ?: "[Unknown]"
    }

    fun submitToService(username: String, tags: String) {
        for (uri in sources) {
            Log.d(tag, "Submitting to service: $uri")
            val intent = Intent(context, UploadService::class.java)

            intent.putExtra("scale", resizeImages)
            intent.putExtra("username", username)
            intent.putExtra("tags", tags)
            intent.putExtra("filename", uri.lastPathSegment)
            intent.putExtra("uri", uri)

            context.startService(intent)
        }
    }
}