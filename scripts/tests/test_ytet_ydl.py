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

    def build(self, media_type="audio", option="m4a", subtitles=False):
        return ytet_ydl.build_options(
            "/tmp/workspace",
            media_type,
            option,
            subtitles,
            self.listener,
            self.logger,
        )

    def test_audio_m4a_uses_direct_audio_stream_without_ffmpeg_postprocessing(self):
        options = self.build(option="m4a")

        self.assertEqual("ba[ext=m4a]/ba/best", options["format"])
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

    def test_video_plan_selects_separate_high_quality_tracks(self):
        plan = ytet_ydl.video_track_plan(fake_video_info(), "best")

        self.assertEqual("mux", plan["mode"])
        self.assertEqual("mkv", plan["container"])
        self.assertEqual("401", plan["video"]["format_id"])
        self.assertEqual("251", plan["audio"]["format_id"])

    def test_video_height_preset_prefers_compatible_avc_track(self):
        plan = ytet_ydl.video_track_plan(fake_video_info(), "720p")

        self.assertEqual("mux", plan["mode"])
        self.assertEqual("mp4", plan["container"])
        self.assertEqual("136", plan["video"]["format_id"])
        self.assertEqual("140", plan["audio"]["format_id"])

    def test_video_height_preset_falls_back_to_mkv_without_mp4_tracks(self):
        plan = ytet_ydl.video_track_plan(fake_webm_only_info(), "1080p")

        self.assertEqual("mux", plan["mode"])
        self.assertEqual("mkv", plan["container"])
        self.assertEqual("248", plan["video"]["format_id"])
        self.assertEqual("251", plan["audio"]["format_id"])

    def test_video_subtitles_are_sidecar_downloads_not_embedded_muxing(self):
        self.assertEqual(["ko", "ko-KR", "en", "en-US", "en-GB"], ytet_ydl.SUBTITLE_LANGUAGES)

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

    def test_filename_sanitizer_removes_android_hostile_characters(self):
        self.assertEqual("A B C D", ytet_ydl.sanitize_filename('A/B:C*D?'))

    def test_supported_cover_images_are_limited_to_jpeg_and_png(self):
        self.assertEqual("image/jpeg", ytet_ydl.supported_image_mime(b"\xff\xd8\xffdata", "image/webp"))
        self.assertEqual("image/png", ytet_ydl.supported_image_mime(b"\x89PNG\r\n\x1a\n", "image/webp"))
        self.assertIsNone(ytet_ydl.supported_image_mime(b"RIFFwebp", "image/webp"))


def fake_video_info():
    return {
        "formats": [
            {"format_id": "18", "ext": "mp4", "height": 360, "width": 640, "vcodec": "avc1.42001E", "acodec": "mp4a.40.2", "tbr": 444},
            {"format_id": "136", "ext": "mp4", "height": 720, "width": 1280, "fps": 30, "vcodec": "avc1.4d401f", "acodec": "none", "tbr": 993},
            {"format_id": "399", "ext": "mp4", "height": 1080, "width": 1920, "fps": 30, "vcodec": "av01.0.08M.08", "acodec": "none", "tbr": 1142},
            {"format_id": "401", "ext": "mp4", "height": 2160, "width": 3840, "fps": 30, "vcodec": "av01.0.12M.08", "acodec": "none", "tbr": 9025},
            {"format_id": "140", "ext": "m4a", "vcodec": "none", "acodec": "mp4a.40.2", "abr": 130, "tbr": 130},
            {"format_id": "251", "ext": "webm", "vcodec": "none", "acodec": "opus", "abr": 160, "tbr": 160},
            {"format_id": "249", "ext": "webm", "vcodec": "none", "acodec": "opus", "abr": 46, "tbr": 46},
        ]
    }


def fake_webm_only_info():
    return {
        "formats": [
            {"format_id": "248", "ext": "webm", "height": 1080, "width": 1920, "fps": 30, "vcodec": "vp9", "acodec": "none", "tbr": 2200},
            {"format_id": "251", "ext": "webm", "vcodec": "none", "acodec": "opus", "abr": 160, "tbr": 160},
        ]
    }


if __name__ == "__main__":
    unittest.main()
