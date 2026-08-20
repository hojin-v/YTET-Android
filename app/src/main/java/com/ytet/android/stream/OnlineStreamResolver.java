package com.ytet.android.stream;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.ytet.android.extract.YtDlpUpdater;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OnlineStreamResolver {
    private static final long CACHE_TTL_MS = 12L * 60L * 1000L;
    private static final int MAX_CACHE_SIZE = 96;
    private static final Map<String, CacheEntry> RESOLVED_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Object> IN_FLIGHT_LOCKS = new ConcurrentHashMap<>();

    private OnlineStreamResolver() {
    }

    public static ResolvedStream resolve(Context context, String videoUrl) throws Exception {
        String url = cleanUrl(videoUrl);
        if (url.isEmpty()) {
            throw new IllegalArgumentException("재생할 YouTube URL이 없습니다.");
        }
        ResolvedStream cached = cachedStream(url);
        if (cached != null) {
            return cached;
        }
        Object lock = IN_FLIGHT_LOCKS.computeIfAbsent(url, ignored -> new Object());
        try {
            synchronized (lock) {
                cached = cachedStream(url);
                if (cached != null) {
                    return cached;
                }
                ResolvedStream resolved = resolveUncached(context, url);
                if (!resolved.streamUrl().trim().isEmpty()) {
                    remember(url, resolved);
                }
                return resolved;
            }
        } finally {
            IN_FLIGHT_LOCKS.remove(url, lock);
        }
    }

    private static ResolvedStream resolveUncached(Context context, String videoUrl) throws Exception {
        ensurePython(context);
        PyObject module = Python.getInstance().getModule("ytet_ydl");
        String json = module.callAttr("resolve_stream", videoUrl).toString();
        JSONObject object = new JSONObject(json);
        return new ResolvedStream(
                object.optString("stream_url", ""),
                object.optString("title", ""),
                object.optString("channel_title", ""),
                object.optString("thumbnail", ""),
                Math.max(0L, object.optLong("duration", 0L)) * 1000L
        );
    }

    private static ResolvedStream cachedStream(String videoUrl) {
        CacheEntry entry = RESOLVED_CACHE.get(videoUrl);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtMs < System.currentTimeMillis()) {
            RESOLVED_CACHE.remove(videoUrl);
            return null;
        }
        return entry.stream;
    }

    private static void remember(String videoUrl, ResolvedStream stream) {
        if (RESOLVED_CACHE.size() >= MAX_CACHE_SIZE) {
            RESOLVED_CACHE.clear();
        }
        RESOLVED_CACHE.put(videoUrl, new CacheEntry(stream, System.currentTimeMillis() + CACHE_TTL_MS));
    }

    private static void ensurePython(Context context) {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
        }
        YtDlpUpdater.applyRuntimeOverride(context);
        YtDlpUpdater.scheduleBackgroundUpdate(context);
    }

    private static String cleanUrl(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class CacheEntry {
        private final ResolvedStream stream;
        private final long expiresAtMs;

        private CacheEntry(ResolvedStream stream, long expiresAtMs) {
            this.stream = stream;
            this.expiresAtMs = expiresAtMs;
        }
    }

    public static final class ResolvedStream {
        private final String streamUrl;
        private final String title;
        private final String channelTitle;
        private final String thumbnailUrl;
        private final long durationMs;

        ResolvedStream(String streamUrl, String title, String channelTitle, String thumbnailUrl, long durationMs) {
            this.streamUrl = clean(streamUrl);
            this.title = clean(title);
            this.channelTitle = clean(channelTitle);
            this.thumbnailUrl = clean(thumbnailUrl);
            this.durationMs = Math.max(0L, durationMs);
        }

        public String streamUrl() {
            return streamUrl;
        }

        public String title() {
            return title;
        }

        public String channelTitle() {
            return channelTitle;
        }

        public String thumbnailUrl() {
            return thumbnailUrl;
        }

        public long durationMs() {
            return durationMs;
        }

        private static String clean(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
