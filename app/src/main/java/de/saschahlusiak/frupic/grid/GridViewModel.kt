package de.saschahlusiak.frupic.grid

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.saschahlusiak.frupic.app.App
import de.saschahlusiak.frupic.app.FrupicRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class GridViewModel(app: Application) : AndroidViewModel(app) {
    val starred = MutableLiveData(false)
    val cursor = MutableLiveData<Cursor>()

    @Inject
    lateinit var repository: FrupicRepository

    init {
        (app as App).appComponent.inject(this)

        reloadData()
    }

    override fun onCleared() {
        super.onCleared()

        cursor.value?.close()
    }

    fun toggleStarred() {
        starred.value = (starred.value == false)
        reloadData()
    }

    fun reloadData() {
        viewModelScope.launch {
            val c = repository.getFrupics(starred.value ?: false)
            cursor.value?.close()
            cursor.value = c
        }
    }
}