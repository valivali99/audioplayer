package com.valivali99.audioplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.valivali99.audioplayer.PlayerViewModel

@Composable
fun NowPlayingScreen(vm: PlayerViewModel, onClose: () -> Unit) {
    val song = vm.songs.firstOrNull { it.id == vm.currentId }
    val title = vm.currentTitle.ifEmpty { song?.title.orEmpty() }
    val artist = vm.currentArtist.ifEmpty { song?.artist.orEmpty() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.SurfaceHigh, Palette.Black, Palette.Black)))
            // Swallow taps so they don't reach the list underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = Palette.Text, modifier = Modifier.size(32.dp))
                }
                Text(
                    "NOW PLAYING",
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f).padding(end = 48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f))
            Artwork(
                vm.currentId,
                Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(1f),
                sizePx = 900,
                corner = 16.dp,
            )
            Spacer(Modifier.weight(1f))

            Column(Modifier.fillMaxWidth()) {
                Text(title, color = Palette.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(artist, color = Palette.TextDim, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(20.dp))
            SeekBar(vm)
            Spacer(Modifier.height(12.dp))
            Controls(vm)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SeekBar(vm: PlayerViewModel) {
    // While dragging, show the drag position; seek only when the finger lifts.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = vm.durationMs.coerceAtLeast(1L).toFloat()
    val value = dragging ?: vm.positionMs.toFloat().coerceIn(0f, duration)

    Slider(
        value = value,
        onValueChange = { dragging = it },
        onValueChangeFinished = {
            dragging?.let { vm.seekTo(it.toLong()) }
            dragging = null
        },
        valueRange = 0f..duration,
        colors = SliderDefaults.colors(
            thumbColor = Palette.White,
            activeTrackColor = Palette.White,
            inactiveTrackColor = Palette.Track,
        ),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatTime(value.toLong()), color = Palette.TextDim, fontSize = 12.sp)
        Text(formatTime(vm.durationMs), color = Palette.TextDim, fontSize = 12.sp)
    }
}

@Composable
private fun Controls(vm: PlayerViewModel) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = vm::toggleShuffle) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = if (vm.shuffleOn) "Shuffle on" else "Shuffle off",
                tint = if (vm.shuffleOn) Palette.White else Palette.Muted,
                modifier = Modifier.size(26.dp),
            )
        }
        IconButton(onClick = vm::previous, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Palette.Text, modifier = Modifier.size(40.dp))
        }
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Palette.White)
                .clickable(onClick = vm::togglePlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (vm.isPlaying) "Pause" else "Play",
                tint = Palette.Black,
                modifier = Modifier.size(42.dp),
            )
        }
        IconButton(onClick = vm::next, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Palette.Text, modifier = Modifier.size(40.dp))
        }
        IconButton(onClick = vm::cycleRepeat) {
            Icon(
                if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = when (vm.repeatMode) {
                    Player.REPEAT_MODE_ONE -> "Repeat one"
                    Player.REPEAT_MODE_ALL -> "Repeat all"
                    else -> "Repeat off"
                },
                tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) Palette.Muted else Palette.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
