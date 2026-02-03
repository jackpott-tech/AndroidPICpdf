package com.example.androidpicpdf.util

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale

object MediaUtils {
    fun resolveDateTaken(contentResolver: ContentResolver, uri: Uri): Long {
        val exifDate = runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            }
        }.getOrNull()

        val exifTimestamp = exifDate?.let { parseExifDate(it) }
        if (exifTimestamp != null) {
            return exifTimestamp
        }

        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor.use {
            if (it != null && it.moveToFirst()) {
                val dateTaken = it.getLongOrNull(MediaStore.Images.Media.DATE_TAKEN)
                if (dateTaken != null && dateTaken > 0) {
                    return dateTaken
                }
                val dateModified = it.getLongOrNull(MediaStore.Images.Media.DATE_MODIFIED)
                if (dateModified != null && dateModified > 0) {
                    return dateModified * 1000
                }
            }
        }
        return System.currentTimeMillis()
    }

    private fun parseExifDate(value: String): Long? {
        return runCatching {
            val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            formatter.parse(value)?.time
        }.getOrNull()
    }

    private fun Cursor.getLongOrNull(column: String): Long? {
        val index = getColumnIndex(column)
        if (index == -1) return null
        return getLong(index)
    }
}
