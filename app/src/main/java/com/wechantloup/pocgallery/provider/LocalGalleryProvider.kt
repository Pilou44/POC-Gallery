package com.wechantloup.pocgallery.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.wechantloup.pocgallery.provider.PhoneAlbumsLoader.Companion.ALL_PHOTOS_EMULATED_BUCKET
import kotlin.coroutines.suspendCoroutine

class LocalGalleryProvider(private val context: Context) {

    private var albumsLoading = false
    private var lastLoadedPageNumber = -1
    private var firstLoadedPageNumber = 0
    private var currentAlbumId: String? = null

    private val currentAlbumFetchedPhotos: MutableList<Photo> = mutableListOf()
    private val fetchedAlbums = arrayListOf<PhotoAlbum>()

//    fun getName(): Int = NAME_RES
//    fun getIcon(): Int = ICON_RES
//    fun getMonochromeIcon(): Int = MONOCHROME_ICON_RES

    fun needLogin(): Boolean = false

    fun askForLogin() {
        // no login necessary for local gallery provider
    }

    fun hasMoreAlbums(): Boolean = false

    fun openAlbum(albumId: String) {
        currentAlbumId = albumId
        currentAlbumFetchedPhotos.clear()
        setCurrentPage(0)
    }

    fun hasMorePhotos(): Boolean {
        val currentAlbum = fetchedAlbums.find { it.id == currentAlbumId } ?: return true
        return currentAlbumFetchedPhotos.count() < currentAlbum.photoCount
    }

    fun canOpenPhotosAtAnyPage(): Boolean = true
    fun canOpenAlbumsAtAnyPage(): Boolean = false

    fun resetPagination() {
        currentAlbumId = null
        setCurrentPage(0)
    }

    fun setCurrentPage(page: Int) {
        lastLoadedPageNumber = page - 1
        firstLoadedPageNumber = page
    }

    fun getLastLoadedPage(): Int = lastLoadedPageNumber

    suspend fun getNextAlbums(): List<PhotoAlbum> {
        albumsLoading = true

        val albums: List<PhotoAlbum> = suspendCoroutine {
            PhoneAlbumsLoader(context.contentResolver, it).loadAlbums()
        }

        val defaultPhotoAlbum = createDefaultPhotoAlbum(albums)

        fetchedAlbums.clear()
        fetchedAlbums.add(defaultPhotoAlbum)
        fetchedAlbums.addAll(albums)

        albumsLoading = false

        return fetchedAlbums
    }

    suspend fun getPreviousAlbums(): List<PhotoAlbum> {
        throw IllegalStateException("Unsupported feature")
    }

    suspend fun getNextPhotos(): List<Photo> {
        if (!hasMorePhotos()) return emptyList()

        val album = fetchedAlbums.find { it.id == currentAlbumId } ?: return emptyList()

        lastLoadedPageNumber++

        val photos = fetchGalleryImages(album.title, currentAlbumId, lastLoadedPageNumber)

        currentAlbumFetchedPhotos.addAll(photos)

        return photos
    }

    suspend fun getPreviousPhotos(): List<Photo> {
        if (!hasMorePhotos()) return emptyList()

        val album = fetchedAlbums.find { it.id == currentAlbumId } ?: return emptyList()

        if (currentAlbumFetchedPhotos.isEmpty()) firstLoadedPageNumber = 0

        if (firstLoadedPageNumber == 0) return emptyList()

        firstLoadedPageNumber--

        val photos = fetchGalleryImages(album.title, currentAlbumId, firstLoadedPageNumber)

        currentAlbumFetchedPhotos.addAll(0, photos)

        return photos
    }

    fun getLoginActivityResultAction(): ((ActivityResult) -> Unit)? = null

    fun setLoginActivityResultLauncher(activityResultLauncher: ActivityResultLauncher<Intent>) {
        // do nothing, no log in associated to the local gallery provider
    }

    private fun createDefaultPhotoAlbum(albums: List<PhotoAlbum>): PhotoAlbum {
        return PhotoAlbum(
            ALL_PHOTOS_EMULATED_BUCKET,
            getAllLocalPhotosTitle(),
            getAllLocalPhotosCount(),
            albums.firstOrNull()?.coverPhotoPath.orEmpty(),
        )
    }

    private fun getAllLocalPhotosTitle(): String = "Gallery"

    private fun getAllLocalPhotosCount(): Int =
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor == null) {
                0
            } else {
                cursor.moveToFirst()
                cursor.count
            }
        }

    @SuppressLint("InlinedApi")
    private fun fetchGalleryImages(
        albumName: String,
        albumId: String?,
        pageNumber: Int,
    ): List<Photo> {
        val selection: String?
        val selectionArgs: Array<String>?
        if (albumId == ALL_PHOTOS_EMULATED_BUCKET) {
            selection = null
            selectionArgs = null
        } else {
            selection = MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME + "=?"
            selectionArgs = arrayOf(albumName)
        }

        val cursor = getCursor(selection, selectionArgs, pageNumber)
        return cursor.getNextPhotos()
    }

    @SuppressLint("InlinedApi")
    private fun getCursor(selection: String?, selectionArgs: Array<String>?, pageNumber: Int): Cursor? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Get All data in Cursor by sorting in DESC order
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PHOTO_PROJECTION,
                Bundle().apply {
                    // Limit & Offset
                    putInt(ContentResolver.QUERY_ARG_LIMIT, PAGINATION_COUNT)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, pageNumber * PAGINATION_COUNT)
                    // Sort function
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    // Selection
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        selectionArgs
                    )
                },
                null
            )
        } else {
            val sortOrder = MediaStore.Images.ImageColumns.DATE_TAKEN +
                " DESC LIMIT $PAGINATION_COUNT OFFSET ${pageNumber * PAGINATION_COUNT}"

            // Get All data in Cursor by sorting in DESC order
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PHOTO_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )
        }

    private fun Cursor?.getNextPhotos(): List<Photo> {
        if (this == null) return emptyList()

        val result = mutableListOf<Photo>()
        use { cursor ->
            if (cursor.isAfterLast) cursor.moveToFirst(); cursor.moveToPrevious() // reinit the cursor since it is used in several albums
            if (cursor.count <= 0) return emptyList()

            val mimeTypeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

            cursor.moveToPosition(-1)
            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeTypeCol)
                if (!isAllowedMimeType(mimeType)) continue

                result.addNextPhoto(cursor)
            }
        }

        return result
    }

    private fun isAllowedMimeType(mimeType: String?): Boolean {
        if (mimeType == null) return false
        return UNSUPPORTED_IMG_TYPE.none { mimeType.endsWith(it) }
    }

    @SuppressLint("InlinedApi")
    private fun MutableList<Photo>.addNextPhoto(cursor: Cursor) {
        val cursorDateReader = CursorDateReader(cursor)
        val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
        val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        val orientationCol = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION)

        val width = cursor.getInt(widthCol)
        val height = cursor.getInt(heightCol)

        if (width == 0 || height == 0) {
            Log.e(TAG, "Null height or width found")
            return
        }

        val photoUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            cursor.getLong(idCol)
        ).toString()

        val orientation = cursor.getInt(orientationCol)
        val newSize = localPhotoSize(width, height, orientation)

        val photo = Photo(
            id = photoUri,
            uri = photoUri,
            widthPx = newSize.width,
            heightPx = newSize.height,
            date = cursorDateReader.getDate()
        )
        add(photo)
    }

    /**
     * Width and height are used for autofill to calculate image ratio and determine the best layout according to it.
     * So if orientation is 90°, we need to invert width and height.
     * Otherwise, orientation is never used.
     */
    private fun localPhotoSize(width: Int, height: Int, orientation: Int): Size =
        if (orientation % 180 != 0) Size(height, width) else Size(width, height)

    companion object {
        private const val TAG = "LocalGalleryProvider"

        private const val PAGINATION_COUNT = 50
        private val UNSUPPORTED_IMG_TYPE = setOf("gif")

        @SuppressLint("InlinedApi")
        private val PHOTO_PROJECTION = arrayOf(
            BaseColumns._ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED
        )
    }
}
