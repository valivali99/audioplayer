package com.valivali99.audioplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valivali99.audioplayer.PlayerViewModel
import com.valivali99.audioplayer.Song

@Composable
fun LibraryScreen(vm: PlayerViewModel, onOpenNowPlaying: () -> Unit) {
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxSize().background(Palette.Black)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item(key = "header") { Header(vm) }
            items(vm.songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == vm.currentId,
                    onClick = { vm.play(song) },
                )
            }
        }
        if (vm.currentId != null) MiniPlayer(vm, onOpenNowPlaying)
        else Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun Header(vm: PlayerViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
    ) {
        Text("Library", color = Palette.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        val subtitle = when {
            !vm.libraryLoaded -> "Loading…"
            vm.songs.isEmpty() -> "No audio files found on this phone"
            else -> "${vm.songs.size} tracks · recently modified first"
        }
        Text(subtitle, color = Palette.TextDim, fontSize = 14.sp)
        if (vm.songs.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = vm::shuffleAll,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.SurfaceHigh, contentColor = Palette.White),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Shuffle all", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isCurrent) Palette.Surface else Palette.Black)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(song.id, Modifier.size(48.dp), sizePx = 144, corner = 6.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isCurrent) Palette.White else Palette.Text,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${song.artist} · ${formatTime(song.durationMs)}",
                color = Palette.TextDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (isCurrent) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = "Now playing", tint = Palette.White, modifier = Modifier.size(20.dp))
        } else {
            Text(formatDate(song.modifiedSec), color = Palette.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MiniPlayer(vm: PlayerViewModel, onOpen: () -> Unit) {
    val title = vm.currentTitle.ifEmpty { vm.songs.firstOrNull { it.id == vm.currentId }?.title.orEmpty() }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.Surface)
            .navigationBarsPadding()
            .clickable(onClick = onOpen),
    ) {
        val progress = if (vm.durationMs > 0) vm.positionMs.toFloat() / vm.durationMs else 0f
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = Palette.White,
            trackColor = Palette.Track,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(vm.currentId, Modifier.size(44.dp), sizePx = 144, corner = 6.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, color = Palette.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(vm.currentArtist, color = Palette.TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = vm::togglePlayPause) {
                Icon(
                    if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (vm.isPlaying) "Pause" else "Play",
                    tint = Palette.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            IconButton(onClick = vm::next) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Palette.Text, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun PermissionScreen(showSettings: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.Black).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Artwork(null, Modifier.size(96.dp), corner = 20.dp)
            Spacer(Modifier.height(24.dp))
            Text("Allow access to your music", color = Palette.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Audio Player needs permission to find the audio files on this phone. Nothing leaves your device.",
                color = Palette.TextDim,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = if (showSettings) onOpenSettings else onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Palette.White, contentColor = Palette.Black),
                shape = RoundedCornerShape(50),
            ) {
                Text(if (showSettings) "Open settings" else "Allow access", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
