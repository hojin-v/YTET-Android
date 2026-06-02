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
  "$root/app/src/main/java/com/ytet/android/stream/StationCatalog.java"
  "$root/app/src/main/java/com/ytet/android/library/MusicLibrary.java"
  "$root/app/src/main/java/com/ytet/android/extract/YtDlpPythonEngine.java"
  "$root/app/src/main/java/com/ytet/android/extract/MediaTrackMuxer.java"
  "$root/app/src/main/java/com/ytet/android/extract/ExtractionOutputs.java"
  "$root/app/src/main/res/drawable/ic_stat_playback.xml"
  "$root/app/src/main/python/ytet_ydl.py"
  "$root/app/src/test/java/com/ytet/android/core/YoutubeUrlValidatorTest.java"
  "$root/app/src/test/java/com/ytet/android/core/ExtractionRequestTest.java"
  "$root/app/src/test/java/com/ytet/android/core/FormatMappingTest.java"
  "$root/app/src/test/java/com/ytet/android/extract/ExtractionOutputsTest.java"
  "$root/app/src/test/java/com/ytet/android/extract/MediaTrackMuxerTest.java"
  "$root/app/src/test/java/com/ytet/android/extract/StorageWriterTest.java"
  "$root/app/src/test/java/com/ytet/android/stream/StationCatalogTest.java"
  "$root/app/src/test/java/com/ytet/android/library/MusicLibraryTest.java"
  "$root/scripts/test_python_engine.sh"
  "$root/scripts/verify_apk_runtime.sh"
  "$root/app/src/main/java/com/ytet/android/core/YoutubeUrlValidator.java"
  "$root/legacy/windows-python/src/youtube_audio_extractor/extractor.py"
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
grep -q 'testImplementation "junit:junit:4.13.2"' "$root/app/build.gradle"
grep -q 'com.mrljdx:ffmpeg-kit-full:6.1.4' "$root/app/build.gradle"
grep -q 'yt-dlp==2026.3.17' "$root/app/build.gradle"
grep -q 'yt-dlp-ejs==0.8.0' "$root/app/build.gradle"
grep -q 'mutagen==1.47.0' "$root/app/build.gradle"
grep -q 'android.permission.FOREGROUND_SERVICE' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android:allowBackup="false"' "$root/app/src/main/AndroidManifest.xml"
grep -q 'android:foregroundServiceType="mediaPlayback"' "$root/app/src/main/AndroidManifest.xml"
grep -q 'ACTION_OPEN_DOCUMENT_TREE' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'new YtDlpPythonEngine()' "$root/app/src/main/java/com/ytet/android/extract/ExtractionService.java"
grep -q 'MediaSession' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'Notification.MediaStyle' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK' "$root/app/src/main/java/com/ytet/android/playback/PlaybackService.java"
grep -q 'recommendedStations' "$root/app/src/main/java/com/ytet/android/stream/StationCatalog.java"
grep -q 'tracksForStation' "$root/app/src/main/java/com/ytet/android/library/MusicLibrary.java"
grep -q 'Python.start' "$root/app/src/main/java/com/ytet/android/extract/YtDlpPythonEngine.java"
grep -q 'MediaTrackMuxer.mergeWorkspace' "$root/app/src/main/java/com/ytet/android/extract/YtDlpPythonEngine.java"
grep -q 'FFmpegKit.executeWithArguments' "$root/app/src/main/java/com/ytet/android/extract/MediaTrackMuxer.java"
grep -q 'findSubtitleFiles' "$root/app/src/main/java/com/ytet/android/extract/MediaTrackMuxer.java"
grep -q 'mov_text' "$root/app/src/main/java/com/ytet/android/extract/MediaTrackMuxer.java"
grep -q 'YoutubeDL' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'video_track_plan' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'best_mux_plan' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'MP4Cover' "$root/app/src/main/python/ytet_ydl.py"
grep -q 'doesNotFallbackToArbitraryFiles' "$root/app/src/test/java/com/ytet/android/extract/ExtractionOutputsTest.java"
grep -q 'embedsSubtitlesIntoMp4AsMovText' "$root/app/src/test/java/com/ytet/android/extract/MediaTrackMuxerTest.java"
grep -q 'guessesAndroidFriendlyMimeTypesForKnownOutputs' "$root/app/src/test/java/com/ytet/android/extract/StorageWriterTest.java"
grep -q 'yt_dlp-2026\\.3\\.17\\.dist-info' "$root/scripts/verify_apk_runtime.sh"

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

echo "Static Android project checks passed."
