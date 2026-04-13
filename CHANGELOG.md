# Changelog

All notable changes to AndroDM are documented here.

## [1.0.0] — Production Release

### Summary
Full consolidation of all development branches into a single stable `main` build,
ready for APK/AAB generation and Play Store release.

### Branches merged

| Branch | Description |
|--------|-------------|
| `copilot/fix-all-kinds-of-error` | Gradle wrapper restoration, `.gitignore`, initial build fixes |
| `copilot/fix-all-errors-for-production` | CI non-blocking dependency graph submission |
| `copilot/fix-ui-layout-overflow` | UI improvements, layout overflow fixes, status-chip colours |
| `copilot/implement-video-audio-download` | `VideoDetailActivity`, format/playlist support, `VideoInfoService` |
| `copilot/feature-select-download-quality` | Quality selection bottom-sheet, `VideoQuality` model |
| `copilot/finalize-merge-all-branches` | `FormatAdapter` colour fix, final integration polish |
| `copilot/fix-incompatible-compose-runtime-error` | Compose runtime version alignment |

### Features included in v1.0.0

- MVVM architecture with Room + LiveData + ViewModel
- OkHttp-based download engine with parallel downloads and HTTP Range pause/resume
- `VideoInfoService` for direct-URL and YouTube metadata extraction
- `VideoDetailActivity` showing thumbnail, title, and available formats
- Format selection fragment with audio-only / video+audio differentiation
- Playlist enumeration fragment
- Quality selection bottom-sheet (`QualitySelectionBottomSheet`)
- Foreground download service with progress notifications
- Notification action receiver (pause/resume/cancel from notification shade)
- Room database with `DownloadDao`, type converters, and migration-safe schema
- Material 3 theming with night-mode support
- Coil-powered async thumbnail loading
- GitHub Actions CI workflow (`./gradlew build`)
