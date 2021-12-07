package com.wechantloup.pocgallery.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.util.Log
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@SuppressLint("InlinedApi")
class PhoneAlbumsLoader(
    private val contentResolver: ContentResolver,
    private val continuation: Continuation<List<PhotoAlbum>>,
) {
    private val albums = mutableMapOf<String, PhotoAlbum>()

    fun loadAlbums() {
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(EXTERNAL_CONTENT_URI, PROJECTION_BUCKETS, null, null, SORT_ORDER)

            cursor.ifValid {
                do {
                    val bucketId = getString(getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))

                    if (!albums.containsKey(bucketId)) {
                        albums[bucketId] = createProviderAlbumFromId(bucketId)
                    }
                } while (moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local provider albums", e)
        } finally {
            cursor?.close()
        }

        val albumsList = albums.map { it.value }

        continuation.resume(albumsList)
    }

    private fun Cursor.createProviderAlbumFromId(bucketId: String): PhotoAlbum {
        val bucketName = getString(getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))

        val pathToPhoto = getMostRecentPhotoPath(bucketId)?.toString().orEmpty()
        val count = getCount(bucketId, bucketName)

        return PhotoAlbum(bucketId, bucketName, count, pathToPhoto)
    }

    private fun getMostRecentPhotoPath(bucketId: String): Uri? {
        var cursor: Cursor? = null
        var photoId: Long? = null

        try {
            cursor = contentResolver.query(
                EXTERNAL_CONTENT_URI,
                PROJECTION_PHOTO_PATH,
                "${MediaStore.Images.Media.BUCKET_ID}=?",
                arrayOf(bucketId),
                "${MediaStore.Images.Media.DATE_TAKEN} DESC"
            )

            cursor.ifValid { photoId = getLong(getColumnIndexOrThrow(MediaStore.Images.Media._ID)) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local provider albums", e)
        } finally {
            cursor?.close()
        }

        return photoId?.let { ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, it) }
    }

    private fun getCount(bucketId: String, bucketName: String): Int {
        var cursor: Cursor? = null
        var photoCount = 0

        try {
            cursor = contentResolver.query(
                EXTERNAL_CONTENT_URI,
                null,
                "${MediaStore.Images.Media.BUCKET_ID}=?",
                arrayOf(bucketId),
                null
            )

            cursor.ifValid { photoCount = count }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local provider album identified by id `$bucketName`", e)
        } finally {
            cursor?.close()
        }

        return photoCount
    }

    private fun Cursor?.ifValid(action: Cursor.() -> Unit) {
        if (this != null && moveToFirst()) action()
    }

    companion object {
        private const val TAG = "PhoneAlbumsLoader"

        private const val SORT_ORDER =
            " CASE ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} WHEN  'Camera' THEN 1 ELSE 2 END "
        private val PROJECTION_PHOTO_PATH = arrayOf(MediaStore.Images.Media._ID)
        private val PROJECTION_BUCKETS = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID
        )

        const val ALL_PHOTOS_EMULATED_BUCKET = "ALL_PHOTOS_EMULATED_BUCKET"
    }
}
