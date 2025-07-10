package de.saschahlusiak.frupic.upload

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    @ApplicationContext val context: Context
) : ViewModel() {

    init {

    }
}