package com.wechantloup.pocgallery.gallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.wechantloup.pocgallery.R
import com.wechantloup.pocgallery.databinding.ActivityGalleryBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding

    private val viewModel by viewModels<GalleryViewModel> {
        val albumId = requireNotNull(intent.getStringExtra(ARG_ALBUM_ID))
        val albumTitle = requireNotNull(intent.getStringExtra(ARG_ALBUM_TITLE))
        GalleryViewModel.Factory(this, albumId, albumTitle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initList()

        subscribeToUpdates()

        if (savedInstanceState != null) return

        lifecycleScope.launch {
            viewModel.getPhotos()
        }
    }

    private fun initList() {
        val columnCount = resources.getInteger(R.integer.gallery_column_count)
        binding.listPhotos.apply {
            layoutManager = GridLayoutManager(this@GalleryActivity, columnCount)
            adapter = GalleryAdapter()
        }
    }

    private fun subscribeToUpdates() {
        viewModel.stateFlow
            .flowWithLifecycle(lifecycle)
            .onEach {
                binding.title.text = it.title
                (binding.listPhotos.adapter as GalleryAdapter).submitList(it.photos)
            }
            .launchIn(lifecycleScope)
    }

    companion object {
        private const val ARG_ALBUM_ID = "album_id"
        private const val ARG_ALBUM_TITLE = "album_title"
        fun createIntent(context: Context, albumId: String, albumTitle: String): Intent {
            val intent = Intent(context, GalleryActivity::class.java)
            intent.putExtra(ARG_ALBUM_ID, albumId)
            intent.putExtra(ARG_ALBUM_TITLE, albumTitle)
            return intent
        }
    }
}