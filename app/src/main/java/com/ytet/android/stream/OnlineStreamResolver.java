package com.ytet.android.stream;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONObject;

public final class OnlineStreamResolver {
    private OnlineStreamResolver() {
    }

    public static ResolvedStream resolve(Context context, String videoUrl) throws Exception {
        ensurePython(context);
        PyObject module = Python.getInstance().getModule("ytet_ydl");
        String json = module.callAttr("resolve_stream", videoUrl == null ? "" : videoUrl.trim()).toString();
        JSONObject object = new JSONObject(json);
        return new ResolvedStream(
                object.optString("stream_url", ""),
                object.optString("title", ""),
                object.optString("channel_title", ""),
                object.optString("thumbnail", ""),
                Math.max(0L, object.optLong("duration", 0L)) * 1000L
        );
    }

    private static void ensurePython(Context context) {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
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
