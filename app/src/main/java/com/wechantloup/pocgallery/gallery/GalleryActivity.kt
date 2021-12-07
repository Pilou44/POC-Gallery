package com.wechantloup.pocgallery.gallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        binding.listPhotos.initPhotoList()
        binding.listDates?.initDateList()

        subscribeToUpdates()

        if (savedInstanceState != null) return

        lifecycleScope.launch {
            viewModel.loadMorePhotos()
        }
    }

    private fun RecyclerView.initDateList() {
        adapter = DatesAdapter(::onDateClicked)
        layoutManager = LinearLayoutManager(this@GalleryActivity)
    }

    private fun onDateClicked(date: String) {
        val adapter = binding.listPhotos.adapter as GalleryAdapter
        val position = adapter.currentList.indexOf(date)
        binding.listPhotos.scrollToPosition(position)
    }

    private fun RecyclerView.initPhotoList() {
        adapter = GalleryAdapter()

        val columnCount = resources.getInteger(R.integer.gallery_column_count)
        val gridLayoutManager = GridLayoutManager(this@GalleryActivity, columnCount)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (adapter?.getItemViewType(position) == GalleryAdapter.ItemType.DATE.ordinal)
                    return gridLayoutManager.spanCount
                return 1
            }
        }
        layoutManager = gridLayoutManager

        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val hasReachBottom = !recyclerView.canScrollVertically(1)

                if (hasReachBottom) {
                    lifecycleScope.launch {
                        viewModel.loadMorePhotos()
                    }
                }
            }
        })
    }

    private fun subscribeToUpdates() {
        viewModel.stateFlow
            .flowWithLifecycle(lifecycle)
            .onEach {
                this.title = it.title
                (binding.listPhotos.adapter as GalleryAdapter).submitList(it.photos)
                (binding.listDates?.adapter as DatesAdapter?)?.submitList(it.dates)
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
