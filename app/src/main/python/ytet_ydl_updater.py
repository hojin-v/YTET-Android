"""yt-dlp 런타임 자동 업데이트 모듈.

앱에 번들된 yt-dlp 버전과 무관하게 PyPI에서 최신 yt-dlp를 내려받아
앱 내부 저장소에 설치한 뒤 sys.path에 우선 삽입하여 오버라이드한다.
"""

import json
import os
import re
import shutil
import sys
import zipfile
from datetime import datetime, timezone
from urllib.request import Request, urlopen


RUNTIME_DIR_NAME = "yt-dlp-runtime"
VERSION_FILE = "version.json"
PYPI_URL = "https://pypi.org/pypi/yt-dlp/json"
USER_AGENT = "RabbYT-Android/0.1"
VERSION_CHECK_TIMEOUT = 10
DOWNLOAD_TIMEOUT = 120


def _version_key(v):
    if not v:
        return ()
    return tuple(int(x) for x in re.findall(r"\d+", str(v)))


def _is_newer(a, b):
    return _version_key(a) > _version_key(b)


def _newer(a, b):
    if a and b:
        return a if _version_key(a) >= _version_key(b) else b
    return a or b


def apply_runtime_override(files_dir):
    """번들 버전보다 새로운 런타임 yt-dlp가 있으면 sys.path에 우선 삽입한다.

    yt-dlp가 아직 임포트되기 전에 호출해야 한다.
    이미 임포트된 뒤에 호출되면 기존 모듈을 언로드하고 새 경로에서 다시 로드한다.

    반환: 오버라이드를 적용했으면 True, 그렇지 않으면 False.
    """
    try:
        runtime_path = os.path.join(str(files_dir), RUNTIME_DIR_NAME)
        runtime_version = _read_runtime_version(runtime_path)
        if not runtime_version:
            return False

        bundled_version = _bundled_version_safe()
        if bundled_version and not _is_newer(runtime_version, bundled_version):
            return False

        if runtime_path not in sys.path:
            sys.path.insert(0, runtime_path)

        _invalidate_yt_dlp_modules(runtime_path)
        return True
    except Exception:
        return False


def check_and_apply_update(files_dir):
    """PyPI에서 최신 yt-dlp 버전을 확인하고 필요하면 다운로드·설치한다.

    반환: JSON 문자열 {"status", "bundled_version", "runtime_version", "message"}
    """
    try:
        files_dir = str(files_dir)
        runtime_path = os.path.join(files_dir, RUNTIME_DIR_NAME)
        bundled_version = _bundled_version_safe()
        runtime_version = _read_runtime_version(runtime_path)

        active_version = _newer(bundled_version, runtime_version) or bundled_version

        latest_version, wheel_url = _fetch_latest_wheel_info()
        if not latest_version:
            return _result("error", bundled_version, runtime_version, "PyPI 버전 확인 실패")

        if active_version and not _is_newer(latest_version, active_version):
            return _result("current", bundled_version, runtime_version,
                           f"이미 최신 버전: {active_version}")

        if not wheel_url:
            return _result("error", bundled_version, runtime_version, "다운로드 가능한 wheel 없음")

        _download_and_install(files_dir, runtime_path, wheel_url, latest_version)
        return _result("updated", bundled_version, latest_version,
                        f"yt-dlp {latest_version} 설치 완료")

    except Exception as error:
        return _result("error", _bundled_version_safe(), None, str(error))


def installed_versions(files_dir):
    """현재 번들·런타임·활성 yt-dlp 버전 정보를 반환한다."""
    try:
        files_dir = str(files_dir)
        runtime_path = os.path.join(files_dir, RUNTIME_DIR_NAME)
        bundled = _bundled_version_safe()
        runtime = _read_runtime_version(runtime_path)
        active = _newer(bundled, runtime) or bundled or "unknown"
        return json.dumps({"bundled": bundled, "runtime": runtime, "active": active},
                          ensure_ascii=False)
    except Exception as error:
        return json.dumps({"error": str(error)}, ensure_ascii=False)


# ---------------------------------------------------------------------------
# 내부 헬퍼
# ---------------------------------------------------------------------------

def _bundled_version_safe():
    try:
        mod = sys.modules.get("yt_dlp")
        if mod and hasattr(mod, "version") and hasattr(mod.version, "__version__"):
            return mod.version.__version__
        import yt_dlp
        if hasattr(yt_dlp, "version") and hasattr(yt_dlp.version, "__version__"):
            return yt_dlp.version.__version__
        import yt_dlp.version
        return yt_dlp.version.__version__
    except Exception:
        return None


def _read_runtime_version(runtime_path):
    version_path = os.path.join(runtime_path, VERSION_FILE)
    if not os.path.isfile(version_path):
        return None
    try:
        with open(version_path, "r", encoding="utf-8") as f:
            return json.load(f).get("runtime_version")
    except Exception:
        return None


def _newer(a, b):
    if a and b:
        return a if a >= b else b
    return a or b


def _fetch_latest_wheel_info():
    request = Request(PYPI_URL, headers={"User-Agent": USER_AGENT, "Accept": "application/json"})
    with urlopen(request, timeout=VERSION_CHECK_TIMEOUT) as response:
        data = json.loads(response.read().decode("utf-8"))
    version = data.get("info", {}).get("version")
    wheel_url = None
    for release_file in data.get("urls") or []:
        if str(release_file.get("filename", "")).endswith("py3-none-any.whl"):
            wheel_url = release_file.get("url")
            break
    if not wheel_url:
        for release_file in data.get("releases", {}).get(version, []):
            if str(release_file.get("filename", "")).endswith("py3-none-any.whl"):
                wheel_url = release_file.get("url")
                break
    return version, wheel_url


def _download_and_install(files_dir, runtime_path, wheel_url, version):
    temp_dir = os.path.join(files_dir, "yt-dlp-update-temp")
    try:
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
        os.makedirs(temp_dir, exist_ok=True)

        wheel_path = os.path.join(temp_dir, "yt_dlp.whl")
        request = Request(wheel_url, headers={"User-Agent": USER_AGENT})
        with urlopen(request, timeout=DOWNLOAD_TIMEOUT) as response:
            with open(wheel_path, "wb") as out:
                shutil.copyfileobj(response, out)

        with zipfile.ZipFile(wheel_path, "r") as zf:
            zf.extractall(temp_dir)

        os.remove(wheel_path)

        marker = {
            "runtime_version": version,
            "updated_at": datetime.now(timezone.utc).isoformat(),
        }
        with open(os.path.join(temp_dir, VERSION_FILE), "w", encoding="utf-8") as f:
            json.dump(marker, f, ensure_ascii=False)

        if os.path.exists(runtime_path):
            shutil.rmtree(runtime_path)
        os.rename(temp_dir, runtime_path)
    except Exception:
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir, ignore_errors=True)
        raise


def _invalidate_yt_dlp_modules(runtime_path):
    loaded = [m for m in sys.modules if m == "yt_dlp" or m.startswith("yt_dlp.")]
    if not loaded:
        return
    current_file = getattr(sys.modules.get("yt_dlp"), "__file__", "") or ""
    if current_file.startswith(runtime_path):
        return
    for module_name in loaded:
        del sys.modules[module_name]


def _result(status, bundled, runtime, message):
    return json.dumps({
        "status": status,
        "bundled_version": bundled,
        "runtime_version": runtime,
        "message": message,
    }, ensure_ascii=False)
