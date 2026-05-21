#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
apk="${1:-$root/app/build/outputs/apk/debug/app-debug.apk}"

if [[ ! -f "$apk" ]]; then
  echo "missing APK: $apk" >&2
  exit 1
fi

listing="$(jar --list --file "$apk")"

require_entry() {
  local pattern="$1"
  local message="$2"
  if ! grep -Eq "$pattern" <<<"$listing"; then
    echo "$message" >&2
    exit 1
  fi
}

reject_entry() {
  local pattern="$1"
  local message="$2"
  if grep -Eq "$pattern" <<<"$listing"; then
    echo "$message" >&2
    exit 1
  fi
}

require_entry '^assets/chaquopy/app\.imy$' "missing packaged app Python module archive"
require_entry '^assets/chaquopy/requirements-arm64-v8a\.imy$' "missing arm64 yt-dlp requirements archive"
require_entry '^lib/arm64-v8a/libpython3\.12\.so$' "missing arm64 embedded Python runtime"
require_entry '^lib/arm64-v8a/libffmpegkit\.so$' "missing arm64 FFmpegKit native runtime"
require_entry '^lib/arm64-v8a/libavformat\.so$' "missing arm64 FFmpeg demux/mux library"
require_entry '^lib/arm64-v8a/libavcodec\.so$' "missing arm64 FFmpeg codec library"
reject_entry '^lib/x86_64/' "x86_64 native libraries should not be packaged without matching FFmpegKit runtime"
reject_entry '^assets/runtime/' "stale executable asset runtime packaged into APK"

if ! strings "$apk" | grep -q 'ytet_ydl\.pyc'; then
  echo "missing compiled ytet_ydl Python module in APK" >&2
  exit 1
fi

if ! strings "$apk" | grep -q 'yt_dlp-2026\.3\.17\.dist-info'; then
  echo "missing packaged yt-dlp 2026.3.17 runtime in APK" >&2
  exit 1
fi

if ! strings "$apk" | grep -q 'yt_dlp_ejs-0\.8\.0\.dist-info'; then
  echo "missing packaged yt-dlp-ejs 0.8.0 scripts in APK" >&2
  exit 1
fi

if ! strings "$apk" | grep -q 'mutagen-1\.47\.0\.dist-info'; then
  echo "missing packaged mutagen 1.47.0 tag writer in APK" >&2
  exit 1
fi

echo "APK runtime packaging checks passed."
