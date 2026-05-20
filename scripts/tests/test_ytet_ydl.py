#!/usr/bin/env python3
import importlib
import sys
import types
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class DownloadError(Exception):
    pass


class FakeYoutubeDL:
    def __init__(self, options):
        self.options = options

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback):
        return False

    def download(self, urls):
        return 0


def install_yt_dlp_stub():
    yt_dlp = types.ModuleType("yt_dlp")
    yt_dlp.YoutubeDL = FakeYoutubeDL
    utils = types.ModuleType("yt_dlp.utils")
    utils.DownloadError = DownloadError
    sys.modules["yt_dlp"] = yt_dlp
    sys.modules["yt_dlp.utils"] = utils


install_yt_dlp_stub()
sys.path.insert(0, str(ROOT / "app" / "src" / "main" / "python"))
ytet_ydl = importlib.import_module("ytet_ydl")


class FakeProgressListener:
    def __init__(self):
        self.events = []

    def onProgress(self, percent, stage, message):
        self.events.append((percent, stage, message))


class YtetYdlOptionsTest(unittest.TestCase):
    def setUp(self):
        self.listener = FakeProgressListener()
        self.logger = ytet_ydl.YtetLogger()

    def build(self, media_type="audio", option="m4a", subtitles=False, multi_audio=False):
        return ytet_ydl.build_options(
            "/tmp/workspace",
            media_type,
            option,
            subtitles,
            multi_audio,
            self.listener,
            self.logger,
        )

    def test_audio_m4a_uses_direct_audio_stream_without_ffmpeg_postprocessing(self):
        options = self.build(option="m4a")

        self.assertEqual("ba[ext=m4a]", options["format"])
        self.assertEqual("/tmp/workspace/%(uploader)s - %(title)s.%(ext)s", options["outtmpl"])
        self.assertNotIn("postprocessors", options)
        self.assertNotIn("ffmpeg_location", options)
        self.assertEqual(1, len(options["progress_hooks"]))

    def test_audio_original_uses_best_audio_without_conversion(self):
        options = self.build(option="original")

        self.assertEqual("ba/best", options["format"])
        self.assertNotIn("postprocessors", options)

    def test_mp3_is_rejected_until_ffmpeg_runtime_exists(self):
        with self.assertRaisesRegex(ytet_ydl.YtetExtractionError, "MP3 변환"):
            self.build(option="mp3")

    def test_video_quality_selects_single_file_formats(self):
        self.assertEqual("b[ext=mp4]/best", self.build(media_type="video", option="best")["format"])
        self.assertEqual("b[height<=1080][ext=mp4]", self.build(media_type="video", option="1080")["format"])
        self.assertEqual("b[height<=720][ext=mp4]", self.build(media_type="video", option="720p")["format"])
        self.assertEqual("b[height<=480][ext=mp4]", self.build(media_type="video", option="480")["format"])

    def test_video_subtitles_are_sidecar_downloads_not_embedded_muxing(self):
        options = self.build(media_type="video", option="720", subtitles=True)

        self.assertTrue(options["writesubtitles"])
        self.assertEqual(["ko", "ko-KR", "en", "en-US", "en-GB"], options["subtitleslangs"])
        self.assertEqual("srt/vtt/best", options["subtitlesformat"])
        self.assertNotIn("embedsubtitles", options)
        self.assertNotIn("postprocessors", options)

    def test_multi_audio_is_rejected_until_ffmpeg_runtime_exists(self):
        with self.assertRaisesRegex(ytet_ydl.YtetExtractionError, "다중 오디오"):
            self.build(media_type="video", multi_audio=True)

    def test_progress_hook_reports_bounded_download_progress(self):
        hook = ytet_ydl.progress_hook(self.listener)

        hook({
            "status": "downloading",
            "filename": "/tmp/workspace/file.m4a",
            "downloaded_bytes": 512,
            "total_bytes": 1024,
        })
        hook({"status": "finished", "filename": "/tmp/workspace/file.m4a"})

        self.assertEqual(48, self.listener.events[0][0])
        self.assertEqual("다운로드", self.listener.events[0][1])
        self.assertIn("file.m4a", self.listener.events[0][2])
        self.assertEqual((88, "정리", "file.m4a"), self.listener.events[1])

    def test_clean_error_strips_ansi_and_includes_logger_tail(self):
        self.logger.warning("\x1b[31mfirst warning\x1b[0m")

        message = ytet_ydl.clean_error("\x1b[31mDownload failed\x1b[0m", self.logger)

        self.assertIn("Download failed", message)
        self.assertIn("first warning", message)
        self.assertNotIn("\x1b[", message)


if __name__ == "__main__":
    unittest.main()
