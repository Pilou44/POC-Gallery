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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GalleryViewModel(
    application: Application,
    albumId: String,
    albumTitle: String,
): AndroidViewModel(application) {

    private val _stateFlow = MutableStateFlow(State(albumTitle))
    val stateFlow: StateFlow<State> = _stateFlow

    private val calendar = Calendar.getInstance()
    private val outputDateFormat = SimpleDateFormat(OUTPUT_DATE_FORMAT, Locale.getDefault())

    init {
        LocalGalleryProvider.openAlbum(albumId)
    }

    fun loadMorePhotos() {
        if (!LocalGalleryProvider.hasMorePhotos()) return

        val nextPhotos = LocalGalleryProvider.getNextPhotos(getApplication())
        val photos = mutableListOf<Any>().apply { addAll(stateFlow.value.photos) }
        photos.addWithDateSeparator(nextPhotos)
        _stateFlow.value = stateFlow.value.copy(
            photos = photos
        )
    }

    private fun MutableList<Any>.addWithDateSeparator(photos: List<Photo>) {
        photos.forEach { newPhoto ->
            if (isNewDate(this, newPhoto)) {
                val newDate = newPhoto.getDate()
                add(newDate)
                val currentDates = stateFlow.value.dates
                _stateFlow.value = stateFlow.value.copy(dates = currentDates + newDate)
            }
            add(newPhoto)
        }
    }

    private fun isNewDate(list: List<Any>, newPhoto: Photo): Boolean {
        if (list.isEmpty() || list.last() !is Photo) {
            return true
        }

        return (list.last() as Photo).getDate() != newPhoto.getDate()
    }

    private fun Photo.getDate(): String {
        val currentDate = Date(date)
        calendar.setTimeWithTimeZoneShift(currentDate)
        return outputDateFormat.format(calendar.time)
    }

    private fun Calendar.setTimeWithTimeZoneShift(date: Date) {
        time = date
        timeInMillis += TimeZone.getDefault().getOffset(timeInMillis)
    }

    data class State(
        val title: String,
        val photos: List<Any> = emptyList(),
        val dates: List<String> = emptyList(),
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

    companion object {
        private const val OUTPUT_DATE_FORMAT = "dd MMMM yyyy"
    }
}