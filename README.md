# Audio Player

A minimal Android music player: it finds every audio file on the phone, lists them newest-modified first, and plays them with a gray and black interface.

- Library of all audio on the device (ringtone / alarm / notification sounds are skipped), sorted by last-modified date
- Play / pause, next / previous, seek bar
- Shuffle (and "Shuffle all"), repeat off / all / one
- Keeps playing in the background, with notification and lock-screen controls
- Pauses automatically when headphones are unplugged

Built with Kotlin, Jetpack Compose and Media3 (ExoPlayer). Android 8.0+.

## Build

Needs Android Studio (or the Android SDK) and JDK 21.

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Or open the folder in Android Studio and press Run.

## Code

| File | What it does |
|---|---|
| `AudioLibrary.kt` | Reads the phone's audio files from MediaStore |
| `PlaybackService.kt` | Background player + media notification |
| `PlayerViewModel.kt` | Library and playback state for the UI |
| `ui/LibraryScreen.kt` | Track list, mini player, permission screen |
| `ui/NowPlayingScreen.kt` | Full-screen player |
