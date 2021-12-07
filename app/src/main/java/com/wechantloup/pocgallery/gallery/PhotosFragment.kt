package com.wechantloup.pocgallery.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wechantloup.pocgallery.GalleryActivity
import com.wechantloup.pocgallery.GalleryViewModel
import com.wechantloup.pocgallery.R
import com.wechantloup.pocgallery.databinding.FragmentPhotosBinding
import com.wechantloup.pocgallery.provider.Photo
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PhotosFragment : Fragment() {

    private var binding: FragmentPhotosBinding? = null

    private var viewModel: GalleryViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPhotosBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = (activity as GalleryActivity?)?.viewModel

        binding?.listPhotos?.initPhotoList()
        binding?.listDates?.initDateList()

        subscribeToUpdates()

        if (savedInstanceState != null) return

        lifecycleScope.launch {
            viewModel?.loadMorePhotos()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun RecyclerView.initDateList() {
        adapter = DatesAdapter(::onDateClicked)
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun onDateClicked(date: String) {
        val adapter = binding?.listPhotos?.adapter as PhotosAdapter? ?: return
        val position = adapter.currentList.indexOf(date)
        binding?.listPhotos?.scrollToPosition(position)
    }

    private fun RecyclerView.initPhotoList() {
        val viewModel = viewModel ?: return
        adapter = PhotosAdapter(::onPhotoClicked)

        val columnCount = resources.getInteger(R.integer.gallery_column_count)
        val gridLayoutManager = GridLayoutManager(requireContext(), columnCount)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (adapter?.getItemViewType(position) == PhotosAdapter.ItemType.DATE.ordinal)
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
                        viewModel?.loadMorePhotos()
                    }
                }
            }
        })
    }

    private fun onPhotoClicked(photo: Photo) {
        viewModel?.changeSelection(photo)
    }

    private fun subscribeToUpdates() {
        viewModel?.stateFlow
            ?.flowWithLifecycle(lifecycle)
            ?.onEach {
                (binding?.listPhotos?.adapter as PhotosAdapter?)?.submitList(
                    it.photos.map { item ->
                        if (item !is Photo) return@map item
                        val isSelected = it.selectedPictures.firstOrNull { it.id == item.id } != null
                        DisplayedPhoto(item, isSelected)
                    }
                )
                (binding?.listDates?.adapter as DatesAdapter?)?.submitList(it.dates)
            }
            ?.launchIn(lifecycleScope)
    }
}
