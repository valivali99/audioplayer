package com.valivali99.audioplayer

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    /** Last-modified time of the file, in seconds since 1970. */
    val modifiedSec: Long,
) {
    val uri: Uri get() = songUri(id)
}

fun songUri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

fun Song.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .build()

object AudioLibrary {
    /** Every audio file on the device, newest-modified first. */
    fun load(context: Context): List<Song> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        // Skip empty files and the system's ringtone / alarm / notification sounds.
        val selection = "${MediaStore.Audio.Media.DURATION} > 0" +
            " AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0" +
            " AND ${MediaStore.Audio.Media.IS_ALARM} = 0" +
            " AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        val songs = mutableListOf<Song>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val fileName = cursor.getString(nameCol).orEmpty().substringBeforeLast('.')
                val artist = cursor.getString(artistCol)
                songs += Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: fileName,
                    artist = artist?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING } ?: "Unknown artist",
                    album = cursor.getString(albumCol).orEmpty(),
                    durationMs = cursor.getLong(durationCol),
                    // Freshly copied files can be missing their modified date until the scanner catches up.
                    modifiedSec = cursor.getLong(modifiedCol).takeIf { it > 0 } ?: cursor.getLong(addedCol),
                )
            }
        }
        // Sort here too, so files that fell back to their "added" date land in the right place.
        return songs.sortedByDescending { it.modifiedSec }
    }
}
