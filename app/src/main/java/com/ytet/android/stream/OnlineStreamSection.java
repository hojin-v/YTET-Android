package com.ytet.android.stream;

import java.util.ArrayList;
import java.util.List;

public final class OnlineStreamSection {
    private final String channelId;
    private final String channelTitle;
    private final String channelUrl;
    private final String avatarUrl;
    private final List<OnlineStreamVideo> videos;

    public OnlineStreamSection(
            String channelId,
            String channelTitle,
            String channelUrl,
            String avatarUrl,
            List<OnlineStreamVideo> videos
    ) {
        this.channelId = clean(channelId, "channel");
        this.channelTitle = clean(channelTitle, "YouTube 채널");
        this.channelUrl = clean(channelUrl, "");
        this.avatarUrl = clean(avatarUrl, "");
        this.videos = videos == null ? new ArrayList<>() : new ArrayList<>(videos);
    }

    public String channelId() {
        return channelId;
    }

    public String channelTitle() {
        return channelTitle;
    }

    public String channelUrl() {
        return channelUrl;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public List<OnlineStreamVideo> videos() {
        return new ArrayList<>(videos);
    }

    private static String clean(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
