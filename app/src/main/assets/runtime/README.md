# Runtime binaries

Place Android-compatible executable runtime files here before making a release build.

Expected layout:

```text
runtime/
  arm64-v8a/
    yt-dlp
    ffmpeg
  armeabi-v7a/
    yt-dlp
    ffmpeg
  x86_64/
    yt-dlp
    ffmpeg
```

`yt-dlp` can be a self-contained native executable or a shell wrapper that runs an
embedded Python/yt-dlp runtime. The app copies these files into internal storage,
marks them executable, and runs them from a foreground service through
`YtDlpProcessEngine`.

This folder is intentionally empty except for this README. Add only binaries whose
licenses you are willing to satisfy for the way the APK will be distributed.
