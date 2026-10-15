package com.valivali99.audioplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valivali99.audioplayer.ui.AudioPlayerTheme
import com.valivali99.audioplayer.ui.LibraryScreen
import com.valivali99.audioplayer.ui.NowPlayingScreen
import com.valivali99.audioplayer.ui.PermissionScreen

private val audioPermission =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            AudioPlayerTheme { AudioPlayerApp(this) }
        }
    }
}

@Composable
private fun AudioPlayerApp(activity: ComponentActivity, vm: PlayerViewModel = viewModel()) {
    fun granted() = ContextCompat.checkSelfPermission(activity, audioPermission) == PackageManager.PERMISSION_GRANTED

    var hasPermission by remember { mutableStateOf(granted()) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    // After a "Don't allow" Android may stop showing the dialog; then we send people to Settings.
    var blocked by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasPermission = ok
        askedOnce = true
        blocked = !ok && !activity.shouldShowRequestPermissionRationale(audioPermission)
        if (ok) vm.refreshLibrary()
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !askedOnce) launcher.launch(audioPermission)
    }

    // Coming back to the app: pick up a permission granted in Settings and any new or changed files.
    LifecycleResumeEffect(Unit) {
        hasPermission = granted()
        if (hasPermission) vm.refreshLibrary()
        onPauseOrDispose { }
    }

    if (!hasPermission) {
        PermissionScreen(
            showSettings = blocked,
            onRequest = { launcher.launch(audioPermission) },
            onOpenSettings = {
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null))
                )
            },
        )
        return
    }

    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LibraryScreen(vm, onOpenNowPlaying = { showNowPlaying = true })
        AnimatedVisibility(
            visible = showNowPlaying && vm.currentId != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            NowPlayingScreen(vm, onClose = { showNowPlaying = false })
        }
    }
    BackHandler(enabled = showNowPlaying) { showNowPlaying = false }
}
