#!/usr/bin/env python3
import importlib
import json
import random
import sys
import tempfile
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


class FakeMusicBrainzClient:
    def search(self, entity, query, limit=5):
        if entity == "release":
            return {"releases": []}
        if entity == "recording":
            return {"recordings": []}
        return {}

    def lookup_release(self, release_id):
        return {}


class YtetYdlOptionsTest(unittest.TestCase):
    def setUp(self):
        self.listener = FakeProgressListener()
        self.logger = ytet_ydl.YtetLogger()

    def build(self, media_type="audio", option="m4a", subtitles=False, playlist=False):
        return ytet_ydl.build_options(
            "/tmp/workspace",
            media_type,
            option,
            subtitles,
            self.listener,
            self.logger,
            include_playlist=playlist,
        )

    def test_audio_m4a_uses_direct_audio_stream_without_ffmpeg_postprocessing(self):
        options = self.build(option="m4a")

        self.assertEqual("ba[ext=m4a]/ba/best", options["format"])
        self.assertEqual("/tmp/workspace/%(uploader)s - %(title)s.%(ext)s", options["outtmpl"])
        self.assertNotIn("postprocessors", options)
        self.assertNotIn("ffmpeg_location", options)
        self.assertEqual(1, len(options["progress_hooks"]))

    def test_audio_playlist_keeps_playlist_order_in_output_names(self):
        options = self.build(option="m4a", playlist=True)

        self.assertFalse(options["noplaylist"])
        self.assertEqual(
            "/tmp/workspace/%(playlist)s/%(playlist_index)03d - %(uploader)s - %(title)s.%(ext)s",
            options["outtmpl"],
        )

    def test_audio_original_uses_best_audio_without_conversion(self):
        options = self.build(option="original")

        self.assertEqual("ba/best", options["format"])
        self.assertNotIn("postprocessors", options)

    def test_mp3_is_rejected_until_ffmpeg_runtime_exists(self):
        with self.assertRaisesRegex(ytet_ydl.YtetExtractionError, "MP3 변환"):
            self.build(option="mp3")

    def test_video_playlist_is_rejected_until_batch_muxing_exists(self):
        with tempfile.TemporaryDirectory() as workspace:
            with self.assertRaisesRegex(ytet_ydl.YtetExtractionError, "영상 플레이리스트"):
                ytet_ydl.extract(workspace, "https://youtu.be/video", "video", "best", False, True, self.listener)

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

    def test_video_subtitle_language_priority_is_limited_to_registered_ko_and_en(self):
        self.assertEqual(["ko", "ko-KR", "en", "en-US", "en-GB"], ytet_ydl.SUBTITLE_LANGUAGES)

    def test_single_video_manifest_remuxes_subtitled_webm_to_mkv(self):
        with tempfile.TemporaryDirectory() as workspace:
            Path(workspace, "video-track.webm").write_text("video")
            Path(workspace, "sample.ko.srt").write_text("subtitle")

            ytet_ydl.write_single_video_manifest(workspace, {"title": "A/B"})

            manifest = json.loads(Path(workspace, "mux.json").read_text())

        self.assertEqual("video-track.webm", manifest["video"])
        self.assertEqual("A B.mkv", manifest["output"])

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

    def test_progress_hook_reports_playlist_position_when_available(self):
        hook = ytet_ydl.progress_hook(self.listener)

        hook({
            "status": "downloading",
            "filename": "/tmp/workspace/002 - file.m4a",
            "downloaded_bytes": 512,
            "total_bytes": 1024,
            "info_dict": {
                "playlist_index": 2,
                "n_entries": 6,
            },
        })
        hook({
            "status": "finished",
            "filename": "/tmp/workspace/002 - file.m4a",
            "info_dict": {
                "playlist_index": 2,
                "n_entries": 6,
            },
        })

        self.assertEqual(27, self.listener.events[0][0])
        self.assertEqual("다운로드", self.listener.events[0][1])
        self.assertIn("2/6", self.listener.events[0][2])
        self.assertEqual((34, "정리", "2/6 002 - file.m4a"), self.listener.events[1])

    def test_playlist_metadata_uses_playlist_as_album(self):
        info = {
            "title": "Song",
            "uploader": "Artist",
            "playlist": "Album Name",
            "playlist_index": 3,
            "n_entries": 6,
        }

        self.assertEqual("Song", ytet_ydl.metadata_title(info))
        self.assertEqual("Artist", ytet_ydl.metadata_artist(info))
        self.assertEqual("Album Name", ytet_ydl.metadata_album(info))
        self.assertEqual((3, 6), ytet_ydl.metadata_track_number(info))

    def test_playlist_album_can_fallback_to_parent_title(self):
        self.assertEqual("Parent Playlist", ytet_ydl.metadata_album({"title": "Song"}, "Parent Playlist"))

    def test_title_candidates_strip_upload_descriptors(self):
        self.assertEqual("LOVE SONG", ytet_ydl.title_candidates("LOVE SONG (Lyric Video/Eng)")[0])
        self.assertEqual("LOVE SONG", ytet_ydl.title_candidates("[Official Audio] LOVE SONG")[0])

    def test_playlist_title_splits_artist_and_album_candidates(self):
        info = {"title": "Bill Evans Trio / Waltz For Debby [Full Album]"}

        self.assertEqual(("Bill Evans Trio", "Waltz For Debby"), ytet_ydl.playlist_artist_album_candidates(info))

    def test_release_metadata_overrides_playlist_entries(self):
        entries = [
            {"title": "LOVE SONG (Lyric Video/Eng)", "duration": 180, "playlist_index": 1},
            {"title": "Next Song", "duration": 200, "playlist_index": 2},
        ]
        release = {
            "title": "Album Title",
            "date": "2024-01-01",
            "artist-credit": [{"name": "매미"}],
            "media": [{
                "track-count": 2,
                "tracks": [
                    {"title": "LOVE SONG", "position": 1, "length": 180000, "artist-credit": [{"name": "매미"}]},
                    {"title": "Next Song", "position": 2, "length": 200000, "artist-credit": [{"name": "매미"}]},
                ],
            }],
        }

        matched = ytet_ydl.apply_release_metadata(entries, release)

        self.assertEqual(2, matched)
        self.assertEqual("LOVE SONG", ytet_ydl.metadata_title(entries[0]))
        self.assertEqual("매미", ytet_ydl.metadata_artist(entries[0]))
        self.assertEqual("Album Title", ytet_ydl.metadata_album(entries[0]))
        self.assertEqual((1, 2), ytet_ydl.metadata_track_number(entries[0]))

    def test_playlist_musicbrainz_reports_entry_progress(self):
        entries = [{"title": "First"}, {"title": "Second"}]

        ytet_ydl.enhance_playlist_metadata(
            {"title": "Artist / Album"},
            entries,
            FakeMusicBrainzClient(),
            self.logger,
            self.listener,
        )

        messages = [event[2] for event in self.listener.events]
        self.assertIn("MusicBrainz 앨범 후보 검색 중", messages)
        self.assertIn("MusicBrainz 검색 중 1/2", messages)
        self.assertIn("MusicBrainz 검색 중 2/2", messages)
        self.assertEqual("MusicBrainz 검색 완료", messages[-1])

    def test_high_score_recording_result_allows_artist_alias_difference(self):
        info = {"title": "LOVE SONG (Lyric Video/Eng)", "artist": "매미"}
        results = [{
            "score": "100",
            "title": "LOVE SONG",
            "artist-credit": [{"name": "MEMI"}],
            "releases": [{
                "title": "LOVE SONG",
                "date": "2024-10-30",
                "artist-credit": [{"name": "MEMI"}],
                "media": [{"track-count": 1}],
            }],
        }]

        best, score = ytet_ydl.best_recording_result(info, results, ytet_ydl.title_candidates(info["title"]), "매미")

        self.assertEqual("LOVE SONG", best["title"])
        self.assertGreaterEqual(score, 0.80)

    def test_long_single_video_playlist_skips_musicbrainz_and_cleans_title(self):
        info = {
            "title": "[Playlist] 퇴근 후 나만의 시간 | 잔잔하게 틀어두기 좋은 재즈 BGM",
            "duration": 2807,
        }

        ytet_ydl.mark_single_video_playlist(info)

        self.assertTrue(ytet_ydl.is_single_video_playlist(info))
        self.assertEqual("퇴근 후 나만의 시간 | 잔잔하게 틀어두기 좋은 재즈 BGM", ytet_ydl.metadata_title(info))
        self.assertEqual("퇴근 후 나만의 시간 | 잔잔하게 틀어두기 좋은 재즈 BGM", ytet_ydl.metadata_album(info))

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


class YtetYdlStreamCatalogTest(unittest.TestCase):
    def test_popular_channel_urls_request_youtube_popular_sort(self):
        urls = ytet_ydl.stream_channel_candidate_urls("https://www.youtube.com/@leeplay.official", popular=True)

        self.assertIn("https://www.youtube.com/@leeplay.official/videos?view=0&sort=p&flow=grid", urls)
        self.assertIn("https://www.youtube.com/@leeplay.official/videos?sort=p", urls)

    def test_stream_channel_videos_marks_popular_rank_and_view_count(self):
        videos = ytet_ydl.stream_channel_videos({
            "entries": [{
                "id": "abc",
                "title": "Popular Video",
                "url": "abc",
                "view_count_text": "1.2만회",
                "upload_date": "20260619",
            }]
        }, {"title": "Channel"}, 10, "popular")

        self.assertEqual(1, len(videos))
        self.assertEqual(12_000, videos[0]["view_count"])
        self.assertEqual(1, videos[0]["popular_rank"])

    def test_merge_enriches_latest_entries_with_popular_metadata(self):
        latest = [{
            "id": "abc",
            "url": "https://www.youtube.com/watch?v=abc",
            "view_count": 0,
            "source_index": 0,
            "popular_rank": 0,
        }]
        popular = [{
            "id": "abc",
            "url": "https://www.youtube.com/watch?v=abc",
            "view_count": 5000,
            "source_index": 0,
            "popular_rank": 1,
        }]

        merged = ytet_ydl.merge_stream_channel_videos(latest, popular, 10)

        self.assertEqual(1, len(merged))
        self.assertEqual(5000, merged[0]["view_count"])
        self.assertEqual(1, merged[0]["popular_rank"])

    def test_varied_stream_video_selection_keeps_popular_and_non_latest_candidates(self):
        videos = []
        for index in range(12):
            videos.append({
                "id": f"id-{index}",
                "url": f"https://www.youtube.com/watch?v=id-{index}",
                "view_count": 50_000 - index * 1000,
                "published": 20260620 - index,
                "source_index": index,
                "popular_rank": index + 1 if index < 4 else 0,
            })

        selected = ytet_ydl.varied_stream_channel_videos(videos, 6, random.Random(7))
        selected_ids = {item["id"] for item in selected}

        self.assertEqual(6, len(selected))
        self.assertIn("id-0", selected_ids)
        self.assertTrue(any(int(item["id"].split("-")[1]) >= 6 for item in selected))

    def test_zero_view_popular_order_falls_back_to_latest_order(self):
        videos = [
            {
                "id": "old-popular",
                "url": "https://www.youtube.com/watch?v=old-popular",
                "view_count": 0,
                "published": 20250101,
                "source_index": 8,
                "popular_rank": 1,
            },
            {
                "id": "new-latest",
                "url": "https://www.youtube.com/watch?v=new-latest",
                "view_count": 0,
                "published": 20260620,
                "source_index": 0,
                "popular_rank": 9,
            },
        ]

        popular_order = [item["id"] for item in sorted(videos, key=ytet_ydl.stream_video_popular_sort_key)]
        latest_order = [item["id"] for item in sorted(videos, key=ytet_ydl.stream_video_latest_sort_key)]

        self.assertEqual(["new-latest", "old-popular"], popular_order)
        self.assertEqual(["new-latest", "old-popular"], latest_order)

    def test_stream_channel_limits_keep_display_small_and_expand_harvest(self):
        self.assertEqual(80, ytet_ydl.stream_channel_quick_harvest_limit(40))
        self.assertEqual(400, ytet_ydl.stream_channel_harvest_limit(40))
        self.assertEqual(40, ytet_ydl.stream_channel_display_limit(40))

    def test_enrich_stream_video_metadata_fills_flat_playlist_fields(self):
        class DetailYdl:
            def extract_info(self, url, download=False):
                return {
                    "id": "abc",
                    "duration": 180,
                    "upload_date": "20260619",
                    "view_count": 34567,
                    "thumbnail": "https://example.test/thumb.jpg",
                }

        videos = [{
            "id": "abc",
            "url": "https://www.youtube.com/watch?v=abc",
            "thumbnail": "",
            "duration": 0,
            "view_count": 0,
            "published": 0,
            "source_index": 0,
            "popular_rank": 0,
        }]

        enriched = ytet_ydl.enrich_stream_video_metadata(DetailYdl(), videos)

        self.assertEqual(34_567, enriched[0]["view_count"])
        self.assertEqual(20260619, enriched[0]["published"])
        self.assertEqual(180, enriched[0]["duration"])
        self.assertEqual("https://example.test/thumb.jpg", enriched[0]["thumbnail"])


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
