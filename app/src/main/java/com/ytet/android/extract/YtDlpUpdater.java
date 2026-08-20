package com.ytet.android.extract;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

/**
 * yt-dlp 런타임 자동 업데이트를 관리한다.
 *
 * <p>앱 내부 저장소에 최신 yt-dlp를 내려받아 번들 버전을 오버라이드한다.
 * 모든 작업은 best-effort이며 실패해도 앱 동작에 영향을 주지 않는다.</p>
 */
public final class YtDlpUpdater {
    private static final String MODULE = "ytet_ydl_updater";
    private static volatile boolean overrideApplied = false;

    private YtDlpUpdater() {
    }

    /**
     * 런타임에 설치된 yt-dlp가 있으면 sys.path에 우선 삽입한다.
     * Python 초기화 직후, yt-dlp를 사용하기 전에 한 번 호출한다.
     */
    public static void applyRuntimeOverride(Context context) {
        if (overrideApplied) {
            return;
        }
        try {
            if (!Python.isStarted()) {
                return;
            }
            PyObject module = Python.getInstance().getModule(MODULE);
            module.callAttr("apply_runtime_override",
                    context.getFilesDir().getAbsolutePath());
        } catch (Exception ignored) {
        }
        overrideApplied = true;
    }

    /**
     * PyPI에서 최신 yt-dlp 버전을 확인하고 필요하면 다운로드·설치한다.
     * 네트워크 I/O가 포함되므로 반드시 백그라운드 스레드에서 호출한다.
     *
     * @return 결과 JSON 문자열 또는 실패 시 null
     */
    public static String checkForUpdate(Context context) {
        try {
            if (!Python.isStarted()) {
                return null;
            }
            PyObject module = Python.getInstance().getModule(MODULE);
            PyObject result = module.callAttr("check_and_apply_update",
                    context.getFilesDir().getAbsolutePath());
            return result != null ? result.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 현재 번들·런타임·활성 yt-dlp 버전 정보를 반환한다.
     *
     * @return 버전 정보 JSON 문자열 또는 실패 시 null
     */
    public static String installedVersions(Context context) {
        try {
            if (!Python.isStarted()) {
                return null;
            }
            PyObject module = Python.getInstance().getModule(MODULE);
            PyObject result = module.callAttr("installed_versions",
                    context.getFilesDir().getAbsolutePath());
            return result != null ? result.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final long UPDATE_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L;
    private static volatile long lastUpdateCheckTimeMs = 0;

    /**
     * 백그라운드에서 yt-dlp 업데이트를 확인하고 설치한다.
     * 최소 6시간 간격으로 1회만 실행된다.
     */
    public static void scheduleBackgroundUpdate(Context context) {
        long now = System.currentTimeMillis();
        if (now - lastUpdateCheckTimeMs < UPDATE_CHECK_INTERVAL_MS) {
            return;
        }
        lastUpdateCheckTimeMs = now;
        Context appContext = context.getApplicationContext();
        new Thread(() -> checkForUpdate(appContext), "yt-dlp-updater").start();
    }
}
