# Windows to Android port notes

## What moved

- Tkinter desktop UI was replaced by a native Android `Activity`.
- Blocking Python thread work was replaced by a foreground `Service`.
- Windows folder paths were replaced by Android Storage Access Framework tree URIs.
- PyInstaller and PowerShell release files are retained only in `legacy/windows-python`.
- Extraction options mirror the source app: audio/video mode, `m4a`, `mp3`, original audio, best/1080p/720p/480p video, optional subtitles, and optional multi-audio.

## Runtime boundary

The original app uses Python packages (`yt-dlp`, `mutagen`, `imageio-ffmpeg`) and a bundled Windows runtime. Android cannot run that Windows executable.

The active Android engine is `YtDlpProcessEngine`. It is implemented in this project and runs Android-compatible `yt-dlp` and `ffmpeg` executables from `app/src/main/assets/runtime/<ABI>/`. This avoids the `io.github.junkfood02.youtubedl-android` Gradle dependency and its app-wide GPL coupling, but it means runtime packaging is now explicit.

## Current limitations

- Audio metadata embedding is delegated to `yt-dlp` post-processing. The Python `mutagen` tagging layer is not copied into Android.
- Extraction requires Android-compatible `yt-dlp` and `ffmpeg` runtime assets. The current repository includes placeholders and the direct execution engine, not those binaries.
- Runtime binaries may still impose their own license obligations depending on what is bundled.
- Build verification was performed with a temporary Android SDK under `/tmp/android-sdk-ytet`.
