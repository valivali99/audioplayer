package com.valivali99.audioplayer.ui

import android.content.Context
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valivali99.audioplayer.songUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A song's embedded cover art, or a gray tile with a note when it has none. */
@Composable
fun Artwork(songId: Long?, modifier: Modifier = Modifier, sizePx: Int = 160, corner: Dp = 8.dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, songId, sizePx) {
        value = songId?.let { ArtworkCache.load(context, it, sizePx) }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(Palette.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        val art = bitmap
        if (art != null) {
            Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Palette.Muted, modifier = Modifier.size(28.dp))
        }
    }
}

private object ArtworkCache {
    private val cache = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    // Songs without cover art, so we don't retry them; touched from several threads.
    private val missing = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()

    suspend fun load(context: Context, id: Long, sizePx: Int): ImageBitmap? {
        // Thumbnails from the media store need Android 10+; older phones show the placeholder.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || id in missing) return null
        val key = "$id:$sizePx"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.loadThumbnail(songUri(id), Size(sizePx, sizePx), null)
                    .asImageBitmap()
                    .also { cache.put(key, it) }
            } catch (e: Exception) {
                missing += id
                null
            }
        }
    }
}
