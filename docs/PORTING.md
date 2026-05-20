# Windows to Android port notes

## What moved

- Tkinter desktop UI was replaced by a native Android `Activity`.
- Blocking Python thread work was replaced by a foreground `Service`.
- Windows folder paths were replaced by Android Storage Access Framework tree URIs.
- PyInstaller and PowerShell release files are retained only in `legacy/windows-python`.
- Extraction options mirror the source app: audio/video mode, `m4a`, `mp3`, original audio, best/1080p/720p/480p video, optional subtitles, and optional multi-audio.

## Runtime boundary

The original app uses Python packages (`yt-dlp`, `mutagen`, `imageio-ffmpeg`) and a bundled Windows runtime. Android cannot run that Windows executable.

The active Android engine is `YtDlpPythonEngine`. It is implemented in this project and runs the `yt-dlp` Python package through an embedded Chaquopy Python runtime. This avoids the `io.github.junkfood02.youtubedl-android` Gradle dependency and the previous executable-asset placeholder.

## Current limitations

- Audio metadata embedding and MP3 conversion are not enabled in the current APK because no FFmpeg runtime is bundled.
- Video downloads use single-file formats only. FFmpeg-based stream merging, subtitle embedding, and multi-audio muxing are not enabled yet.
- Registered subtitles can be saved as sidecar files when YouTube exposes them directly.
- Embedded runtime packages may still impose their own license obligations depending on what is bundled.
- Build verification was performed with a temporary Android SDK under `/tmp/android-sdk-ytet`.
