package com.wechantloup.pocgallery

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import com.wechantloup.pocgallery.databinding.ActivityAlbumsBinding
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

        binding = ActivityAlbumsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initList()

        subscribeToUpdates()

        if (savedInstanceState != null) return

        lifecycleScope.launch {
            viewModel.getAlbums()
        }
    }

    private fun initList() {
        val columnCount = resources.getInteger(R.integer.albums_column_count)
        binding.listAlbums.apply {
            layoutManager = GridLayoutManager(this@AlbumsActivity, columnCount)
            adapter = AlbumsAdapter()
        }
    }

    private fun subscribeToUpdates() {
        viewModel.stateFlow
            .flowWithLifecycle(lifecycle)
            .onEach {
                it.albums.map {
                    Log.i("TOTO", it.title)
                }
                (binding.listAlbums.adapter as AlbumsAdapter).submitList(it.albums)
            }
            .launchIn(lifecycleScope)
    }
}