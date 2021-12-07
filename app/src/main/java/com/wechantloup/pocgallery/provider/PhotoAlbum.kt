package com.wechantloup.pocgallery.provider

data class PhotoAlbum(
    val id: String,
    val title: String,
    val photoCount: Int,
    val coverPhotoPath: List<String>,
)
