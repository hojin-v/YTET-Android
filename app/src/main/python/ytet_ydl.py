import os
import json
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
MUSICBRAINZ_API_ROOT = "https://musicbrainz.org/ws/2"
MUSICBRAINZ_USER_AGENT = "YTET-Android/0.1 (https://github.com/hojin/youtube-audio-extractor-android)"
MUSICBRAINZ_MIN_INTERVAL = 1.05


class YtetExtractionError(Exception):
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
    )

    try:
        with YoutubeDL(options) as ydl:
            info = ydl.extract_info(url, download=True)
            mark_single_video_playlist(info)
            if enhance_metadata and not is_single_video_playlist(info):
                info = enhance_music_metadata(info, progress_listener, logger)
            embed_audio_covers(workspace, info, logger, ydl)
            if enhance_metadata:
                rename_metadata_matched_audio_files(workspace, info, logger, ydl)
            if include_playlist:
                relocate_playlist_folder_for_metadata(workspace, info, logger)
    except YtetExtractionError:
        raise
    except DownloadError as error:
        raise YtetExtractionError(clean_error(str(error), logger)) from error
    except Exception as error:
        raise YtetExtractionError(clean_error(str(error), logger)) from error

def build_options(
    workspace,
    media_type,
    option,
    include_subtitles,
    progress_listener,
    logger,
    include_playlist=False,
):
    options = {
        "cachedir": False,
        "logger": logger,
        "no_warnings": False,
        "noplaylist": not bool(include_playlist),
        "outtmpl": output_template(workspace, include_playlist),
        "overwrites": True,
        "progress_hooks": [progress_hook(progress_listener)],
        "quiet": True,
    }

    if media_type == "video":
        raise YtetExtractionError("영상은 Android 병합 경로로 처리합니다.")

    if option == "mp3":
        raise YtetExtractionError("MP3 변환은 현재 Android APK에 FFmpeg 변환 런타임이 없어 사용할 수 없습니다. M4A 또는 Original Opus를 선택하세요.")
    if option == "original":
        options["format"] = "ba/best"
    else:
        options["format"] = "ba[ext=m4a]/ba/best"
    return options


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
):
    try:
        notify(progress_listener, 8, "분석", "영상 형식 확인 중")
        with YoutubeDL(base_options(logger, progress_listener)) as ydl:
            info = ydl.extract_info(url, download=False)

        plan = video_track_plan(info, option, android_sdk)
        if plan["mode"] == "mux":
            notify(progress_listener, 12, "다운로드", "고화질 영상 트랙 다운로드 중")
            download_one(url, workspace, plan["video"]["format_id"], "video-track.%(ext)s", logger, progress_listener)
            notify(progress_listener, 52, "다운로드", "오디오 트랙 다운로드 중")
            download_one(url, workspace, plan["audio"]["format_id"], "audio-track.%(ext)s", logger, progress_listener)
        else:
            notify(progress_listener, 12, "다운로드", "단일 파일 영상 다운로드 중")
            outtmpl = "video-track.%(ext)s" if include_subtitles else final_outtmpl(info)
            download_one(url, workspace, plan["format"], outtmpl, logger, progress_listener)

        if include_subtitles:
            notify(progress_listener, 84, "자막", "자막 다운로드 중")
            download_subtitles(url, workspace, info, logger, progress_listener)
        if plan["mode"] == "mux":
            write_mux_manifest(workspace, info, plan)
        elif include_subtitles:
            write_single_video_manifest(workspace, info)
    except YtetExtractionError:
        raise
    except DownloadError as error:
        raise YtetExtractionError(clean_error(str(error), logger)) from error
    except Exception as error:
        raise YtetExtractionError(clean_error(str(error), logger)) from error


def base_options(logger, progress_listener):
    return {
        "cachedir": False,
        "logger": logger,
        "no_warnings": False,
        "noplaylist": True,
        "overwrites": True,
        "progress_hooks": [progress_hook(progress_listener)],
        "quiet": True,
    }


def download_one(url, workspace, format_id, outtmpl, logger, progress_listener):
    options = {
        **base_options(logger, progress_listener),
        "format": format_id,
        "outtmpl": os.path.join(workspace, outtmpl),
    }
    with YoutubeDL(options) as ydl:
        ydl.download([url])


def download_subtitles(url, workspace, info, logger, progress_listener):
    options = {
        **base_options(logger, progress_listener),
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
        return "YTET"
    left = first_text(info.get("uploader"), info.get("channel"), info.get("artist"))
    title = first_text(info.get("title"), info.get("track"), info.get("id"), "YTET")
    return sanitize_filename(f"{left} - {title}" if left and left != title else title)


def sanitize_filename(name):
    text = re.sub(r'[\\/:*?"<>|\r\n]+', " ", str(name or "YTET")).strip()
    text = re.sub(r"\s+", " ", text)
    return text[:160].rstrip(" .") or "YTET"


def first_text(*values):
    for value in values:
        if value:
            return str(value).strip()
    return None


def enhance_music_metadata(info, progress_listener, logger):
    if not isinstance(info, dict):
        return info
    if is_single_video_playlist(info):
        logger.info("MusicBrainz 보정 건너뜀: 긴 단일 플레이리스트 영상")
        return info
    try:
        notify(progress_listener, 91, "메타데이터", "MusicBrainz 앨범 정보 검색 중")
        client = MusicBrainzClient(logger)
        entries = [entry for entry in info.get("entries") or [] if isinstance(entry, dict)]
        if entries:
            enhance_playlist_metadata(info, entries, client, logger, progress_listener)
        else:
            metadata = match_recording_metadata(info, client)
            if metadata:
                apply_metadata_override(info, metadata)
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


def enhance_playlist_metadata(info, entries, client, logger, progress_listener=None):
    artist_candidate, album_candidate = playlist_artist_album_candidates(info)
    if album_candidate:
        notify(progress_listener, 91, "메타데이터", "MusicBrainz 앨범 후보 검색 중")
    release = match_release_for_playlist(entries, artist_candidate, album_candidate, client)
    if release:
        matched = apply_release_metadata(entries, release)
        logger.info(f"MusicBrainz release matched {matched}/{len(entries)} tracks")

    total = len(entries)
    for index, entry in enumerate(entries, start=1):
        if entry.get("__ytet_metadata"):
            continue
        notify(progress_listener, 91, "메타데이터", f"MusicBrainz 검색 중 {index}/{total}")
        metadata = match_recording_metadata(entry, client, artist_candidate=artist_candidate, album_candidate=album_candidate)
        if metadata:
            apply_metadata_override(entry, metadata)
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


def match_release_for_playlist(entries, artist_candidate, album_candidate, client):
    if not album_candidate:
        return None
    clauses = [f'release:"{mb_escape(album_candidate)}"']
    if artist_candidate:
        clauses.append(f'artist:"{mb_escape(artist_candidate)}"')
    results = client.search("release", " AND ".join(clauses), limit=10).get("releases") or []
    minimum_matches = max(2, int(round(len(entries) * 0.5))) if len(entries) > 1 else 1
    best_release = None
    best_rank = None
    for release_candidate in results:
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


def match_recording_metadata(info, client, artist_candidate=None, album_candidate=None):
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
    if not results and artist and len(titles) > 1:
        results = client.search("recording", f'recording:"{mb_escape(titles[1])}" AND artistname:"{mb_escape(artist)}"', limit=5).get("recordings") or []
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
    text = re.sub(r"(?i)\s+w\.?\s+", " with ", text)
    text = re.sub(r"(?i)\s+w\s*/\s*", " with ", text)
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
    if not safe_name:
        return
    target = os.path.join(parent, safe_name + extension)
    if os.path.abspath(audio_path) == os.path.abspath(target):
        return
    target = unique_workspace_path(target)
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


def progress_hook(progress_listener):
    def hook(data):
        status = data.get("status")
        if status == "downloading":
            notify(progress_listener, download_percent(data), "다운로드", download_message(data))
        elif status == "finished":
            filename = os.path.basename(str(data.get("filename") or ""))
            notify(progress_listener, cleanup_percent(data), "정리", playlist_message(data, filename or "다운로드 완료"))

    return hook


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
