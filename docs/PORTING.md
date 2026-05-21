# Windows to Android port notes

## What moved

- Tkinter desktop UI was replaced by a native Android `Activity`.
- Blocking Python thread work was replaced by a foreground `Service`.
- Windows folder paths were replaced by Android Storage Access Framework tree URIs.
- PyInstaller and PowerShell release files are retained only in `legacy/windows-python`.
- Extraction options mirror the source app where they are useful on mobile: audio/video mode, `m4a`, `mp3`, original audio, best/1080p/720p/480p video, and optional subtitles.

## Runtime boundary

The original app uses Python packages (`yt-dlp`, `mutagen`, `imageio-ffmpeg`) and a bundled Windows runtime. Android cannot run that Windows executable.

The active Android engine is `YtDlpPythonEngine`. It is implemented in this project and runs the `yt-dlp` Python package through an embedded Chaquopy Python runtime. This avoids the `io.github.junkfood02.youtubedl-android` Gradle dependency and the previous executable-asset placeholder.

High-quality video is no longer limited to yt-dlp single-file formats. The Android engine downloads separate video-only and audio-only streams and writes a mux manifest. `MediaTrackMuxer` then uses the Android NDK FFmpegKit runtime to remux the tracks.

For the `best` option, Android follows the Windows direction: pick the best video-only stream and the best audio-only stream, then remux to `MKV`. For the capped options, Android prefers AVC MP4 video plus M4A audio so phone players handle the result well; if that combination is unavailable, it falls back to capped `MKV` rather than lowering quality to a progressive file.

M4A cover images are embedded with `mutagen` when YouTube exposes a JPEG or PNG thumbnail.

## Runtime packaging

- `yt-dlp` runs as a Chaquopy Python package, not as an executable under `assets/runtime`.
- `FFmpegKit` is packaged as native Android libraries and currently limits release APKs to `arm64-v8a`.
- `yt-dlp-ejs` scripts are packaged, but an Android JS runtime executable such as Deno, Node, or QuickJS is not bundled yet.
- Because no JS runtime is bundled, yt-dlp can still warn that some YouTube formats may be missing on videos that require player JS evaluation.

## Current limitations

- MP3 conversion is not enabled in the Android UI yet.
- Original Opus cover embedding is not enabled yet.
- Subtitle embedding is not enabled yet; registered subtitles can be saved as sidecar files.
- Embedded runtime packages may still impose their own license obligations depending on what is bundled.
- Build verification was performed with a temporary Android SDK under `/tmp/android-sdk-ytet`.
