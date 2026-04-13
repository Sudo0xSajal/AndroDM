# AndroDM — Android Video Downloader

AndroDM is a production-grade Android video downloader app built with Kotlin, following MVVM architecture and Material 3 design.

## Features

- **Multi-source downloads** — supports direct media URLs and YouTube links
- **Format & quality selection** — choose resolution, fps, and audio/video-only formats
- **Playlist support** — enumerate and download individual videos from playlists
- **Background downloads** — foreground service with pause/resume via HTTP Range headers
- **Download history** — Room-backed local database with LiveData-driven UI
- **Thumbnail previews** — async image loading via Coil
- **Dark mode** — Material 3 dynamic colour with night theme support

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | XML layouts, Material 3, ViewBinding |
| Architecture | MVVM (ViewModel + LiveData) |
| Database | Room (with KSP) |
| Networking | OkHttp 4 |
| Image loading | Coil 2 |
| Async | Kotlin Coroutines |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

## Project Structure

```
app/src/main/java/com/vmate/downloader/
├── core/
│   ├── download/        # DownloadManager (parallel, pause/resume)
│   └── network/         # HttpClientFactory, VideoDetector, VideoInfoService
├── data/
│   ├── local/           # Room DAO, Database, Converters
│   └── repository/      # DownloadRepository
├── domain/
│   └── models/          # Download, Video, VideoInfo, FormatInfo, VideoQuality, …
├── presentation/
│   ├── ui/adapter/      # RecyclerView adapters
│   ├── ui/fragments/    # DownloadList, FormatSelection, Playlist, QualitySelection
│   ├── viewmodel/       # DownloadViewModel
│   ├── MainActivity
│   └── VideoDetailActivity
└── service/
    ├── DownloadForegroundService
    └── DownloadNotificationReceiver
```

## Build & Run

**Prerequisites:** Android Studio Hedgehog (or later), JDK 17, Android SDK 35.

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (configure signing in app/build.gradle.kts first)
./gradlew assembleRelease

# Run all checks
./gradlew build
```

The debug APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

## CI

GitHub Actions runs `./gradlew build` on every push and pull request (see `.github/workflows/gradle.yml`).

## Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes with a clear message.
4. Push and open a pull request against `main`.

## License

This project is licensed under the MIT License.