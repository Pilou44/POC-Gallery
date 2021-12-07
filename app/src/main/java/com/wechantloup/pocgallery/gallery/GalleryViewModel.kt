package com.wechantloup.pocgallery.gallery

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wechantloup.pocgallery.provider.LocalGalleryProvider
import com.wechantloup.pocgallery.provider.Photo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GalleryViewModel(
    application: Application,
    albumId: String,
    albumTitle: String,
): AndroidViewModel(application) {

    private val _stateFlow = MutableStateFlow(State(albumTitle))
    val stateFlow: StateFlow<State> = _stateFlow

    init {
        LocalGalleryProvider.openAlbum(albumId)
    }

    suspend fun loadMorePhotos() {
        if (!LocalGalleryProvider.hasMorePhotos()) return

        val nextPhotos = LocalGalleryProvider.getNextPhotos(getApplication())
        val currentPhotos = stateFlow.value.photos
        _stateFlow.value = stateFlow.value.copy(
            photos = currentPhotos + nextPhotos
        )
    }

    data class State(
        val title: String,
        val photos: List<Photo> = emptyList()
    )

    class Factory(
        private val activity: Activity,
        private val albumId: String,
        private val albumTitle: String,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(activity.application, albumId, albumTitle) as T
        }
    }
}