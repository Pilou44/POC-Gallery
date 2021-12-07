package com.wechantloup.pocgallery

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wechantloup.pocgallery.provider.LocalGalleryProvider
import com.wechantloup.pocgallery.provider.PhotoAlbum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AlbumsViewModel(application: Application): AndroidViewModel(application) {

    private val _stateFlow = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _stateFlow

    private val provider = LocalGalleryProvider(application)

    suspend fun getAlbums() {
        Log.i("TOTO", "getAlbums()")
        _stateFlow.value = stateFlow.value.copy(
            albums = provider.getNextAlbums()
        )
    }

    data class State(
        val albums: List<PhotoAlbum> = emptyList()
    )

    class Factory(private val activity: Activity) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AlbumsViewModel(activity.application) as T
        }
    }
}