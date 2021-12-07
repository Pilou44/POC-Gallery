package com.wechantloup.pocgallery.provider

open class Photo(
    val id: String,
    val uri: String,
    val widthPx: Int,
    val heightPx: Int,
    val date: Long? = null,
    var hash: String? = null,
)
