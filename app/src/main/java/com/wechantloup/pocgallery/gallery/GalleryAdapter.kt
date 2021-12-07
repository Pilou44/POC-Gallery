package com.wechantloup.pocgallery.gallery

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wechantloup.pocgallery.R
import com.wechantloup.pocgallery.databinding.ItemDateLayoutBinding
import com.wechantloup.pocgallery.databinding.ItemPhotoLayoutBinding
import com.wechantloup.pocgallery.inflate
import com.wechantloup.pocgallery.provider.Photo

class GalleryAdapter: ListAdapter<Any, GalleryAdapter.GalleryHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryHolder {
        return when (ItemType.values().first { it.ordinal == viewType }) {
            ItemType.PHOTO -> PhotoHolder(parent)
            ItemType.DATE -> DateHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: GalleryHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Photo -> ItemType.PHOTO
            is String -> ItemType.DATE
            else -> throw IllegalStateException()
        }.ordinal
    }

    sealed class GalleryHolder(view: View): RecyclerView.ViewHolder(view) {
        abstract fun bind(item: Any)
    }

    inner class PhotoHolder(parent: ViewGroup): GalleryHolder(parent.inflate(R.layout.item_photo_layout)) {

        private val binding = ItemPhotoLayoutBinding.bind(itemView)

        override fun bind(item: Any) {
            val photo = item as Photo
            Glide.with(itemView.context)
                .load(photo.uri)
                .into(binding.ivPhoto)
        }
    }

    inner class DateHolder(parent: ViewGroup): GalleryHolder(parent.inflate(R.layout.item_date_layout)) {

        private val binding = ItemDateLayoutBinding.bind(itemView)

        override fun bind(item: Any) {
            val date = item as String
            binding.tvDate.text = date
        }
    }

    enum class ItemType {
        PHOTO,
        DATE,
    }

    companion object {

        private val diffCallback = object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is Photo && newItem is Photo -> oldItem.id == newItem.id
                    oldItem is String && newItem is String -> true
                    else -> false
                }
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is Photo && newItem is Photo -> {
                        oldItem.uri == newItem.uri &&
                            oldItem.widthPx == newItem.widthPx &&
                            oldItem.heightPx == newItem.heightPx &&
                            oldItem.date == newItem.date
                    }
                    oldItem is String && newItem is String -> oldItem == newItem
                    else -> false
                }
            }
        }
    }
}