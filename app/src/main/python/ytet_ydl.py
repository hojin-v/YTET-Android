import os
import re

from yt_dlp import YoutubeDL
from yt_dlp.utils import DownloadError


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


def extract(workspace, url, media_type, option, include_subtitles, include_multi_audio, progress_listener):
    os.makedirs(workspace, exist_ok=True)
    logger = YtetLogger()
    options = build_options(
        workspace,
        str(media_type or "audio"),
        str(option or ""),
        bool(include_subtitles),
        bool(include_multi_audio),
        progress_listener,
        logger,
    )

    try:
        with YoutubeDL(options) as ydl:
            result = ydl.download([url])
    except YtetExtractionError:
        raise
    except DownloadError as error:
        raise YtetExtractionError(clean_error(str(error), logger)) from error
    except Exception as error:
        raise YtetExtractionError(clean_error(str(error), logger)) from error

    if result != 0:
        raise YtetExtractionError(clean_error("yt-dlp가 실패했습니다.", logger))


def build_options(workspace, media_type, option, include_subtitles, include_multi_audio, progress_listener, logger):
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
        if include_multi_audio:
            raise YtetExtractionError("다중 오디오는 현재 Android APK에 FFmpeg 병합 런타임이 없어 사용할 수 없습니다.")
        options["format"] = video_format_selector(option)
        if include_subtitles:
            options["writesubtitles"] = True
            options["subtitleslangs"] = ["ko", "ko-KR", "en", "en-US", "en-GB"]
            options["subtitlesformat"] = "srt/vtt/best"
        return options

    if option == "mp3":
        raise YtetExtractionError("MP3 변환은 현재 Android APK에 FFmpeg 변환 런타임이 없어 사용할 수 없습니다. M4A 또는 Original Opus를 선택하세요.")
    if option == "original":
        options["format"] = "ba/best"
    else:
        options["format"] = "ba[ext=m4a]"
    return options


def video_format_selector(option):
    if option in {"1080", "1080p"}:
        return "b[height<=1080][ext=mp4]"
    if option in {"720", "720p"}:
        return "b[height<=720][ext=mp4]"
    if option in {"480", "480p"}:
        return "b[height<=480][ext=mp4]"
    return "b[ext=mp4]/best"


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
