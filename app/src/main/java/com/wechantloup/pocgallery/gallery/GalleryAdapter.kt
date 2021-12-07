package com.wechantloup.pocgallery.gallery

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wechantloup.pocgallery.R
import com.wechantloup.pocgallery.databinding.ItemPhotoLayoutBinding
import com.wechantloup.pocgallery.inflate
import com.wechantloup.pocgallery.provider.Photo

class GalleryAdapter: ListAdapter<Photo, GalleryAdapter.PhotoHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
        return PhotoHolder(parent)
    }

    override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PhotoHolder(parent: ViewGroup): RecyclerView.ViewHolder(parent.inflate(R.layout.item_photo_layout)) {

        private val binding = ItemPhotoLayoutBinding.bind(itemView)

        fun bind(item: Photo) {
            Glide.with(itemView.context)
                .load(item.uri)
                .into(binding.ivPhoto)
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<Photo>() {
            override fun areItemsTheSame(oldItem: Photo, newItem: Photo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Photo, newItem: Photo): Boolean {
                return oldItem.uri == newItem.uri &&
                        oldItem.widthPx == newItem.widthPx &&
                        oldItem.heightPx == newItem.heightPx &&
                        oldItem.date == newItem.date
            }
        }
    }
}