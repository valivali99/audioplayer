package com.valivali99.audioplayer

import android.app.Application
import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

private val diacritics = Regex("\\p{Mn}+")

/** Lowercase without accents, so "beyonce" finds "Beyoncé". */
private fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(diacritics, "").lowercase()

/** Library + playback state for the UI, talking to [PlaybackService] through a MediaController. */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    var songs by mutableStateOf<List<Song>>(emptyList()); private set
    var libraryLoaded by mutableStateOf(false); private set

    /** Text typed in the library's search bar. */
    var query by mutableStateOf("")

    /** [songs] narrowed to those whose title, artist or album contain every word of [query]. */
    val visibleSongs by derivedStateOf {
        val words = normalize(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) songs
        else songs.filter { song ->
            val text = normalize("${song.title} ${song.artist} ${song.album}")
            words.all { it in text }
        }
    }

    var currentId by mutableStateOf<Long?>(null); private set
    var currentTitle by mutableStateOf(""); private set
    var currentArtist by mutableStateOf(""); private set
    var isPlaying by mutableStateOf(false); private set
    var shuffleOn by mutableStateOf(false); private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF); private set
    var positionMs by mutableLongStateOf(0L); private set
    var durationMs by mutableLongStateOf(0L); private set

    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            runCatching { controllerFuture.get() }.onSuccess { c ->
                controller = c
                c.addListener(listener)
                sync(c)
            }
        }, ContextCompat.getMainExecutor(app))

        // The player doesn't report position continuously, so poll it while playing.
        viewModelScope.launch {
            while (true) {
                controller?.let { if (it.isPlaying) positionMs = it.currentPosition }
                delay(500)
            }
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            songs = withContext(Dispatchers.IO) { AudioLibrary.load(getApplication()) }
            libraryLoaded = true
        }
    }

    /** Plays [song], queueing the whole library around it so next/previous/shuffle work. */
    fun play(song: Song) {
        val c = controller ?: return
        val index = songs.indexOf(song).takeIf { it >= 0 } ?: return
        if (queueIsLibrary(c)) c.seekToDefaultPosition(index)
        else c.setMediaItems(songs.map { it.toMediaItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    fun shuffleAll() {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val index = songs.indices.random()
        if (queueIsLibrary(c)) c.seekToDefaultPosition(index)
        else c.setMediaItems(songs.map { it.toMediaItem() }, index, 0L)
        c.shuffleModeEnabled = true
        c.prepare()
        c.play()
    }

    @OptIn(UnstableApi::class)
    fun togglePlayPause() {
        controller?.let { Util.handlePlayPauseButtonAction(it) }
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    /** Off → repeat all → repeat one → off. */
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    private fun sync(p: Player) {
        currentId = p.currentMediaItem?.mediaId?.toLongOrNull()
        currentTitle = p.mediaMetadata.title?.toString().orEmpty()
        currentArtist = p.mediaMetadata.artist?.toString().orEmpty()
        isPlaying = p.isPlaying
        shuffleOn = p.shuffleModeEnabled
        repeatMode = p.repeatMode
        positionMs = p.currentPosition
        durationMs = p.duration.takeIf { it != C.TIME_UNSET } ?: 0L
    }

    /** True when the player's queue is already the current library (cheap check on size and ends). */
    private fun queueIsLibrary(c: Player): Boolean {
        val n = c.mediaItemCount
        return n > 0 && n == songs.size &&
            c.getMediaItemAt(0).mediaId == songs.first().id.toString() &&
            c.getMediaItemAt(n - 1).mediaId == songs.last().id.toString()
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
    }
}
