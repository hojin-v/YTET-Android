#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

required=(
  "$root/settings.gradle"
  "$root/build.gradle"
  "$root/app/build.gradle"
  "$root/app/src/main/AndroidManifest.xml"
  "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
  "$root/app/src/main/java/com/ytet/android/extract/ExtractionService.java"
  "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
  "$root/app/src/main/java/com/ytet/android/update/UpdateChecker.java"
  "$root/app/src/main/java/com/ytet/android/update/UpdateApkProvider.java"
  "$root/app/src/main/java/com/ytet/android/update/UpdateDownloadService.java"
  "$root/app/src/main/java/com/ytet/android/update/UpdateInfo.java"
  "$root/app/src/main/java/com/ytet/android/stream/StationCatalog.java"
  "$root/app/src/main/java/com/ytet/android/library/MusicLibrary.java"
  "$root/app/src/main/java/com/ytet/android/extract/YtDlpPythonEngine.java"
  "$root/app/src/main/java/com/ytet/android/extract/MediaTrackMuxer.java"
  "$root/app/src/main/java/com/ytet/android/extract/ExtractionOutputs.java"
  "$root/app/src/main/java/com/ytet/android/extract/StorageWriter.java"
  "$root/app/src/main/java/com/ytet/android/core/DefaultMediaPaths.java"
  "$root/app/src/main/res/drawable-nodpi/rabbyt_launcher_icon.png"
  "$root/app/src/main/res/drawable/ic_stat_playback.xml"
  "$root/app/src/main/res/drawable/ic_keyboard_arrow_down.xml"
  "$root/app/src/main/res/drawable/ic_play_arrow.xml"
  "$root/app/src/main/res/drawable/ic_pause.xml"
  "$root/app/src/main/res/drawable/ic_skip_previous.xml"
  "$root/app/src/main/res/drawable/ic_skip_next.xml"
  "$root/app/src/main/res/drawable/ic_shuffle.xml"
  "$root/app/src/main/res/drawable/ic_repeat.xml"
  "$root/app/src/main/res/drawable/ic_repeat_one.xml"
  "$root/app/src/main/res/drawable/ic_timer.xml"
  "$root/app/src/main/res/drawable/ic_queue_music.xml"
  "$root/app/src/main/res/drawable/ic_search.xml"
  "$root/app/src/main/res/drawable/ic_view_list.xml"
  "$root/app/src/main/res/drawable/ic_grid_view.xml"
  "$root/app/src/main/res/drawable/ic_more_vert.xml"
  "$root/app/src/main/python/ytet_ydl.py"
  "$root/app/src/test/java/com/ytet/android/core/YoutubeUrlValidatorTest.java"
  "$root/app/src/test/java/com/ytet/android/core/ExtractionRequestTest.java"
  "$root/app/src/test/java/com/ytet/android/core/FormatMappingTest.java"
  "$root/app/src/test/java/com/ytet/android/extract/ExtractionOutputsTest.java"
  "$root/app/src/test/java/com/ytet/android/extract/MediaTrackMuxerTest.java"
  "$root/app/src/test/java/com/ytet/android/extract/StorageWriterTest.java"
  "$root/app/src/test/java/com/ytet/android/stream/StationCatalogTest.java"
  "$root/app/src/test/java/com/ytet/android/library/MusicLibraryTest.java"
  "$root/app/src/test/java/com/ytet/android/update/UpdateCheckerTest.java"
  "$root/scripts/test_python_engine.sh"
  "$root/scripts/verify_apk_runtime.sh"
  "$root/app/src/main/java/com/ytet/android/core/YoutubeUrlValidator.java"
)

for path in "${required[@]}"; do
  if [[ ! -f "$path" ]]; then
    echo "missing: $path" >&2
    exit 1
  fi
done

grep -q 'com.android.application' "$root/build.gradle"
grep -q 'com.chaquo.python' "$root/build.gradle"
grep -q 'compileSdk = 36' "$root/app/build.gradle"
grep -q 'abiFilters "arm64-v8a"' "$root/app/build.gradle"
grep -q 'release {' "$root/app/build.gradle"
grep -q 'signingConfig = signingConfigs.ytetRelease' "$root/app/build.gradle"
grep -q 'testImplementation "junit:junit:4.13.2"' "$root/app/build.gradle"
grep -q 'com.mrljdx:ffmpeg-kit-full:6.1.4' "$root/app/build.gradle"
grep -q 'yt-dlp==2026.3.17' "$root/app/build.gradle"
grep -q 'yt-dlp-ejs==0.8.0' "$root/app/build.gradle"
grep -q 'mutagen==1.47.0' "$root/app/build.gradle"
grep -q 'flavorDimensions "channel"' "$root/app/build.gradle"
grep -q 'applicationId = "com.ytet.android.beta"' "$root/app/build.gradle"
grep -q 'UPDATE_CHANNEL' "$root/app/build.gradle"
grep -q 'android.permission.FOREGROUND_SERVICE' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android.permission.REQUEST_INSTALL_PACKAGES' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android:allowBackup="false"' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android:icon="@drawable/rabbyt_launcher_icon"' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android:foregroundServiceType="mediaPlayback"' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android:name=".update.UpdateApkProvider"' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android:name=".update.UpdateDownloadService"' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android:authorities="${updateApkAuthority}"' "$root/app/src/main/AndroidManifest.xml"
grep -q 'RabbYT Beta' "$root/app/src/nightly/res/values/strings.xml"
grep -q 'ACTION_OPEN_DOCUMENT_TREE' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'new YtDlpPythonEngine()' "$root/app/src/main/java/com/ytet/android/extract/ExtractionService.java"
grep -q 'MediaSession' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'Notification.MediaStyle' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'ACTION_TOGGLE_SHUFFLE' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'ACTION_TOGGLE_REPEAT' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'ACTION_SET_SLEEP_TIMER' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'ACTION_SLEEP_TIMER_FINISHED' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'ACTION_AUDIO_BECOMING_NOISY' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'pauseForOutputDisconnect' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'PREF_PLAYBACK_QUEUE_IDS' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'restoreQueueAsync' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'EXTRA_ALBUM_ART_URI' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'EXTRA_QUEUE_TRACK_IDS' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'GitHub releases request' "$root/app/src/main/java/com/ytet/android/update/UpdateChecker.java"
grep -q 'latestStableUpdateFromJson' "$root/app/src/main/java/com/ytet/android/update/UpdateChecker.java"
grep -q 'latestNightlyUpdateFromJson' "$root/app/src/main/java/com/ytet/android/update/UpdateChecker.java"
grep -q 'STABLE_APK_ASSET_PATTERN' "$root/app/src/main/java/com/ytet/android/update/UpdateChecker.java"
grep -q 'NIGHTLY_APK_ASSET_PATTERN' "$root/app/src/main/java/com/ytet/android/update/UpdateChecker.java"
grep -q 'BuildConfig.UPDATE_CHANNEL' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'UpdateApkProvider.uriFor' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'ACTION_INSTALL_PACKAGE' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'UpdateDownloadService.downloadIntent' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'ACTION_INSTALL_UPDATE' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'showUpdateNotificationPermissionRationale' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'downloadApk' "$root/app/src/main/java/com/ytet/android/update/UpdateDownloadService.java"
grep -q 'FOREGROUND_SERVICE_TYPE_DATA_SYNC' "$root/app/src/main/java/com/ytet/android/update/UpdateDownloadService.java"
grep -q '업데이트 설치 준비 완료' "$root/app/src/main/java/com/ytet/android/update/UpdateDownloadService.java"
grep -q 'NOTIFICATION_ID = 4213' "$root/app/src/main/java/com/ytet/android/update/UpdateDownloadService.java"
grep -q 'START_REDELIVER_INTENT' "$root/app/src/main/java/com/ytet/android/update/UpdateDownloadService.java"
grep -q 'HttpURLConnection.HTTP_PARTIAL' "$root/app/src/main/java/com/ytet/android/update/UpdateDownloadService.java"
grep -q 'downloadedBytes != totalBytes' "$root/app/src/main/java/com/ytet/android/update/UpdateDownloadService.java"
grep -q '\.commit()' "$root/app/src/main/java/com/ytet/android/update/UpdateDownloadService.java"
grep -q 'AUTHORITY_SUFFIX = ".updateapk"' "$root/app/src/main/java/com/ytet/android/update/UpdateApkProvider.java"
grep -q 'ParcelFileDescriptor.open' "$root/app/src/main/java/com/ytet/android/update/UpdateApkProvider.java"
grep -q 'ACTION_MANAGE_UNKNOWN_APP_SOURCES' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'showExtractionNotificationPermissionRationale' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q '추출은 앱을 나가도 백그라운드에서 계속 진행됩니다' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q '전체 플레이리스트 추출' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q '실제 제목/아티스트 보정' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'resetOutputButton' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'weightedControlParams(8, 8)' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'weightedControlParams(2, 0)' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'showExpandedPlayer' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'showQueueDialog' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'SleepTimerDialView' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'DragDismissLayout' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'librarySearchToolbar' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'libraryFilterBar' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'LibraryFilter' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'LibraryGroup' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'libraryGridView ? R.drawable.ic_view_list : R.drawable.ic_grid_view' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'librarySearchInputRow' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'trackCard' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'showTrackDetails' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'showLibrarySourceDialog' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'handleLibraryPullToRefresh' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'PullRefreshScrollView' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'toolbarIconButton' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'LIBRARY_SOURCE_DEVICE' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'DefaultMediaPaths.displayPath' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'startHomeRefresh' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'recommendedStations(homeTracks)' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'controlParams(58, 18)' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'recommendedStations' "$root/app/src/main/java/com/ytet/android/stream/StationCatalog.java"
grep -q 'isLongSinglePlaylistTrack' "$root/app/src/main/java/com/ytet/android/stream/StationCatalog.java"
grep -q 'tracksForStation' "$root/app/src/main/java/com/ytet/android/library/MusicLibrary.java"
grep -q 'loadTracksByIds' "$root/app/src/main/java/com/ytet/android/library/DeviceMusicLibrary.java"
grep -q 'relativePathPrefixes' "$root/app/src/main/java/com/ytet/android/library/DeviceMusicLibrary.java"
grep -q 'ALBUM_ID' "$root/app/src/main/java/com/ytet/android/library/DeviceMusicLibrary.java"
grep -q 'Python.start' "$root/app/src/main/java/com/ytet/android/extract/YtDlpPythonEngine.java"
grep -q 'MediaTrackMuxer.mergeWorkspace' "$root/app/src/main/java/com/ytet/android/extract/YtDlpPythonEngine.java"
grep -q 'workspace,' "$root/app/src/main/java/com/ytet/android/extract/YtDlpPythonEngine.java"
grep -q 'FFmpegKit.executeWithArguments' "$root/app/src/main/java/com/ytet/android/extract/MediaTrackMuxer.java"
grep -q 'findSubtitleFiles' "$root/app/src/main/java/com/ytet/android/extract/MediaTrackMuxer.java"
grep -q 'mov_text' "$root/app/src/main/java/com/ytet/android/extract/MediaTrackMuxer.java"
grep -q 'STOP_FOREGROUND_DETACH' "$root/app/src/main/java/com/ytet/android/extract/ExtractionService.java"
grep -q '실패' "$root/app/src/main/java/com/ytet/android/extract/ExtractionService.java"
grep -q 'YoutubeDL' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'video_track_plan' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'playlist_index' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'include_playlist' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'enhance_metadata' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'MusicBrainzClient' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'MUSICBRAINZ_MIN_INTERVAL' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'mark_single_video_playlist' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'MusicBrainz 보정 건너뜀' "$root/app/src/main/python/ytet_ydl.py"
grep -q '"\\xa9alb"' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'best_mux_plan' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'MP4Cover' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'DocumentsContract.Document.MIME_TYPE_DIR' "$root/app/src/main/java/com/ytet/android/extract/StorageWriter.java"
grep -q 'copyToDefaultPublicFolder' "$root/app/src/main/java/com/ytet/android/extract/StorageWriter.java"
grep -q 'APP_FOLDER = "RabbYT"' "$root/app/src/main/java/com/ytet/android/core/DefaultMediaPaths.java"
grep -q 'LEGACY_APP_FOLDER = "YTET"' "$root/app/src/main/java/com/ytet/android/core/DefaultMediaPaths.java"
grep -q 'migrateLegacyDefaultFolders' "$root/app/src/main/java/com/ytet/android/extract/StorageWriter.java"
grep -q 'musicRelativePaths()' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'MUSIC_FOLDER = "Music"' "$root/app/src/main/java/com/ytet/android/core/DefaultMediaPaths.java"
grep -q 'doesNotFallbackToArbitraryFiles' "$root/app/src/test/java/com/ytet/android/extract/ExtractionOutputsTest.java"
grep -q 'embedsSubtitlesIntoMp4AsMovText' "$root/app/src/test/java/com/ytet/android/extract/MediaTrackMuxerTest.java"
grep -q 'guessesAndroidFriendlyMimeTypesForKnownOutputs' "$root/app/src/test/java/com/ytet/android/extract/StorageWriterTest.java"
grep -q 'yt_dlp-2026\\.3\\.17\\.dist-info' "$root/scripts/verify_apk_runtime.sh"
grep -q 'assembleStableRelease' "$root/.github/workflows/release.yml"
grep -q 'assembleNightlyRelease' "$root/.github/workflows/release.yml"
grep -q 'RabbYT-Beta.apk' "$root/.github/workflows/release.yml"
grep -q 'beta.\$GITHUB_RUN_NUMBER' "$root/.github/workflows/release.yml"
grep -q 'apksigner" verify' "$root/.github/workflows/release.yml"
grep -q 'app-stable-release.apk' "$root/.github/workflows/release.yml"
grep -q 'app-nightly-release.apk' "$root/.github/workflows/release.yml"
grep -q 'Release version must be nightly or a formal tag like v1.2.3' "$root/.github/workflows/release.yml"
grep -q 'skipping nightly release publishing' "$root/.github/workflows/release.yml"
grep -q -- '--prerelease' "$root/.github/workflows/release.yml"
grep -q 'app-stable-release-unsigned.apk' "$root/.github/workflows/ci.yml"
grep -q 'app-nightly-release-unsigned.apk' "$root/.github/workflows/ci.yml"

if grep -R "io.github.junkfood02.youtubedl-android\\|com.yausername.youtubedl_android\\|YoutubeDlAndroidEngine" "$root/app/src" "$root/app/build.gradle" "$root/build.gradle" "$root/settings.gradle" >/dev/null; then
  echo "unexpected youtubedl-android dependency or engine reference found" >&2
  exit 1
fi

if grep -R "filtered.addAll(allFiles)" "$root/app/src/main/java" >/dev/null; then
  echo "unsafe output-file fallback found" >&2
  exit 1
fi

if grep -R "app/src/main/assets/runtime/<ABI>" "$root/app/src/main/java" "$root/README.md" "$root/docs" >/dev/null; then
  echo "stale executable asset runtime instructions found" >&2
  exit 1
fi

if grep -R "somafm\\|StreamUrlResolver\\|cleartextTrafficPermitted\\|android:networkSecurityConfig\\|station\\.m3u\\|직접 스트리밍 URL\\|인터넷 라디오" "$root/app/src" "$root/README.md" >/dev/null; then
  echo "external radio or playlist streaming reference found" >&2
  exit 1
fi

if grep -R "buildSettingsTab\\|PREF_HIDE_SHORT_AUDIO\\|PREF_HIDE_SYSTEM_AUDIO_FOLDERS\\|Tab\\.SETTINGS" "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java" >/dev/null; then
  echo "removed settings tab or audio hiding filter reference found" >&2
  exit 1
fi

if grep -R "sectionTitle(\"폴더\")\\|sectionTitle(\"파일\")\\|selectedFolder" "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java" >/dev/null; then
  echo "removed folder/file library section reference found" >&2
  exit 1
fi

if grep -R "EXTRA_TRACK_TITLES\\|EXTRA_TRACK_ARTISTS\\|EXTRA_TRACK_ALBUMS\\|EXTRA_TRACK_URIS" "$root/app/src/main/java/com/ytet/android/playback" >/dev/null; then
  echo "oversized playback queue intent metadata found" >&2
  exit 1
fi

if grep -R "DownloadManager" "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java" >/dev/null; then
  echo "legacy DownloadManager update install path found" >&2
  exit 1
fi

echo "Static Android project checks passed."
