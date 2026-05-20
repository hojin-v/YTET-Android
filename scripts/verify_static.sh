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
  "$root/app/src/main/java/com/ytet/android/extract/YtDlpPythonEngine.java"
  "$root/app/src/main/java/com/ytet/android/extract/ExtractionOutputs.java"
  "$root/app/src/main/python/ytet_ydl.py"
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
grep -q 'yt-dlp==2026.3.17' "$root/app/build.gradle"
grep -q 'android.permission.FOREGROUND_SERVICE' "$root/app/src/main/AndroidManifest.xml"
grep -q 'ACTION_OPEN_DOCUMENT_TREE' "$root/app/src/main/java/com/ytet/android/ui/MainActivity.java"
grep -q 'new YtDlpPythonEngine()' "$root/app/src/main/java/com/ytet/android/extract/ExtractionService.java"
grep -q 'Python.start' "$root/app/src/main/java/com/ytet/android/extract/YtDlpPythonEngine.java"
grep -q 'YoutubeDL' "$root/app/src/main/python/ytet_ydl.py"

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

echo "Static Android project checks passed."
