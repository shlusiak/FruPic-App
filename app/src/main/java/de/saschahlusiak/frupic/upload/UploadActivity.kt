package de.saschahlusiak.frupic.upload

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import de.saschahlusiak.frupic.Feature
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.utils.AppTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UploadActivity : AppCompatActivity() {

    private val viewModel: UploadActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                UploadScreen(
                    viewModel,
                    { finish() },
                    ::startUpload
                )
            }
        }
    }

    private fun startUpload(username: String, tags: String, resized: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        lifecycleScope.launch {
            prefs.edit {
                putString("username", username)
            }

            viewModel.submitToService(username, tags, resized)

            if (Feature.UPLOAD_STATUS) {
                val intent = Intent(this@UploadActivity, StatusActivity::class.java)
                startActivity(intent)
            }

            Toast.makeText(this@UploadActivity, R.string.uploading_toast, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

