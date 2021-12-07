package com.wechantloup.pocgallery.album

import android.app.Activity
import android.app.Application
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

    suspend fun getAlbums() {
        _stateFlow.value = stateFlow.value.copy(
            albums = LocalGalleryProvider.getNextAlbums(getApplication())
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