package com.wechantloup.pocgallery.gallery

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wechantloup.pocgallery.R
import com.wechantloup.pocgallery.databinding.ItemDateLayoutBinding
import com.wechantloup.pocgallery.databinding.ItemPhotoLayoutBinding
import com.wechantloup.pocgallery.inflate
import com.wechantloup.pocgallery.provider.Photo

class PhotosAdapter(
    val onPhotoClicked: (Photo) -> Unit,
): ListAdapter<Any, PhotosAdapter.ItemHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return when (ItemType.values().first { it.ordinal == viewType }) {
            ItemType.PHOTO -> PhotoHolder(parent)
            ItemType.DATE -> DateHolder(parent)
        }
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DisplayedPhoto -> ItemType.PHOTO
            is String -> ItemType.DATE
            else -> throw IllegalStateException()
        }.ordinal
    }

    sealed class ItemHolder(view: View): RecyclerView.ViewHolder(view) {
        abstract fun bind(item: Any)
    }

    inner class PhotoHolder(parent: ViewGroup): ItemHolder(parent.inflate(R.layout.item_photo_layout)) {

        private val binding = ItemPhotoLayoutBinding.bind(itemView)

        override fun bind(item: Any) {
            val photo = (item as DisplayedPhoto).photo
            Glide.with(itemView.context)
                .load(photo.uri)
                .into(binding.ivPhoto)
            binding.isSelected.isVisible = item.isSelected
            itemView.setOnClickListener { onPhotoClicked(photo) }
        }
    }

    inner class DateHolder(parent: ViewGroup): ItemHolder(parent.inflate(R.layout.item_date_layout)) {

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
                    oldItem is DisplayedPhoto && newItem is DisplayedPhoto -> oldItem.photo.id == newItem.photo.id
                    oldItem is String && newItem is String -> true
                    else -> false
                }
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is DisplayedPhoto && newItem is DisplayedPhoto -> {
                        oldItem.photo.uri == newItem.photo.uri &&
                            oldItem.photo.widthPx == newItem.photo.widthPx &&
                            oldItem.photo.heightPx == newItem.photo.heightPx &&
                            oldItem.photo.date == newItem.photo.date &&
                            oldItem.isSelected == newItem.isSelected
                    }
                    oldItem is String && newItem is String -> oldItem == newItem
                    else -> false
                }
            }
        }
    }
}