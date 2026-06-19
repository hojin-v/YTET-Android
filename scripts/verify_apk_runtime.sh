#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
apk="${1:-$root/app/build/outputs/apk/release/app-release-unsigned.apk}"

if [[ ! -f "$apk" ]]; then
  echo "missing APK: $apk" >&2
  exit 1
fi
apk="$(cd "$(dirname "$apk")" && pwd)/$(basename "$apk")"

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
require_entry '^assets/chaquopy/requirements-common\.imy$' "missing common yt-dlp requirements archive"
require_entry '^lib/arm64-v8a/libpython3\.12\.so$' "missing arm64 embedded Python runtime"
require_entry '^lib/arm64-v8a/libffmpegkit\.so$' "missing arm64 FFmpegKit native runtime"
require_entry '^lib/arm64-v8a/libavformat\.so$' "missing arm64 FFmpeg demux/mux library"
require_entry '^lib/arm64-v8a/libavcodec\.so$' "missing arm64 FFmpeg codec library"
reject_entry '^lib/x86_64/' "x86_64 native libraries should not be packaged without matching FFmpegKit runtime"
reject_entry '^assets/runtime/' "stale executable asset runtime packaged into APK"

require_archive_string() {
  local entry="$1"
  local pattern="$2"
  local message="$3"
  local tmpdir=""
  if command -v unzip >/dev/null 2>&1; then
    if ! unzip -p "$apk" "$entry" | strings | grep "$pattern" >/dev/null; then
      echo "$message" >&2
      exit 1
    fi
    return
  fi

  tmpdir="$(mktemp -d)"
  trap '[[ -n "${tmpdir:-}" ]] && rm -rf "$tmpdir"' RETURN
  (cd "$tmpdir" && jar --extract --file "$apk" "$entry")
  if ! strings "$tmpdir/$entry" | grep "$pattern" >/dev/null; then
    echo "$message" >&2
    exit 1
  fi
  rm -rf "$tmpdir"
  tmpdir=""
  trap - RETURN
}

require_archive_string 'assets/chaquopy/app.imy' 'ytet_ydl\.pyc' \
  "missing compiled ytet_ydl Python module in APK"
require_archive_string 'assets/chaquopy/requirements-common.imy' 'yt_dlp-2026\.3\.17\.dist-info' \
  "missing packaged yt-dlp 2026.3.17 runtime in APK"
require_archive_string 'assets/chaquopy/requirements-common.imy' 'yt_dlp_ejs-0\.8\.0\.dist-info' \
  "missing packaged yt-dlp-ejs 0.8.0 scripts in APK"
require_archive_string 'assets/chaquopy/requirements-common.imy' 'mutagen-1\.47\.0\.dist-info' \
  "missing packaged mutagen 1.47.0 tag writer in APK"

echo "APK runtime packaging checks passed."
