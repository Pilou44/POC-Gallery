package com.wechantloup.pocgallery.album

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.wechantloup.pocgallery.R
import com.wechantloup.pocgallery.databinding.ActivityAlbumsBinding
import com.wechantloup.pocgallery.gallery.GalleryActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AlbumsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumsBinding

    private val viewModel by viewModels<AlbumsViewModel> {
        AlbumsViewModel.Factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Gallery"

        binding = ActivityAlbumsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initList()

        subscribeToUpdates()

        if (savedInstanceState != null) return

        lifecycleScope.launch {
            viewModel.getAlbums()
        }
    }

    private fun onAlbumClicked(id: String, albumTitle: String) {
        val intent = GalleryActivity.createIntent(this, id, albumTitle)
        startActivity(intent)
    }

    private fun initList() {
        val columnCount = resources.getInteger(R.integer.albums_column_count)
        binding.listAlbums.apply {
            layoutManager = GridLayoutManager(this@AlbumsActivity, columnCount)
            adapter = AlbumsAdapter(::onAlbumClicked)
        }
    }

    private fun subscribeToUpdates() {
        viewModel.stateFlow
            .flowWithLifecycle(lifecycle)
            .onEach {
                (binding.listAlbums.adapter as AlbumsAdapter).submitList(it.albums)
            }
            .launchIn(lifecycleScope)
    }
}