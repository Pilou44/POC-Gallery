package com.wechantloup.pocgallery

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.wechantloup.pocgallery.album.AlbumsFragment
import com.wechantloup.pocgallery.databinding.ActivityGalleryBinding
import com.wechantloup.pocgallery.gallery.DatesAdapter
import com.wechantloup.pocgallery.gallery.PhotosAdapter
import com.wechantloup.pocgallery.gallery.PhotosFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class GalleryActivity : AppCompatActivity() {

    val viewModel by viewModels<GalleryViewModel> {
        GalleryViewModel.Factory(this)
    }

    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        subscribeToUpdates()

        if (savedInstanceState != null) {
            title = savedInstanceState.getCharSequence(ARG_TITLE)
            return
        }

        title = "Gallery"
        val fragment = AlbumsFragment()
        supportFragmentManager.beginTransaction()
            .replace(binding.galleryContainer.id, fragment, TAG_FRAGMENT_ALBUMS)
            .commit()
    }

    override fun onBackPressed() {
        title = "Gallery"
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(ARG_TITLE, title)
    }

    fun showAlbum(albumId: String, albumTitle: String) {
        title = albumTitle
        viewModel.onAlbumClicked(albumId)
        val fragment = PhotosFragment()
        supportFragmentManager.beginTransaction()
            .replace(binding.galleryContainer.id, fragment, TAG_FRAGMENT_PHOTOS)
            .addToBackStack(null)
            .commit()
    }

    private fun subscribeToUpdates() {
        viewModel.stateFlow
            .flowWithLifecycle(lifecycle)
            .onEach {
                val text = "${it.selectedPictures.size} selected photos"
                binding.tvPhotosCount.text = text
            }
            .launchIn(lifecycleScope)
    }

    companion object {
        private const val TAG_FRAGMENT_ALBUMS = "fragment_albums"
        private const val TAG_FRAGMENT_PHOTOS = "fragment_photos"
        private const val ARG_TITLE = "title"
    }
}