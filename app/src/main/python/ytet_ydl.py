import os
import json
import re
from urllib.request import Request, urlopen

from yt_dlp import YoutubeDL
from yt_dlp.utils import DownloadError


SUBTITLE_LANGUAGES = ["ko", "ko-KR", "en", "en-US", "en-GB"]
AUDIO_EXTENSIONS = {".m4a", ".aac", ".flac", ".mp3", ".opus", ".ogg", ".wav", ".webm"}
DEFAULT_ANDROID_SDK = 36


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


def extract(workspace, url, media_type, option, include_subtitles, progress_listener, android_sdk=DEFAULT_ANDROID_SDK):
    os.makedirs(workspace, exist_ok=True)
    logger = YtetLogger()
    media_type = str(media_type or "audio")
    android_sdk = as_int(android_sdk) or DEFAULT_ANDROID_SDK

    if media_type == "video":
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
    )

    try:
        with YoutubeDL(options) as ydl:
            info = ydl.extract_info(url, download=True)
    except YtetExtractionError:
        raise
    except DownloadError as error:
        raise YtetExtractionError(clean_error(str(error), logger)) from error
    except Exception as error:
        raise YtetExtractionError(clean_error(str(error), logger)) from error

    embed_audio_cover(workspace, info, logger)


def build_options(workspace, media_type, option, include_subtitles, progress_listener, logger):
    options = {
        "cachedir": False,
        "logger": logger,
        "no_warnings": False,
        "noplaylist": True,
        "outtmpl": os.path.join(workspace, "%(uploader)s - %(title)s.%(ext)s"),
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
            write_mux_manifest(workspace, info, plan)
        else:
            notify(progress_listener, 12, "다운로드", "단일 파일 영상 다운로드 중")
            download_one(url, workspace, plan["format"], final_outtmpl(info), logger, progress_listener)

        if include_subtitles:
            notify(progress_listener, 84, "자막", "자막 다운로드 중")
            download_subtitles(url, workspace, info, logger, progress_listener)
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


def embed_audio_cover(workspace, info, logger):
    audio_path = find_audio_file(workspace)
    if not audio_path or os.path.splitext(audio_path)[1].lower() != ".m4a":
        return
    cover = download_cover_image(info, workspace, logger)
    if not cover:
        return
    try:
        from mutagen.mp4 import MP4, MP4Cover
        audio = MP4(audio_path)
        if audio.tags is None:
            audio.add_tags()
        image_format = MP4Cover.FORMAT_PNG if cover["mime"] == "image/png" else MP4Cover.FORMAT_JPEG
        audio.tags["covr"] = [MP4Cover(cover["data"], imageformat=image_format)]
        audio.save()
    except Exception as error:
        logger.warning(f"커버 이미지 임베딩 실패: {error}")


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


def progress_hook(progress_listener):
    def hook(data):
        status = data.get("status")
        if status == "downloading":
            notify(progress_listener, download_percent(data), "다운로드", download_message(data))
        elif status == "finished":
            filename = os.path.basename(str(data.get("filename") or ""))
            notify(progress_listener, 88, "정리", filename or "다운로드 완료")

    return hook


def download_percent(data):
    total = data.get("total_bytes") or data.get("total_bytes_estimate")
    downloaded = data.get("downloaded_bytes")
    if total and downloaded:
        return min(90, max(6, int(round(6 + (downloaded / total) * 84))))
    return 20


def download_message(data):
    filename = os.path.basename(str(data.get("filename") or ""))
    total = data.get("total_bytes") or data.get("total_bytes_estimate")
    downloaded = data.get("downloaded_bytes")
    if total and downloaded:
        size = f"{format_bytes(downloaded)} / {format_bytes(total)}"
        return f"{filename} {size}".strip()
    return filename or "다운로드 중"


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
