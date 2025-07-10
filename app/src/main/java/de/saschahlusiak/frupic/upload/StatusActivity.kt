package de.saschahlusiak.frupic.upload

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import de.saschahlusiak.frupic.utils.AppTheme

@AndroidEntryPoint
class StatusActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        setContent {
            AppTheme {
                StatusContent(
                    hiltViewModel()
                )
            }
        }
    }
}