package com.ytet.android.stream;

import java.util.Locale;

public final class OnlineStreamVideo {
    private final String id;
    private final String title;
    private final String channelTitle;
    private final String watchUrl;
    private final String thumbnailUrl;
    private final long durationMs;
    private final long viewCount;
    private final long publishedRank;
    private final int sourceIndex;

    public OnlineStreamVideo(
            String id,
            String title,
            String channelTitle,
            String watchUrl,
            String thumbnailUrl,
            long durationMs,
            long viewCount,
            long publishedRank,
            int sourceIndex
    ) {
        this.id = clean(id, stableId(watchUrl));
        this.title = clean(title, "제목 없음");
        this.channelTitle = clean(channelTitle, "YouTube");
        this.watchUrl = clean(watchUrl, "");
        this.thumbnailUrl = clean(thumbnailUrl, "");
        this.durationMs = Math.max(0L, durationMs);
        this.viewCount = Math.max(0L, viewCount);
        this.publishedRank = Math.max(0L, publishedRank);
        this.sourceIndex = Math.max(0, sourceIndex);
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String channelTitle() {
        return channelTitle;
    }

    public String watchUrl() {
        return watchUrl;
    }

    public String thumbnailUrl() {
        return thumbnailUrl;
    }

    public long durationMs() {
        return durationMs;
    }

    public long viewCount() {
        return viewCount;
    }

    public long publishedRank() {
        return publishedRank;
    }

    public int sourceIndex() {
        return sourceIndex;
    }

    public long playbackId() {
        long hash = 1125899906842597L;
        String key = watchUrl.isEmpty() ? id : watchUrl;
        for (int index = 0; index < key.length(); index++) {
            hash = 31L * hash + key.charAt(index);
        }
        return hash == Long.MIN_VALUE ? -1L : -Math.abs(hash);
    }

    private static String clean(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String stableId(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return text.isEmpty() ? "online" : Integer.toHexString(text.hashCode());
    }
}
