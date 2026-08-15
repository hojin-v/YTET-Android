import os
import json
import random
import re
import time
import unicodedata
from difflib import SequenceMatcher
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from yt_dlp import YoutubeDL
from yt_dlp.utils import DownloadError


SUBTITLE_LANGUAGES = ["ko", "ko-KR", "en", "en-US", "en-GB"]
AUDIO_EXTENSIONS = {".m4a", ".aac", ".flac", ".mp3", ".opus", ".ogg", ".wav", ".webm"}
DEFAULT_ANDROID_SDK = 36
EXTRACTION_REPORT_NAME = "ytet-extraction-report.json"
MUSICBRAINZ_API_ROOT = "https://musicbrainz.org/ws/2"
MUSICBRAINZ_USER_AGENT = "RabbYT-Android/0.1 (https://github.com/hojin/youtube-audio-extractor-android)"
MUSICBRAINZ_MIN_INTERVAL = 1.05


class YtetExtractionError(Exception):
    pass


class YtetCancellationError(Exception):
    pass


class YtetLogger:
    def __init__(self):
        self.messages = []

    def debug(self, message):
        pass

    def info(self, message):
        pass

    def warning(self, message):
        self._append(message)

    def error(self, message):
        self._append(message)

    def _append(self, message):
        text = strip_ansi(str(message or "").strip())
        if text:
            self.messages.append(text)
            del self.messages[:-8]

    def tail(self):
        return "\n".join(self.messages)


def stream_channels(channels_json, per_channel=3):
    channels = json.loads(channels_json or "[]")
    per_channel = max(1, min(as_int(per_channel) or 3, 40))
    harvest_limit = stream_channel_quick_harvest_limit(per_channel)
    return stream_channel_sections(channels, harvest_limit, stream_channel_display_limit(per_channel), varied=True)


def stream_channel_candidates(channels_json, per_channel=400):
    channels = json.loads(channels_json or "[]")
    harvest_limit = max(1, min(as_int(per_channel) or 400, 500))
    return stream_channel_sections(channels, harvest_limit, harvest_limit * 2, varied=False)


def stream_channel_sections(channels, harvest_limit, output_limit, varied):
    logger = YtetLogger()
    sections = []
    options = {
        "cachedir": False,
        # "approximate_date" makes the channel tab return the "3주 전" upload text as a timestamp,
        # so 최신순/오래된순 can sort without a per-video metadata request.
        "extractor_args": {"youtubetab": {"approximate_date": [""]}},
        "extract_flat": "in_playlist",
        "ignoreerrors": True,
        "logger": logger,
        "no_warnings": False,
        "noplaylist": False,
        "playlistend": harvest_limit,
        "quiet": True,
    }
    with YoutubeDL(options) as ydl:
        for channel in channels:
            if not isinstance(channel, dict):
                continue
            channel_url = str(channel.get("url") or "").strip()
            if not channel_url:
                continue
            latest_info = None
            last_error = None
            for candidate_url in stream_channel_candidate_urls(channel_url):
                try:
                    latest_info = ydl.extract_info(candidate_url, download=False)
                    break
                except Exception as error:
                    last_error = error
                    continue
            if latest_info is None:
                logger.warning(f"스트림 채널 조회 실패: {channel_url} / {last_error}")
                continue
            popular_info = None
            for candidate_url in stream_channel_candidate_urls(channel_url, popular=True):
                try:
                    popular_info = ydl.extract_info(candidate_url, download=False)
                    break
                except Exception:
                    continue
            try:
                latest_videos = stream_channel_videos(latest_info, channel, harvest_limit, "latest")
                popular_videos = stream_channel_videos(popular_info, channel, harvest_limit, "popular") if popular_info else []
                videos = merge_stream_channel_videos(latest_videos, popular_videos, harvest_limit)
                if varied:
                    videos = varied_stream_channel_videos(videos, output_limit)
                else:
                    videos = unique_stream_channel_videos(videos, output_limit)
            except Exception as error:
                logger.warning(f"스트림 채널 조회 실패: {channel_url} / {error}")
                continue
            if not videos:
                continue
            sections.append({
                "id": str(channel.get("id") or latest_info.get("id") or stable_text_id(channel_url)),
                "title": stream_channel_title(latest_info, channel),
                "url": channel_url,
                "avatar": best_thumbnail(latest_info),
                "videos": videos,
            })
    return json.dumps(sections, ensure_ascii=False)


def stream_channel_quick_harvest_limit(per_channel):
    return max(per_channel, min(max(per_channel * 2, 80), 120))


def stream_channel_harvest_limit(per_channel):
    return max(per_channel, min(max(per_channel * 10, 200), 500))


def stream_channel_display_limit(per_channel):
    return max(1, per_channel)


def enrich_stream_videos(videos_json):
    videos = json.loads(videos_json or "[]")
    logger = YtetLogger()
    options = {
        "cachedir": False,
        "ignoreerrors": True,
        "logger": logger,
        "no_warnings": False,
        "noplaylist": True,
        "quiet": True,
    }
    with YoutubeDL(options) as ydl:
        return json.dumps(enrich_stream_video_metadata(ydl, videos), ensure_ascii=False)


def stream_channel_candidate_urls(channel_url, popular=False):
    url = str(channel_url or "").strip().split("?", 1)[0].rstrip("/")
    if not url:
        return []
    videos_url = url if "/videos" in url else url + "/videos"
    if popular:
        candidates = [
            videos_url + "?view=0&sort=p&flow=grid",
            videos_url + "?sort=p",
        ]
    else:
        candidates = [videos_url, url]
    return list(dict.fromkeys(candidates))


def stream_channel_videos(info, channel, per_channel, source_kind="latest"):
    if not isinstance(info, dict):
        return []
    entries = info.get("entries") if isinstance(info.get("entries"), list) else []
    candidates = []
    for source_index, entry in enumerate(entries):
        if not isinstance(entry, dict):
            continue
        title = str(entry.get("title") or "").strip()
        video_id = str(entry.get("id") or entry.get("url") or "").strip()
        if not title or title.lower() in {"[private video]", "[deleted video]"}:
            continue
        watch_url = stream_watch_url(entry)
        if not watch_url:
            continue
        candidates.append({
            "id": video_id or stable_text_id(watch_url),
            "title": title,
            "channel_title": stream_channel_title(info, channel),
            "url": watch_url,
            "thumbnail": best_thumbnail(entry) or best_thumbnail(info),
            "duration": as_int(entry.get("duration")) or 0,
            "view_count": stream_video_view_count(entry),
            "published": stream_video_published_rank(entry),
            "source_index": source_index,
            "popular_rank": source_index + 1 if source_kind == "popular" else 0,
        })
    return candidates[:per_channel]


def merge_stream_channel_videos(latest_videos, popular_videos, per_channel):
    merged = []
    by_key = {}

    def remember(item):
        key = stream_video_key(item)
        if key:
            by_key[key] = item
        merged.append(item)

    for item in latest_videos or []:
        remember(dict(item))

    for popular_index, item in enumerate(popular_videos or []):
        copy = dict(item)
        rank = as_int(copy.get("popular_rank")) or popular_index + 1
        key = stream_video_key(copy)
        existing = by_key.get(key)
        if existing is not None:
            if as_int(existing.get("view_count")) <= 0 and as_int(copy.get("view_count")) > 0:
                existing["view_count"] = copy.get("view_count")
            if not existing.get("thumbnail") and copy.get("thumbnail"):
                existing["thumbnail"] = copy.get("thumbnail")
            if as_int(existing.get("duration")) <= 0 and as_int(copy.get("duration")) > 0:
                existing["duration"] = copy.get("duration")
            existing["popular_rank"] = rank
            continue

        copy["source_index"] = per_channel + popular_index
        copy["popular_rank"] = rank
        remember(copy)

    return merged[:per_channel * 2]


def unique_stream_channel_videos(videos, limit=None):
    unique = []
    seen = set()
    for item in videos or []:
        key = stream_video_key(item)
        if not key or key in seen:
            continue
        seen.add(key)
        unique.append(dict(item))
        if limit and len(unique) >= limit:
            break
    return unique


def varied_stream_channel_videos(videos, limit, rng=None):
    unique = unique_stream_channel_videos(videos)
    if not unique:
        return []

    rng = rng or random.Random(time.time_ns())
    limit = max(1, min(as_int(limit) or len(unique), len(unique)))
    if len(unique) <= limit:
        selected = list(unique)
    else:
        selected = []
        selected_keys = set()

        def add(item):
            key = stream_video_key(item)
            if key and key not in selected_keys:
                selected_keys.add(key)
                selected.append(item)

        popular_quota = max(1, limit // 4)
        latest_quota = max(1, limit // 4)
        for item in sorted(unique, key=stream_video_popular_sort_key)[:popular_quota]:
            add(item)
        for item in sorted(unique, key=stream_video_latest_sort_key)[:latest_quota]:
            add(item)

        rest = [item for item in unique if stream_video_key(item) not in selected_keys]
        rng.shuffle(rest)
        for item in rest:
            add(item)
            if len(selected) >= limit:
                break

    rng.shuffle(selected)
    return selected


def stream_video_popular_sort_key(item):
    return (
        -(as_int(item.get("view_count")) or 0),
        -(as_int(item.get("published")) or 0),
        as_int(item.get("source_index")) or 0,
    )


def stream_video_latest_sort_key(item):
    return (
        -(as_int(item.get("published")) or 0),
        as_int(item.get("source_index")) or 0,
    )


def enrich_stream_video_metadata(ydl, videos):
    enriched = []
    for video in videos or []:
        item = dict(video)
        if stream_video_needs_metadata(item):
            try:
                info = ydl.extract_info(item.get("url"), download=False)
                if isinstance(info, dict):
                    apply_stream_video_metadata(item, info)
            except Exception:
                pass
        enriched.append(item)
    return enriched


def stream_video_needs_metadata(item):
    if not isinstance(item, dict):
        return False
    return as_int(item.get("view_count")) <= 0 or as_int(item.get("published")) <= 0


def apply_stream_video_metadata(item, info):
    view_count = stream_video_view_count(info)
    if view_count > 0:
        item["view_count"] = view_count
    published = stream_video_published_rank(info)
    if published > 0:
        item["published"] = published
    duration = as_int(info.get("duration"))
    if duration > 0:
        item["duration"] = duration
    thumbnail = best_thumbnail(info)
    if thumbnail:
        item["thumbnail"] = thumbnail
    video_id = str(info.get("id") or "").strip()
    if video_id:
        item["id"] = video_id


def stream_video_key(item):
    if not isinstance(item, dict):
        return ""
    return str(item.get("url") or item.get("id") or "").strip()


def stream_video_view_count(entry):
    if not isinstance(entry, dict):
        return 0
    for key in ("view_count", "viewCount", "views", "viewCountInt"):
        count = as_int(entry.get(key))
        if count > 0:
            return count
    for key in (
        "view_count_text",
        "viewCountText",
        "short_view_count_text",
        "shortViewCountText",
        "viewsText",
        "viewCountTextShort",
    ):
        count = compact_view_count(compact_text(entry.get(key)))
        if count > 0:
            return count
    return 0


def compact_text(value):
    if value is None:
        return ""
    if isinstance(value, str):
        return value
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, dict):
        for key in ("simpleText", "text", "content"):
            text = compact_text(value.get(key))
            if text:
                return text
        runs = value.get("runs")
        if isinstance(runs, list):
            return "".join(compact_text(item) for item in runs)
        accessibility = value.get("accessibility")
        if isinstance(accessibility, dict):
            text = compact_text(accessibility.get("accessibilityData"))
            if text:
                return text
        label = value.get("label")
        return compact_text(label)
    if isinstance(value, list):
        return "".join(compact_text(item) for item in value)
    return str(value)


def compact_view_count(value):
    text = str(value or "").strip().lower().replace(",", "")
    if not text:
        return 0
    match = re.search(r"(\d+(?:\.\d+)?)\s*([kmb]|천|만|억)?", text)
    if not match:
        return 0
    number = float(match.group(1))
    suffix = match.group(2) or ""
    multiplier = {
        "k": 1_000,
        "m": 1_000_000,
        "b": 1_000_000_000,
        "천": 1_000,
        "만": 10_000,
        "억": 100_000_000,
    }.get(suffix, 1)
    return int(number * multiplier)


def stream_video_published_rank(entry):
    upload_date = str(entry.get("upload_date") or "").strip()
    if re.fullmatch(r"\d{8}", upload_date):
        return as_int(upload_date) or 0
    timestamp = as_int(entry.get("timestamp") or entry.get("release_timestamp"))
    if timestamp:
        return as_int(time.strftime("%Y%m%d", time.gmtime(timestamp))) or 0
    return 0


def stream_watch_url(entry):
    webpage_url = str(entry.get("webpage_url") or "").strip()
    if webpage_url.startswith("http"):
        return webpage_url
    url = str(entry.get("url") or "").strip()
    if url.startswith("http"):
        return url
    video_id = str(entry.get("id") or url).strip()
    if video_id:
        return "https://www.youtube.com/watch?v=" + video_id
    return ""


def stream_channel_title(info, channel):
    configured = str(channel.get("title") or "").strip() if isinstance(channel, dict) else ""
    if isinstance(info, dict):
        title = str(info.get("uploader") or info.get("channel") or info.get("title") or "").strip()
        if title:
            return strip_video_suffix(title)
    return configured or "YouTube 채널"


def strip_video_suffix(title):
    return re.sub(r"\s*-\s*Videos\s*$", "", title or "", flags=re.IGNORECASE).strip()


def resolve_stream(url):
    url = str(url or "").strip()
    if not url:
        raise YtetExtractionError("재생할 YouTube URL이 없습니다.")
    logger = YtetLogger()
    options = {
        "cachedir": False,
        "format": "ba[ext=m4a]/bestaudio[ext=m4a]/ba/best",
        "logger": logger,
        "no_warnings": False,
        "noplaylist": True,
        "quiet": True,
    }
    with YoutubeDL(options) as ydl:
        info = ydl.extract_info(url, download=False)
    stream_url = str(info.get("url") or "").strip() if isinstance(info, dict) else ""
    if not stream_url and isinstance(info, dict):
        stream_url = best_stream_format_url(info)
    if not stream_url:
        raise YtetExtractionError("온라인 스트림 URL을 찾을 수 없습니다.")
    return json.dumps({
        "stream_url": stream_url,
        "title": info.get("title") or "",
        "channel_title": info.get("uploader") or info.get("channel") or "",
        "thumbnail": best_thumbnail(info),
        "duration": as_int(info.get("duration")) or 0,
    }, ensure_ascii=False)


def best_stream_format_url(info):
    formats = info.get("formats") if isinstance(info.get("formats"), list) else []
    audio_formats = []
    for item in formats:
        if not isinstance(item, dict):
            continue
        stream_url = str(item.get("url") or "").strip()
        if not stream_url:
            continue
        has_audio = item.get("acodec") not in (None, "none")
        has_video = item.get("vcodec") not in (None, "none")
        if has_audio and not has_video:
            audio_formats.append(item)
    if not audio_formats:
        return ""
    audio_formats.sort(key=lambda item: (
        0 if item.get("ext") == "m4a" else 1,
        -(as_int(item.get("abr")) or 0),
    ))
    return str(audio_formats[0].get("url") or "").strip()


def best_thumbnail(info):
    if not isinstance(info, dict):
        return ""
    thumbnails = info.get("thumbnails")
    if isinstance(thumbnails, list) and thumbnails:
        best = sorted(
            [thumb for thumb in thumbnails if isinstance(thumb, dict) and thumb.get("url")],
            key=lambda thumb: (as_int(thumb.get("width")) or 0) * (as_int(thumb.get("height")) or 0),
            reverse=True,
        )
        if best:
            return str(best[0].get("url") or "").strip()
    return str(info.get("thumbnail") or "").strip()


def stable_text_id(value):
    text = str(value or "").strip()
    return hex(abs(hash(text)))[2:] if text else "online"


def extract(
    workspace,
    url,
    media_type,
    option,
    include_subtitles,
    include_playlist=False,
    enhance_metadata=False,
    progress_listener=None,
    android_sdk=DEFAULT_ANDROID_SDK,
    cancel_checker=None,
):
    if not isinstance(include_playlist, bool):
        old_progress_listener = include_playlist
        old_android_sdk = progress_listener
        progress_listener = old_progress_listener
        include_playlist = False
        if old_android_sdk is not None:
            android_sdk = old_android_sdk
    elif not isinstance(enhance_metadata, bool):
        old_progress_listener = enhance_metadata
        old_android_sdk = progress_listener
        progress_listener = old_progress_listener
        enhance_metadata = False
        if old_android_sdk is not None:
            android_sdk = old_android_sdk

    os.makedirs(workspace, exist_ok=True)
    logger = YtetLogger()
    media_type = str(media_type or "audio")
    android_sdk = as_int(android_sdk) or DEFAULT_ANDROID_SDK
    check_canceled(cancel_checker)

    if media_type == "video":
        if include_playlist:
            raise YtetExtractionError("영상 플레이리스트 전체 추출은 아직 지원하지 않습니다. 음원 모드에서 전체 플레이리스트를 사용하세요.")
        extract_video(
            workspace,
            url,
            str(option or ""),
            bool(include_subtitles),
            progress_listener,
            logger,
            android_sdk,
            cancel_checker,
        )
        return

    options = build_options(
        workspace,
        media_type,
        str(option or ""),
        bool(include_subtitles),
        progress_listener,
        logger,
        include_playlist=bool(include_playlist),
        cancel_checker=cancel_checker,
    )

    try:
        expected_playlist_items = []
        if include_playlist:
            notify(progress_listener, 6, "분석", "플레이리스트 항목 확인 중")
            expected_playlist_items = probe_playlist_items(url, logger, cancel_checker)
        with YoutubeDL(options) as ydl:
            check_canceled(cancel_checker)
            info = ydl.extract_info(url, download=True)
            check_canceled(cancel_checker)
            mark_single_video_playlist(info)
            if enhance_metadata and not is_single_video_playlist(info):
                info = enhance_music_metadata(info, progress_listener, logger, cancel_checker)
                check_canceled(cancel_checker)
            embed_audio_covers(workspace, info, logger, ydl)
            check_canceled(cancel_checker)
            if enhance_metadata:
                rename_metadata_matched_audio_files(workspace, info, logger, ydl)
            if include_playlist:
                write_playlist_extraction_report(workspace, info, logger, ydl, expected_playlist_items)
                relocate_playlist_folder_for_metadata(workspace, info, logger)
            check_canceled(cancel_checker)
    except YtetCancellationError:
        raise
    except YtetExtractionError:
        raise
    except DownloadError as error:
        message = clean_error(str(error), logger)
        if is_cancel_message(message):
            raise YtetCancellationError("추출이 취소되었습니다.") from error
        raise YtetExtractionError(message) from error
    except Exception as error:
        message = clean_error(str(error), logger)
        if is_cancel_message(message):
            raise YtetCancellationError("추출이 취소되었습니다.") from error
        raise YtetExtractionError(message) from error

def build_options(
    workspace,
    media_type,
    option,
    include_subtitles,
    progress_listener,
    logger,
    include_playlist=False,
    cancel_checker=None,
):
    options = {
        "cachedir": False,
        "logger": logger,
        "no_warnings": False,
        "noplaylist": not bool(include_playlist),
        "outtmpl": output_template(workspace, include_playlist),
        "overwrites": True,
        "progress_hooks": [progress_hook(progress_listener, cancel_checker)],
        "quiet": True,
    }
    if include_playlist:
        options["ignoreerrors"] = True

    if media_type == "video":
        raise YtetExtractionError("영상은 Android 병합 경로로 처리합니다.")

    if option == "mp3":
        raise YtetExtractionError("MP3 변환은 현재 Android APK에 FFmpeg 변환 런타임이 없어 사용할 수 없습니다. M4A 또는 Original Opus를 선택하세요.")
    if option == "original":
        options["format"] = "ba/best"
    else:
        options["format"] = "ba[ext=m4a]/ba/best"
    return options


def playlist_probe_options(logger):
    return {
        "cachedir": False,
        "extract_flat": "in_playlist",
        "ignoreerrors": True,
        "logger": logger,
        "no_warnings": False,
        "noplaylist": False,
        "quiet": True,
    }


def probe_playlist_items(url, logger, cancel_checker=None):
    check_canceled(cancel_checker)
    try:
        with YoutubeDL(playlist_probe_options(logger)) as ydl:
            info = ydl.extract_info(url, download=False)
        check_canceled(cancel_checker)
        return expected_playlist_items(info)
    except YtetCancellationError:
        raise
    except Exception as error:
        logger.warning(f"플레이리스트 항목 확인 실패: {error}")
        return []


def expected_playlist_items(info):
    if not isinstance(info, dict):
        return []
    entries = info.get("entries")
    if not isinstance(entries, list):
        return []
    items = []
    for position, entry in enumerate(entries, start=1):
        if isinstance(entry, dict):
            index = playlist_item_index(entry, position)
            items.append({
                "index": index,
                "keys": playlist_item_keys(entry),
                "label": report_entry_label(entry, index),
            })
        else:
            items.append({
                "index": position,
                "keys": set(),
                "label": f"{position}번째 항목",
            })
    return items


def output_template(workspace, include_playlist=False):
    if include_playlist:
        return os.path.join(workspace, "%(playlist)s", "%(playlist_index)03d - %(uploader)s - %(title)s.%(ext)s")
    return os.path.join(workspace, "%(uploader)s - %(title)s.%(ext)s")


def extract_video(
    workspace,
    url,
    option,
    include_subtitles,
    progress_listener,
    logger,
    android_sdk=DEFAULT_ANDROID_SDK,
    cancel_checker=None,
):
    try:
        check_canceled(cancel_checker)
        notify(progress_listener, 8, "분석", "영상 형식 확인 중")
        with YoutubeDL(base_options(logger, progress_listener, cancel_checker)) as ydl:
            info = ydl.extract_info(url, download=False)

        check_canceled(cancel_checker)
        plan = video_track_plan(info, option, android_sdk)
        if plan["mode"] == "mux":
            notify(progress_listener, 12, "다운로드", "고화질 영상 트랙 다운로드 중")
            download_one(url, workspace, plan["video"]["format_id"], "video-track.%(ext)s", logger, progress_listener, cancel_checker)
            check_canceled(cancel_checker)
            notify(progress_listener, 52, "다운로드", "오디오 트랙 다운로드 중")
            download_one(url, workspace, plan["audio"]["format_id"], "audio-track.%(ext)s", logger, progress_listener, cancel_checker)
        else:
            notify(progress_listener, 12, "다운로드", "단일 파일 영상 다운로드 중")
            outtmpl = "video-track.%(ext)s" if include_subtitles else final_outtmpl(info)
            download_one(url, workspace, plan["format"], outtmpl, logger, progress_listener, cancel_checker)

        check_canceled(cancel_checker)
        if include_subtitles:
            notify(progress_listener, 84, "자막", "자막 다운로드 중")
            download_subtitles(url, workspace, info, logger, progress_listener, cancel_checker)
        if plan["mode"] == "mux":
            write_mux_manifest(workspace, info, plan)
        elif include_subtitles:
            write_single_video_manifest(workspace, info)
        check_canceled(cancel_checker)
    except YtetCancellationError:
        raise
    except YtetExtractionError:
        raise
    except DownloadError as error:
        message = clean_error(str(error), logger)
        if is_cancel_message(message):
            raise YtetCancellationError("추출이 취소되었습니다.") from error
        raise YtetExtractionError(message) from error
    except Exception as error:
        message = clean_error(str(error), logger)
        if is_cancel_message(message):
            raise YtetCancellationError("추출이 취소되었습니다.") from error
        raise YtetExtractionError(message) from error


def base_options(logger, progress_listener, cancel_checker=None):
    return {
        "cachedir": False,
        "logger": logger,
        "no_warnings": False,
        "noplaylist": True,
        "overwrites": True,
        "progress_hooks": [progress_hook(progress_listener, cancel_checker)],
        "quiet": True,
    }


def download_one(url, workspace, format_id, outtmpl, logger, progress_listener, cancel_checker=None):
    options = {
        **base_options(logger, progress_listener, cancel_checker),
        "format": format_id,
        "outtmpl": os.path.join(workspace, outtmpl),
    }
    with YoutubeDL(options) as ydl:
        ydl.download([url])


def download_subtitles(url, workspace, info, logger, progress_listener, cancel_checker=None):
    options = {
        **base_options(logger, progress_listener, cancel_checker),
        "skip_download": True,
        "outtmpl": os.path.join(workspace, final_stem(info) + ".%(ext)s"),
        "writesubtitles": True,
        "writeautomaticsub": False,
        "subtitleslangs": SUBTITLE_LANGUAGES,
        "subtitlesformat": "srt/vtt/best",
    }
    with YoutubeDL(options) as ydl:
        ydl.download([url])


def video_track_plan(info, option, android_sdk=DEFAULT_ANDROID_SDK):
    max_height = video_max_height(option)
    if max_height is not None:
        video = select_video_format(info, "mp4", DEFAULT_ANDROID_SDK, max_height=max_height, prefer_avc=True)
        audio = select_audio_format(info, "mp4", DEFAULT_ANDROID_SDK)
        if video and audio:
            return mux_plan(video, audio, "mp4")
        video = select_best_video_format(info, max_height=max_height)
        audio = select_best_audio_format(info)
        if video and audio:
            return mux_plan(video, audio, "mkv")
        return {"mode": "single", "format": single_file_video_selector(option)}

    plan = best_mux_plan(info)
    if plan:
        return plan
    return {"mode": "single", "format": single_file_video_selector(option)}


def best_mux_plan(info):
    video = select_best_video_format(info)
    audio = select_best_audio_format(info)
    if video and audio:
        return mux_plan(video, audio, "mkv")
    return None


def mux_plan(video, audio, container):
    return {"mode": "mux", "video": video, "audio": audio, "container": container}


def select_video_format(info, container, android_sdk, max_height=None, prefer_avc=False):
    formats = media_formats(info)
    candidates = [
        item for item in formats
        if is_video_only(item)
        and item.get("format_id")
        and mux_container_for_video(item, android_sdk) == container
        and (max_height is None or as_int(item.get("height")) <= max_height)
    ]
    if not candidates:
        return None

    avc_candidates = [item for item in candidates if is_avc_codec(item.get("vcodec"))]
    if prefer_avc and avc_candidates:
        candidates = avc_candidates

    return sorted(candidates, key=format_score, reverse=True)[0]


def select_audio_format(info, container="mp4", android_sdk=DEFAULT_ANDROID_SDK):
    candidates = [
        item for item in media_formats(info)
        if is_audio_only(item)
        and item.get("format_id")
        and mux_container_for_audio(item, android_sdk) == container
    ]
    if not candidates:
        return None
    return sorted(candidates, key=audio_score, reverse=True)[0]


def select_best_video_format(info, max_height=None):
    candidates = [
        item for item in media_formats(info)
        if is_video_only(item)
        and item.get("format_id")
        and (max_height is None or as_int(item.get("height")) <= max_height)
    ]
    if not candidates:
        return None
    return sorted(candidates, key=format_score, reverse=True)[0]


def select_best_audio_format(info):
    candidates = [
        item for item in media_formats(info)
        if is_audio_only(item)
        and item.get("format_id")
    ]
    if not candidates:
        return None
    return sorted(candidates, key=audio_score, reverse=True)[0]


def mux_container_for_video(item, android_sdk):
    ext = (first_text(item.get("ext")) or "").lower()
    codec = first_text(item.get("vcodec"))
    if ext == "mp4" and is_mp4_video_codec_supported(codec, android_sdk):
        return "mp4"
    if ext == "webm" and is_webm_video_codec_supported(codec, android_sdk):
        return "webm"
    return None


def mux_container_for_audio(item, android_sdk):
    ext = (first_text(item.get("ext")) or "").lower()
    codec = first_text(item.get("acodec"))
    if ext == "m4a" and is_aac_codec(codec):
        return "mp4"
    if ext == "webm" and is_webm_audio_codec_supported(codec, android_sdk):
        return "webm"
    return None


def is_mp4_video_codec_supported(codec, android_sdk):
    codec = (codec or "").lower()
    if codec.startswith(("avc1", "avc3", "mp4v", "h263")):
        return True
    if codec.startswith(("hvc1", "hev1")):
        return android_sdk >= 24
    if codec.startswith(("dvhe", "dvh1")):
        return android_sdk >= 33
    if codec.startswith("av01"):
        return android_sdk >= 34
    return False


def is_webm_video_codec_supported(codec, android_sdk):
    codec = (codec or "").lower()
    if codec.startswith("vp8"):
        return android_sdk >= 21
    if codec.startswith(("vp9", "vp09")):
        return android_sdk >= 24
    return False


def is_webm_audio_codec_supported(codec, android_sdk):
    codec = (codec or "").lower()
    if codec.startswith("vorbis"):
        return android_sdk >= 21
    if codec.startswith("opus"):
        return android_sdk >= 29
    return False


def is_avc_codec(codec):
    return str(codec or "").lower().startswith(("avc1", "avc3"))


def is_aac_codec(codec):
    return str(codec or "").lower().startswith("mp4a")


def media_formats(info):
    formats = info.get("formats") if isinstance(info, dict) else None
    return [item for item in formats or [] if isinstance(item, dict)]


def is_video_only(item):
    return item.get("vcodec") not in {None, "none"} and item.get("acodec") in {None, "none"}


def is_audio_only(item):
    return item.get("acodec") not in {None, "none"} and item.get("vcodec") in {None, "none"}


def video_max_height(option):
    if option in {"1080", "1080p"}:
        return 1080
    if option in {"720", "720p"}:
        return 720
    if option in {"480", "480p"}:
        return 480
    return None


def single_file_video_selector(option):
    if option in {"1080", "1080p"}:
        return "b[height<=1080][ext=mp4]/b[height<=1080]/b[ext=mp4]/best"
    if option in {"720", "720p"}:
        return "b[height<=720][ext=mp4]/b[height<=720]/b[ext=mp4]/best"
    if option in {"480", "480p"}:
        return "b[height<=480][ext=mp4]/b[height<=480]/b[ext=mp4]/best"
    return "best"


def format_score(item):
    codec = str(item.get("vcodec") or "").lower()
    codec_score = 4 if codec.startswith("av01") else 3 if codec.startswith(("vp9", "vp09")) else 2 if codec.startswith(("hvc1", "hev1")) else 1 if codec.startswith(("avc1", "avc3")) else 0
    return (
        as_int(item.get("height")),
        as_int(item.get("width")),
        as_int(item.get("fps")),
        as_float(item.get("tbr")),
        codec_score,
    )


def audio_score(item):
    return (as_float(item.get("abr")), as_float(item.get("tbr")))


def as_int(value):
    try:
        return int(value or 0)
    except (TypeError, ValueError):
        return 0


def as_float(value):
    try:
        return float(value or 0)
    except (TypeError, ValueError):
        return 0.0


def write_mux_manifest(workspace, info, plan):
    video = find_prefixed_file(workspace, "video-track.")
    audio = find_prefixed_file(workspace, "audio-track.")
    if not video or not audio:
        raise YtetExtractionError("병합할 영상 또는 오디오 트랙을 찾지 못했습니다.")
    container = plan.get("container") or "mp4"
    manifest = {
        "video": os.path.basename(video),
        "audio": os.path.basename(audio),
        "container": container,
        "output": final_stem(info) + "." + container,
    }
    with open(os.path.join(workspace, "mux.json"), "w", encoding="utf-8") as file:
        json.dump(manifest, file, ensure_ascii=False)


def write_single_video_manifest(workspace, info):
    video = find_prefixed_file(workspace, "video-track.")
    if not video:
        raise YtetExtractionError("자막을 삽입할 영상 파일을 찾지 못했습니다.")
    extension = os.path.splitext(video)[1].lower()
    if extension not in {".mp4", ".m4v", ".mov", ".mkv"} and find_subtitle_files(workspace):
        extension = ".mkv"
    manifest = {
        "video": os.path.basename(video),
        "output": final_stem(info) + extension,
    }
    with open(os.path.join(workspace, "mux.json"), "w", encoding="utf-8") as file:
        json.dump(manifest, file, ensure_ascii=False)


def final_outtmpl(info):
    return final_stem(info) + ".%(ext)s"


def final_stem(info):
    if not isinstance(info, dict):
        return "RabbYT"
    left = first_text(info.get("uploader"), info.get("channel"), info.get("artist"))
    title = first_text(info.get("title"), info.get("track"), info.get("id"), "RabbYT")
    return sanitize_filename(f"{left} - {title}" if left and left != title else title)


def sanitize_filename(name):
    text = re.sub(r'[\\/:*?"<>|\r\n]+', " ", str(name or "RabbYT")).strip()
    text = re.sub(r"\s+", " ", text)
    return text[:160].rstrip(" .") or "RabbYT"


def first_text(*values):
    for value in values:
        if value:
            return str(value).strip()
    return None


def enhance_music_metadata(info, progress_listener, logger, cancel_checker=None):
    if not isinstance(info, dict):
        return info
    if is_single_video_playlist(info):
        logger.info("MusicBrainz 보정 건너뜀: 긴 단일 플레이리스트 영상")
        return info
    try:
        check_canceled(cancel_checker)
        notify(progress_listener, 91, "메타데이터", "MusicBrainz 앨범 정보 검색 중")
        client = MusicBrainzClient(logger)
        entries = [entry for entry in info.get("entries") or [] if isinstance(entry, dict)]
        if entries:
            enhance_playlist_metadata(info, entries, client, logger, progress_listener, cancel_checker)
        else:
            metadata = match_recording_metadata(info, client, cancel_checker=cancel_checker)
            if metadata:
                apply_metadata_override(info, metadata)
        check_canceled(cancel_checker)
    except YtetCancellationError:
        raise
    except Exception as error:
        logger.warning(f"MusicBrainz 보정 실패: {error}")
    return info


def mark_single_video_playlist(info):
    if isinstance(info, dict) and is_single_video_playlist_candidate(info):
        info["__ytet_single_video_playlist"] = True


def is_single_video_playlist(info):
    return isinstance(info, dict) and bool(info.get("__ytet_single_video_playlist"))


def is_single_video_playlist_candidate(info):
    if not isinstance(info, dict) or info.get("entries"):
        return False
    duration = as_int(info.get("duration"))
    if not duration or duration < 20 * 60:
        return False
    return has_playlist_title_marker(info.get("title"))


def has_playlist_title_marker(value):
    text = normalize_text(value).casefold()
    if not text:
        return False
    return re.search(r"(^|[\s\[(（【|:：\-–—])playlist([\s\])）】|:：\-–—]|$)", text) is not None \
        or "플레이리스트" in text


def clean_single_video_playlist_title(value):
    text = normalize_text(value)
    text = re.sub(r"(?i)^\s*[\[(（【]\s*playlist\s*[\])）】]\s*", "", text)
    text = re.sub(r"(?i)^\s*playlist\s*([|:：\-–—]+)\s*", "", text)
    return clean_music_title(text)


class MusicBrainzClient:
    def __init__(self, logger):
        self.logger = logger
        self.last_request_at = 0
        self.cache = {}

    def search(self, entity, query, limit=5):
        return self.get(f"/{entity}", {"query": query, "fmt": "json", "limit": str(limit)})

    def lookup_release(self, release_id):
        return self.get(f"/release/{release_id}", {"inc": "recordings+artist-credits+media+release-groups", "fmt": "json"})

    def get(self, path, params):
        url = MUSICBRAINZ_API_ROOT + path + "?" + urlencode(params)
        if url in self.cache:
            return self.cache[url]
        elapsed = time.monotonic() - self.last_request_at
        if self.last_request_at and elapsed < MUSICBRAINZ_MIN_INTERVAL:
            time.sleep(MUSICBRAINZ_MIN_INTERVAL - elapsed)
        request = Request(url, headers={
            "Accept": "application/json",
            "User-Agent": MUSICBRAINZ_USER_AGENT,
        })
        try:
            with urlopen(request, timeout=20) as response:
                data = json.loads(response.read().decode("utf-8"))
        finally:
            self.last_request_at = time.monotonic()
        self.cache[url] = data
        return data


def enhance_playlist_metadata(info, entries, client, logger, progress_listener=None, cancel_checker=None):
    check_canceled(cancel_checker)
    artist_candidate, album_candidate = playlist_artist_album_candidates(info)
    if album_candidate:
        notify(progress_listener, 91, "메타데이터", "MusicBrainz 앨범 후보 검색 중")
    release = match_release_for_playlist(entries, artist_candidate, album_candidate, client, cancel_checker)
    if release:
        matched = apply_release_metadata(entries, release)
        logger.info(f"MusicBrainz release matched {matched}/{len(entries)} tracks")

    total = len(entries)
    for index, entry in enumerate(entries, start=1):
        check_canceled(cancel_checker)
        if entry.get("__ytet_metadata"):
            continue
        notify(progress_listener, 91, "메타데이터", f"MusicBrainz 검색 중 {index}/{total}")
        metadata = match_recording_metadata(entry, client, artist_candidate=artist_candidate, album_candidate=album_candidate, cancel_checker=cancel_checker)
        if metadata:
            apply_metadata_override(entry, metadata)
    check_canceled(cancel_checker)
    notify(progress_listener, 91, "메타데이터", "MusicBrainz 검색 완료")


def playlist_artist_album_candidates(info):
    title = first_text(info.get("playlist"), info.get("playlist_title"), info.get("title"))
    if not title:
        return None, None
    cleaned = remove_bracket_descriptors(normalize_text(title))
    for separator in (" / ", " - ", " – ", " — ", " | ", " : "):
        if separator in cleaned:
            left, right = cleaned.split(separator, 1)
            artist = clean_music_title(left)
            album = clean_album_candidate(right)
            if artist and album:
                return artist, album
    return None, clean_album_candidate(cleaned)


def clean_album_candidate(value):
    text = remove_bracket_descriptors(value)
    text = re.sub(r"(?i)\bfull\s+album\s+topic\b", " ", text)
    text = re.sub(r"(?i)\bfull\s+album\b", " ", text)
    text = re.sub(r"(?i)\balbum\s+topic\b", " ", text)
    for pattern in (r"(?i)\bclean\b", r"(?i)\bexplicit\b"):
        cleaned = clean_music_title(re.sub(pattern, " ", text))
        if cleaned:
            text = cleaned
    without_year = clean_music_title(re.sub(r"\b(?:19|20)\d{2}\b\s*$", " ", text))
    if without_year:
        text = without_year
    return clean_music_title(text)


def match_release_for_playlist(entries, artist_candidate, album_candidate, client, cancel_checker=None):
    if not album_candidate:
        return None
    check_canceled(cancel_checker)
    clauses = [f'release:"{mb_escape(album_candidate)}"']
    if artist_candidate:
        clauses.append(f'artist:"{mb_escape(artist_candidate)}"')
    results = client.search("release", " AND ".join(clauses), limit=10).get("releases") or []
    check_canceled(cancel_checker)
    minimum_matches = max(2, int(round(len(entries) * 0.5))) if len(entries) > 1 else 1
    best_release = None
    best_rank = None
    for release_candidate in results:
        check_canceled(cancel_checker)
        release_id = release_candidate.get("id")
        if not release_id:
            continue
        release_score = as_int(release_candidate.get("score")) or 0
        title_score = similarity(album_candidate, release_candidate.get("title"))
        artist_score = similarity(artist_candidate, artist_credit_name(release_candidate.get("artist-credit"))) if artist_candidate else 0.7
        search_score = release_score / 100 * 0.45 + title_score * 0.35 + artist_score * 0.20
        if search_score < 0.72:
            continue
        release = client.lookup_release(release_id)
        tracks = release_tracks(release)
        matches = release_track_matches(entries, release)
        if len(entries) > 1 and matches < minimum_matches:
            continue
        track_count_delta = abs(len(tracks) - len(entries))
        exact_count = 1 if len(tracks) == len(entries) else 0
        rank = (matches, exact_count, -track_count_delta, search_score)
        if best_rank is None or rank > best_rank:
            best_release = release
            best_rank = rank
        if len(entries) > 1 and exact_count and matches == len(entries):
            return release
    return best_release


def release_track_matches(entries, release):
    tracks = release_tracks(release)
    matches = 0
    for entry in entries:
        track, score = best_track_match(entry, tracks)
        if track is not None and score >= 0.82:
            matches += 1
    return matches


def apply_release_metadata(entries, release):
    tracks = release_tracks(release)
    release_title = first_text(release.get("title"))
    release_artist = artist_credit_name(release.get("artist-credit"))
    release_date = first_text(release.get("date"))
    total = len(tracks)
    matched = 0
    used = set()
    for entry in entries:
        track, score = best_track_match(entry, tracks, used)
        if not track or score < 0.82:
            continue
        used.add(track["index"])
        metadata = {
            "title": track["title"],
            "artist": track["artist"] or release_artist,
            "album": release_title,
            "album_artist": release_artist,
            "date": release_date,
            "track_number": track["position"],
            "track_total": total,
        }
        apply_metadata_override(entry, metadata)
        matched += 1
    return matched


def release_tracks(release):
    tracks = []
    for medium in release.get("media") or []:
        for track in medium.get("tracks") or []:
            recording = track.get("recording") or {}
            title = first_text(track.get("title"), recording.get("title"))
            if not title:
                continue
            tracks.append({
                "index": len(tracks),
                "title": title,
                "artist": artist_credit_name(track.get("artist-credit")) or artist_credit_name(recording.get("artist-credit")),
                "position": as_int(track.get("position") or track.get("number")) or len(tracks) + 1,
                "duration": as_int(track.get("length") or recording.get("length")),
            })
    return tracks


def best_track_match(entry, tracks, used=None):
    used = used or set()
    candidates = title_candidates(metadata_title(entry))
    entry_duration = duration_ms(entry)
    best = None
    best_score = 0
    for track in tracks:
        if track["index"] in used:
            continue
        title_score = max((similarity(candidate, track["title"]) for candidate in candidates), default=0)
        duration_score = duration_similarity(entry_duration, track.get("duration"))
        score = title_score * 0.82 + duration_score * 0.18
        if score > best_score:
            best = track
            best_score = score
    return best, best_score


def match_recording_metadata(info, client, artist_candidate=None, album_candidate=None, cancel_checker=None):
    check_canceled(cancel_checker)
    titles = title_candidates(metadata_title(info))
    if not titles:
        return None
    artist = artist_candidate or metadata_artist(info)
    query_title = titles[0]
    clauses = [f'recording:"{mb_escape(query_title)}"']
    if artist:
        clauses.append(f'artistname:"{mb_escape(artist)}"')
    if album_candidate:
        clauses.append(f'release:"{mb_escape(album_candidate)}"')
    results = client.search("recording", " AND ".join(clauses), limit=5).get("recordings") or []
    check_canceled(cancel_checker)
    if not results and artist and len(titles) > 1:
        results = client.search("recording", f'recording:"{mb_escape(titles[1])}" AND artistname:"{mb_escape(artist)}"', limit=5).get("recordings") or []
        check_canceled(cancel_checker)
    best, score = best_recording_result(info, results, titles, artist)
    if not best or score < 0.80:
        return None
    release = best_recording_release(best, album_candidate)
    return {
        "title": first_text(best.get("title"), query_title),
        "artist": artist_credit_name(best.get("artist-credit")) or artist,
        "album": release.get("title") if release else album_candidate,
        "album_artist": artist_credit_name(release.get("artist-credit")) if release else None,
        "date": release.get("date") if release else first_text(best.get("first-release-date")),
        "track_number": release_track_position(release),
        "track_total": release_track_count(release),
    }


def best_recording_result(info, results, title_candidates_list, artist_candidate):
    entry_duration = duration_ms(info)
    best = None
    best_score = 0
    for item in results:
        api_score = (as_int(item.get("score")) or 0) / 100
        title_score = max((similarity(candidate, item.get("title")) for candidate in title_candidates_list), default=0)
        artist_score = similarity(artist_candidate, artist_credit_name(item.get("artist-credit"))) if artist_candidate else 0.7
        if artist_candidate and api_score >= 0.95:
            artist_score = max(artist_score, 0.75)
        duration_score = duration_similarity(entry_duration, as_int(item.get("length")))
        release_score = 0.1 if item.get("releases") else 0
        score = api_score * 0.35 + title_score * 0.30 + artist_score * 0.20 + duration_score * 0.10 + release_score
        if score > best_score:
            best = item
            best_score = score
    return best, best_score


def best_recording_release(recording, album_candidate=None):
    releases = recording.get("releases") or []
    if not releases:
        return None
    if album_candidate:
        return sorted(releases, key=lambda release: similarity(album_candidate, release.get("title")), reverse=True)[0]
    return releases[0]


def release_track_position(release):
    if not release:
        return None
    for medium in release.get("media") or []:
        for track in medium.get("tracks") or []:
            position = as_int(track.get("position") or track.get("number"))
            if position:
                return position
    return None


def release_track_count(release):
    if not release:
        return None
    for medium in release.get("media") or []:
        count = as_int(medium.get("track-count"))
        if count:
            return count
    return None


def apply_metadata_override(info, metadata):
    clean = {key: value for key, value in (metadata or {}).items() if value}
    if clean:
        info["__ytet_metadata"] = clean


def artist_credit_name(artist_credit):
    if not isinstance(artist_credit, list):
        return None
    parts = []
    for item in artist_credit:
        if isinstance(item, str):
            parts.append(item)
        elif isinstance(item, dict):
            parts.append(first_text(item.get("name"), (item.get("artist") or {}).get("name")))
            if item.get("joinphrase"):
                parts.append(item.get("joinphrase"))
    return normalize_artist_text("".join(part for part in parts if part))


def normalize_artist_text(value):
    text = normalize_text(value)
    if not text:
        return None
    text = re.sub(r"(?i)\s+w\.?\s+", " with. ", text)
    text = re.sub(r"(?i)\s+w\s*/\s*", " with. ", text)
    text = re.sub(r"(?i)\s+with\.?\s+", " with. ", text)
    text = re.sub(r"(?i)\s+ft\.?\s+", " feat. ", text)
    text = re.sub(r"(?i)\s+feat\.?\s+", " feat. ", text)
    text = re.sub(r"(?i)\s+featuring\s+", " feat. ", text)
    return normalize_text(text) or None


def title_candidates(title):
    base = clean_music_title(title)
    if not base:
        return []
    variants = [base, remove_bracket_descriptors(base), remove_media_descriptors(base)]
    variants.append(remove_media_descriptors(remove_bracket_descriptors(base)))
    out = []
    seen = set()
    for variant in variants:
        clean = clean_music_title(variant)
        key = comparable_text(clean)
        if clean and key and key not in seen:
            out.append(clean)
            seen.add(key)
    return sorted(out, key=lambda item: (descriptor_penalty(item), len(item)))


def clean_music_title(value):
    text = normalize_text(value)
    text = re.sub(r"^[\[(（【][^\])）】]{0,40}[\])）】]\s*", "", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip(" -_|:;.,")


def normalize_text(value):
    text = unicodedata.normalize("NFKC", str(value or ""))
    text = text.replace("\u200b", " ").replace("\ufeff", " ")
    text = text.replace("“", '"').replace("”", '"').replace("‘", "'").replace("’", "'")
    text = text.replace("＿", "_").replace("_", " ")
    return re.sub(r"\s+", " ", text).strip()


def remove_bracket_descriptors(value):
    text = normalize_text(value)

    def replace(match):
        content = match.group(1)
        return "" if is_descriptor_text(content) else " " + content + " "

    text = re.sub(r"[\[(（【](.*?)[\])）】]", replace, text)
    return clean_music_title(text)


def remove_media_descriptors(value):
    text = normalize_text(value)
    patterns = [
        r"official\s*(music\s*)?video",
        r"official\s*audio",
        r"lyric\s*video",
        r"lyrics?",
        r"visuali[sz]er",
        r"music\s*video",
        r"m\s*/\s*v",
        r"mv",
        r"audio",
        r"eng\s*sub",
        r"kor\s*sub",
        r"\beng\b",
        r"\bkor\b",
        r"한글\s*자막",
        r"가사\s*(해석)?",
        r"뮤직비디오",
        r"뮤비",
    ]
    for pattern in patterns:
        text = re.sub(pattern, " ", text, flags=re.IGNORECASE)
    text = re.sub(r"[\[(（【]\s*[/|,;:-]*\s*[\])）】]", " ", text)
    return clean_music_title(text)


def is_descriptor_text(value):
    comparable = comparable_text(value)
    if not comparable:
        return True
    descriptor_tokens = {
        "official", "video", "musicvideo", "audio", "lyrics", "lyricvideo",
        "visualizer", "visualiser", "mv", "eng", "english", "sub", "subtitle",
        "remaster", "remastered", "fullalbum", "album", "live", "teaser",
        "한글자막", "가사", "뮤비", "뮤직비디오", "라이브",
    }
    compact = comparable.replace(" ", "")
    if compact in descriptor_tokens:
        return True
    return any(token in compact for token in descriptor_tokens)


def descriptor_penalty(value):
    comparable = comparable_text(value)
    return sum(1 for token in ("official", "video", "lyric", "audio", "visualizer", "remaster") if token in comparable)


def comparable_text(value):
    text = normalize_text(value).casefold()
    text = re.sub(r"[^\w가-힣ぁ-ゟ゠-ヿ一-龯]+", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def similarity(left, right):
    left_key = comparable_text(left)
    right_key = comparable_text(right)
    if not left_key or not right_key:
        return 0
    if left_key == right_key:
        return 1
    return SequenceMatcher(None, left_key, right_key).ratio()


def duration_ms(info):
    if not isinstance(info, dict):
        return None
    duration = info.get("duration")
    if duration is None:
        return None
    try:
        return int(float(duration) * 1000)
    except (TypeError, ValueError):
        return None


def duration_similarity(left_ms, right_ms):
    if not left_ms or not right_ms:
        return 0.5
    delta = abs(left_ms - right_ms)
    if delta <= 2000:
        return 1
    if delta >= 30000:
        return 0
    return max(0, 1 - delta / 30000)


def mb_escape(value):
    return str(value or "").replace("\\", "\\\\").replace('"', '\\"')


def embed_audio_covers(workspace, info, logger, ydl=None):
    entries = info.get("entries") if isinstance(info, dict) else None
    album_fallback = metadata_album(info, first_text(info.get("title")) if entries else None)
    if entries:
        for entry in entries:
            if isinstance(entry, dict):
                embed_audio_cover(workspace, entry, logger, ydl, fallback_to_first=False, album_fallback=album_fallback)
        return
    embed_audio_cover(workspace, info, logger, ydl, fallback_to_first=True, album_fallback=album_fallback)


def embed_audio_cover(workspace, info, logger, ydl=None, fallback_to_first=True, album_fallback=None):
    audio_path = audio_path_for_info(workspace, info, ydl)
    if not audio_path and fallback_to_first:
        audio_path = find_audio_file(workspace)
    if not audio_path or os.path.splitext(audio_path)[1].lower() != ".m4a":
        return
    try:
        from mutagen.mp4 import MP4, MP4Cover
        audio = MP4(audio_path)
        if audio.tags is None:
            audio.add_tags()
        write_mp4_metadata(audio, info, album_fallback)
        cover = download_cover_image(info, workspace, logger)
        if cover:
            image_format = MP4Cover.FORMAT_PNG if cover["mime"] == "image/png" else MP4Cover.FORMAT_JPEG
            audio.tags["covr"] = [MP4Cover(cover["data"], imageformat=image_format)]
        audio.save()
    except Exception as error:
        logger.warning(f"오디오 메타데이터 임베딩 실패: {error}")


def rename_metadata_matched_audio_files(workspace, info, logger, ydl=None):
    entries = info.get("entries") if isinstance(info, dict) else None
    if entries:
        for entry in entries:
            if isinstance(entry, dict):
                rename_metadata_matched_audio_file(workspace, entry, logger, ydl)
        return
    rename_metadata_matched_audio_file(workspace, info, logger, ydl)


def write_playlist_extraction_report(workspace, info, logger, ydl=None, expected_items=None):
    if not isinstance(info, dict):
        return
    entries = info.get("entries")
    if not isinstance(entries, list):
        return
    expected_items = expected_items or []
    total = len(expected_items) or as_int(info.get("n_entries") or info.get("playlist_count")) or len(entries)
    expected_by_index = {
        item["index"]: item
        for item in expected_items
        if item.get("index")
    }
    expected_by_key = {}
    for item in expected_items:
        for key in item.get("keys") or set():
            expected_by_key[key] = item
    matched_expected = set()
    succeeded = []
    failed = []
    for position, entry in enumerate(entries, start=1):
        if not isinstance(entry, dict):
            expected = expected_by_index.get(position)
            failed.append(report_item(
                expected,
                position,
                f"{position}번째 항목",
                reason="비공개 또는 사용할 수 없는 항목",
            ))
            mark_expected(matched_expected, expected)
            continue
        index = playlist_item_index(entry, position)
        expected = match_expected_playlist_item(entry, index, expected_by_key, expected_by_index)
        label = (expected or {}).get("label") or report_entry_label(entry, index)
        audio_path = audio_path_for_info(workspace, entry, ydl)
        if audio_path and os.path.exists(audio_path) and os.path.splitext(audio_path)[1].lower() in AUDIO_EXTENSIONS:
            succeeded.append({
                "index": (expected or {}).get("index") or index,
                "label": label,
                "file": os.path.basename(audio_path),
            })
        else:
            failed.append(report_item(
                expected,
                index,
                label,
                reason=first_text(entry.get("reason"), entry.get("availability"), "결과 파일 없음"),
            ))
        mark_expected(matched_expected, expected)
    for item in expected_items:
        marker = expected_marker(item)
        if marker and marker in matched_expected:
            continue
        failed.append(report_item(
            item,
            item.get("index"),
            item.get("label"),
            reason="추출 결과 없음",
        ))
    if not expected_items:
        missing_count = max(0, total - len(entries))
        if missing_count:
            failed.append({
                "index": None,
                "label": f"정보를 받지 못한 항목 {missing_count}개",
                "reason": "비공개 또는 삭제되어 yt-dlp가 항목 정보를 반환하지 않음",
            })
    report = {
        "kind": "playlist",
        "total": total,
        "succeeded": sorted(succeeded, key=report_sort_key),
        "failed": sorted(failed, key=report_sort_key),
    }
    try:
        with open(os.path.join(workspace, EXTRACTION_REPORT_NAME), "w", encoding="utf-8") as file:
            json.dump(report, file, ensure_ascii=False)
    except Exception as error:
        logger.warning(f"플레이리스트 결과 리포트 작성 실패: {error}")


def playlist_item_index(info, fallback):
    return as_int(info.get("playlist_index") or info.get("playlist_autonumber") or info.get("track_number")) or fallback


def match_expected_playlist_item(entry, index, expected_by_key, expected_by_index):
    for key in playlist_item_keys(entry):
        if key in expected_by_key:
            return expected_by_key[key]
    return expected_by_index.get(index)


def playlist_item_keys(info):
    if not isinstance(info, dict):
        return set()
    keys = set()
    for key in ("id", "url", "webpage_url", "original_url"):
        value = first_text(info.get(key))
        if not value:
            continue
        keys.add(value.strip().casefold())
        video_id = youtube_video_id(value)
        if video_id:
            keys.add(video_id.casefold())
    return keys


def youtube_video_id(value):
    text = str(value or "")
    match = re.search(r"(?:[?&]v=|youtu\.be/|/shorts/)([A-Za-z0-9_-]{6,})", text)
    if match:
        return match.group(1)
    if re.fullmatch(r"[A-Za-z0-9_-]{6,}", text):
        return text
    return None


def report_item(expected, fallback_index, fallback_label, reason=None):
    expected = expected if isinstance(expected, dict) else {}
    return {
        "index": expected.get("index") or fallback_index,
        "label": expected.get("label") or fallback_label or "알 수 없는 항목",
        "reason": reason,
    }


def expected_marker(item):
    if not isinstance(item, dict):
        return None
    index = item.get("index")
    if index:
        return f"index:{index}"
    keys = sorted(item.get("keys") or [])
    if keys:
        return f"key:{keys[0]}"
    return None


def mark_expected(marked, item):
    marker = expected_marker(item)
    if marker:
        marked.add(marker)


def report_sort_key(item):
    index = as_int((item or {}).get("index"))
    return (index is None, index or 0, str((item or {}).get("label") or ""))


def report_entry_label(info, index=None):
    title = first_text(metadata_title(info), info.get("fulltitle"), info.get("title"), info.get("webpage_url"), info.get("id"))
    artist = metadata_artist(info)
    if artist and title and comparable_text(artist) not in comparable_text(title):
        return f"{artist} - {title}"
    if title:
        return title
    if index:
        return f"{index}번째 항목"
    return "알 수 없는 항목"


def rename_metadata_matched_audio_file(workspace, info, logger, ydl=None):
    if not isinstance(info, dict) or not isinstance(info.get("__ytet_metadata"), dict):
        return
    artist = metadata_override(info, "artist")
    title = metadata_override(info, "title")
    if not artist or not title:
        return
    audio_path = audio_path_for_info(workspace, info, ydl)
    if not audio_path:
        return
    parent = os.path.dirname(audio_path)
    _stem, extension = os.path.splitext(audio_path)
    safe_name = sanitize_filename(f"{artist} - {title}")
    target = os.path.join(parent, safe_name + extension)
    if os.path.abspath(audio_path) == os.path.abspath(target):
        return
    target = unique_workspace_file_path(target)
    try:
        os.replace(audio_path, target)
        update_audio_info_path(info, audio_path, target)
        logger.info(f"MusicBrainz 파일명 적용: {os.path.basename(target)}")
    except Exception as error:
        logger.warning(f"MusicBrainz 파일명 적용 실패: {error}")


def update_audio_info_path(info, old_path, new_path):
    for key in ("filepath", "_filename", "filename"):
        if info.get(key) == old_path:
            info[key] = new_path
    requested_downloads = info.get("requested_downloads")
    if not isinstance(requested_downloads, list):
        return
    for item in requested_downloads:
        if not isinstance(item, dict):
            continue
        for key in ("filepath", "_filename", "filename"):
            if item.get(key) == old_path:
                item[key] = new_path


def relocate_playlist_folder_for_metadata(workspace, info, logger):
    folder_name = matched_playlist_folder_name(info)
    if not folder_name:
        return
    source = downloaded_playlist_folder(workspace)
    if not source:
        return
    desired = os.path.join(workspace, folder_name)
    if os.path.abspath(source) == os.path.abspath(desired):
        return
    target = unique_workspace_path(desired)
    try:
        os.replace(source, target)
        logger.info(f"MusicBrainz 앨범 폴더명 적용: {os.path.basename(target)}")
    except Exception as error:
        logger.warning(f"앨범 폴더명 적용 실패: {error}")


def matched_playlist_folder_name(info):
    entries = [entry for entry in (info.get("entries") if isinstance(info, dict) else None) or [] if isinstance(entry, dict)]
    if not entries:
        return None
    album, album_count = most_common_metadata_text(entries, "album")
    artist, artist_count = most_common_album_artist(entries)
    minimum = 1 if len(entries) == 1 else max(2, (len(entries) + 1) // 2)
    if not album or not artist or album_count < minimum or artist_count < minimum:
        return None
    return sanitize_filename(f"{artist} - {album}")


def most_common_album_artist(entries):
    values = []
    for entry in entries:
        values.append(metadata_override(entry, "album_artist") or metadata_override(entry, "artist"))
    return most_common_text(values)


def most_common_metadata_text(entries, key):
    return most_common_text(metadata_override(entry, key) for entry in entries)


def most_common_text(values):
    counts = {}
    labels = {}
    for value in values:
        label = normalize_text(value)
        key = comparable_text(label)
        if not key:
            continue
        counts[key] = counts.get(key, 0) + 1
        labels.setdefault(key, label)
    if not counts:
        return None, 0
    key = max(counts, key=counts.get)
    return labels[key], counts[key]


def downloaded_playlist_folder(workspace):
    try:
        candidates = [
            os.path.join(workspace, name)
            for name in sorted(os.listdir(workspace))
            if os.path.isdir(os.path.join(workspace, name)) and contains_audio_file(os.path.join(workspace, name))
        ]
    except OSError:
        return None
    return candidates[0] if len(candidates) == 1 else None


def contains_audio_file(path):
    for root, _dirs, names in os.walk(path):
        for name in names:
            if os.path.splitext(name)[1].lower() in AUDIO_EXTENSIONS:
                return True
    return False


def unique_workspace_path(path):
    if not os.path.exists(path):
        return path
    parent = os.path.dirname(path)
    stem = os.path.basename(path)
    for index in range(2, 1000):
        candidate = os.path.join(parent, f"{stem} ({index})")
        if not os.path.exists(candidate):
            return candidate
    return os.path.join(parent, f"{stem} ({int(time.time())})")


def unique_workspace_file_path(path):
    if not os.path.exists(path):
        return path
    parent = os.path.dirname(path)
    stem, extension = os.path.splitext(os.path.basename(path))
    for index in range(2, 1000):
        candidate = os.path.join(parent, f"{stem} ({index}){extension}")
        if not os.path.exists(candidate):
            return candidate
    return os.path.join(parent, f"{stem} ({int(time.time())}){extension}")


def write_mp4_metadata(audio, info, album_fallback=None):
    if audio.tags is None:
        audio.add_tags()
    tags = audio.tags
    set_mp4_text(tags, "\xa9nam", metadata_title(info))
    set_mp4_text(tags, "\xa9ART", metadata_artist(info))
    set_mp4_text(tags, "aART", metadata_album_artist(info))
    set_mp4_text(tags, "\xa9alb", metadata_album(info, album_fallback))
    set_mp4_text(tags, "\xa9day", metadata_date(info))
    track = metadata_track_number(info)
    if track:
        tags["trkn"] = [track]


def set_mp4_text(tags, key, value):
    if value:
        tags[key] = [value]


def metadata_title(info):
    if not isinstance(info, dict):
        return None
    if is_single_video_playlist(info):
        return first_text(clean_single_video_playlist_title(info.get("title")), info.get("title"), info.get("id"))
    return first_text(metadata_override(info, "title"), info.get("track"), info.get("title"), info.get("id"))


def metadata_artist(info):
    if not isinstance(info, dict):
        return None
    return normalize_artist_text(first_text(metadata_override(info, "artist"), info.get("artist"), info.get("creator"), info.get("uploader"), info.get("channel")))


def metadata_album_artist(info):
    if not isinstance(info, dict):
        return None
    return normalize_artist_text(first_text(
        metadata_override(info, "album_artist"),
        info.get("album_artist"),
        metadata_override(info, "artist"),
        info.get("artist"),
        info.get("creator"),
        info.get("uploader"),
        info.get("channel"),
    ))


def metadata_album(info, fallback=None):
    if not isinstance(info, dict):
        return fallback
    if is_single_video_playlist(info):
        return first_text(clean_single_video_playlist_title(info.get("title")), fallback)
    return first_text(metadata_override(info, "album"), info.get("album"), info.get("playlist"), info.get("playlist_title"), fallback)


def metadata_date(info):
    if not isinstance(info, dict):
        return None
    override = metadata_override(info, "date")
    if override:
        return override
    release_year = first_text(info.get("release_year"))
    if release_year:
        return release_year
    upload_date = first_text(info.get("upload_date"))
    if upload_date and len(upload_date) >= 4:
        return upload_date[:4]
    return None


def metadata_track_number(info):
    if not isinstance(info, dict):
        return None
    override_index = as_int(metadata_override(info, "track_number"))
    if override_index:
        override_total = as_int(metadata_override(info, "track_total"))
        return (override_index, override_total if override_total and override_total > 0 else 0)
    index = as_int(info.get("playlist_index") or info.get("track_number"))
    total = as_int(info.get("n_entries") or info.get("playlist_count") or info.get("track_total"))
    if not index or index < 1:
        return None
    return (index, total if total and total > 0 else 0)


def metadata_override(info, key):
    metadata = info.get("__ytet_metadata") if isinstance(info, dict) else None
    if not isinstance(metadata, dict):
        return None
    return metadata.get(key)


def audio_path_for_info(workspace, info, ydl=None):
    if not isinstance(info, dict):
        return None

    requested_downloads = info.get("requested_downloads")
    if isinstance(requested_downloads, list):
        for item in requested_downloads:
            if not isinstance(item, dict):
                continue
            path = first_text(item.get("filepath"), item.get("_filename"), item.get("filename"))
            if is_existing_audio_path(path):
                return path

    path = first_text(info.get("filepath"), info.get("_filename"), info.get("filename"))
    if is_existing_audio_path(path):
        return path

    if ydl is not None:
        try:
            path = ydl.prepare_filename(info)
            if is_existing_audio_path(path):
                return path
        except Exception:
            return None
    return None


def is_existing_audio_path(path):
    if not path:
        return False
    return os.path.isfile(path) and os.path.splitext(path)[1].lower() in AUDIO_EXTENSIONS


def download_cover_image(info, workspace, logger):
    thumbnails = info.get("thumbnails") if isinstance(info, dict) else None
    for thumbnail in reversed(thumbnails or []):
        if not isinstance(thumbnail, dict) or not thumbnail.get("url"):
            continue
        try:
            request = Request(thumbnail["url"], headers={"User-Agent": "Mozilla/5.0"})
            with urlopen(request, timeout=20) as response:
                data = response.read(25 * 1024 * 1024)
                mime = supported_image_mime(data, response.headers.get_content_type())
                if not mime:
                    continue
                suffix = ".png" if mime == "image/png" else ".jpg"
                path = os.path.join(workspace, "cover" + suffix)
                with open(path, "wb") as file:
                    file.write(data)
                return {"path": path, "mime": mime, "data": data}
        except Exception as error:
            logger.warning(f"커버 이미지 다운로드 실패: {error}")
    return None


def supported_image_mime(data, content_type):
    if data.startswith(b"\xff\xd8\xff"):
        return "image/jpeg"
    if data.startswith(b"\x89PNG\r\n\x1a\n"):
        return "image/png"
    if content_type in {"image/jpeg", "image/png"}:
        return content_type
    return None


def find_audio_file(workspace):
    files = []
    for root, _dirs, names in os.walk(workspace):
        for name in names:
            if os.path.splitext(name)[1].lower() in AUDIO_EXTENSIONS:
                files.append(os.path.join(root, name))
    return sorted(files)[0] if files else None


def find_prefixed_file(workspace, name_prefix):
    files = []
    for root, _dirs, names in os.walk(workspace):
        for name in names:
            if name.startswith(name_prefix):
                files.append(os.path.join(root, name))
    return sorted(files)[0] if files else None


def find_subtitle_files(workspace):
    files = []
    for root, _dirs, names in os.walk(workspace):
        for name in names:
            if os.path.splitext(name)[1].lower() in {".srt", ".vtt"}:
                files.append(os.path.join(root, name))
    return sorted(files)


def progress_hook(progress_listener, cancel_checker=None):
    def hook(data):
        check_canceled(cancel_checker)
        status = data.get("status")
        if status == "downloading":
            notify(progress_listener, download_percent(data), "다운로드", download_message(data))
        elif status == "finished":
            filename = os.path.basename(str(data.get("filename") or ""))
            notify(progress_listener, cleanup_percent(data), "정리", playlist_message(data, filename or "다운로드 완료"))

    return hook


def check_canceled(cancel_checker):
    if cancel_checker is None:
        return
    try:
        if bool(cancel_checker.isCanceled()):
            raise YtetCancellationError("추출이 취소되었습니다.")
    except YtetCancellationError:
        raise
    except Exception:
        return


def is_cancel_message(message):
    return "취소" in str(message or "")


def download_percent(data):
    fraction = download_fraction(data)
    playlist_progress = playlist_percent(data, fraction if fraction is not None else 0)
    if playlist_progress is not None:
        return playlist_progress
    if fraction is not None:
        return min(90, max(6, int(round(6 + fraction * 84))))
    return 20


def cleanup_percent(data):
    playlist_progress = playlist_percent(data, 1)
    return playlist_progress if playlist_progress is not None else 88


def download_fraction(data):
    total = data.get("total_bytes") or data.get("total_bytes_estimate")
    downloaded = data.get("downloaded_bytes")
    if total and downloaded:
        return min(1, max(0, downloaded / total))
    return None


def playlist_percent(data, item_fraction):
    info = data.get("info_dict") if isinstance(data, dict) else None
    if not isinstance(info, dict):
        return None
    index = as_int(info.get("playlist_index") or info.get("playlist_autonumber"))
    total = as_int(info.get("n_entries") or info.get("playlist_count"))
    if not index or not total or index < 1 or total < 1:
        return None
    bounded_index = min(index, total)
    progress = ((bounded_index - 1) + min(1, max(0, item_fraction))) / total
    return min(90, max(6, int(round(6 + progress * 84))))


def download_message(data):
    filename = os.path.basename(str(data.get("filename") or ""))
    total = data.get("total_bytes") or data.get("total_bytes_estimate")
    downloaded = data.get("downloaded_bytes")
    if total and downloaded:
        size = f"{format_bytes(downloaded)} / {format_bytes(total)}"
        return playlist_message(data, f"{filename} {size}".strip())
    return playlist_message(data, filename or "다운로드 중")


def playlist_message(data, message):
    info = data.get("info_dict") if isinstance(data, dict) else None
    if not isinstance(info, dict):
        return message
    index = as_int(info.get("playlist_index") or info.get("playlist_autonumber"))
    total = as_int(info.get("n_entries") or info.get("playlist_count"))
    if index and total:
        return f"{index}/{total} {message}"
    if index:
        return f"{index}번째 {message}"
    return message


def notify(listener, percent, stage, message):
    if listener is None:
        return
    try:
        listener.onProgress(int(percent), stage, str(message or "")[:300])
    except Exception:
        pass


def format_bytes(value):
    try:
        size = float(value)
    except (TypeError, ValueError):
        return "알 수 없음"
    units = ["B", "KB", "MB", "GB"]
    for unit in units:
        if size < 1024 or unit == units[-1]:
            return f"{int(size)} {unit}" if unit == "B" else f"{size:.1f} {unit}"
        size /= 1024
    return f"{size:.1f} GB"


def clean_error(message, logger):
    parts = [strip_ansi(str(message or "").strip()), logger.tail()]
    text = "\n".join(part for part in parts if part).strip()
    return text or "yt-dlp 실행 중 오류가 발생했습니다."


def strip_ansi(text):
    return re.sub(r"\x1b\[[0-9;]*m", "", text)
