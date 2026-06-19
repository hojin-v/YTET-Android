package com.ytet.android.stream;

public final class OnlineStreamChannel {
    private final String id;
    private final String title;
    private final String url;

    public OnlineStreamChannel(String id, String title, String url) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        this.url = requireText(url, "url");
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String url() {
        return url;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
