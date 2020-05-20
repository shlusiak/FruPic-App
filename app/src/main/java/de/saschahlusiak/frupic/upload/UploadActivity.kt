package de.saschahlusiak.frupic.upload

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import de.saschahlusiak.frupic.R
import kotlinx.android.synthetic.main.upload_activity.*

class UploadActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var imageUri: List<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent == null) {
            finish()
            return
        }

        imageUri = if (Intent.ACTION_SEND == intent.action) {
            val uri = intent.extras?.get(Intent.EXTRA_STREAM) as? Uri
            uri?.let { listOf(it) } ?: emptyList()
        } else if (Intent.ACTION_SEND_MULTIPLE == intent.action) {
            intent.extras?.get(Intent.EXTRA_STREAM) as? List<Uri> ?: emptyList()
        } else {
            finish()
            return
        }

        // TODO: we need to copy the content away before be dismiss the activity, in case permission is revoked!

        setContentView(R.layout.upload_activity)

        url.text = if (imageUri.size == 1) {
            getString(R.string.upload_file_name, getFileName(imageUri[0]))
        } else {
            getString(R.string.upload_file_names, imageUri.size)
        }
        closeButton.setOnClickListener { finish() }
        upload.setOnClickListener(this)

        image_resize_checkbox.isChecked = true
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)

        if (prefs.getString("username", "") !== "") username.setText(prefs.getString("username", null))
    }

    private fun getFileName(uri: Uri): String {
        var fileName: String? = null
        val scheme = uri.scheme
        if (scheme == "file") {
            fileName = uri.lastPathSegment
        } else if (scheme == "content") {
            val proj = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
            val cursor = contentResolver.query(uri, proj, null,
                null, null)
            if (cursor != null && cursor.count != 0) {
                val columnIndex = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                cursor.moveToFirst()
                fileName = cursor.getString(columnIndex)
            }
            cursor?.close()
        }
        return fileName ?: "[Unknown]"
    }

    override fun onClick(v: View) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val username = username.text.toString()

        prefs.edit()
            .putString("username", username)
            .apply()

        var tags = tags.text.toString()
        if (tags.isEmpty()) tags = "via:android"

        for (uri in imageUri) {
            val intent = Intent(this, UploadService::class.java)
            intent.putExtra("scale", image_resize_checkbox.isChecked)
            intent.putExtra("username", username)
            intent.putExtra("tags", tags)
            intent.putExtra("filename", uri.lastPathSegment)
            intent.putExtra("uri", uri)
            startService(intent)
        }

        Toast.makeText(this, R.string.uploading_toast, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private val tag = UploadActivity::class.java.simpleName
    }
}