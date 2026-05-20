package com.ytet.android.core;

public enum MediaType {
    AUDIO("audio"),
    VIDEO("video");

    private final String value;

    MediaType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static MediaType fromValue(String value) {
        if ("video".equalsIgnoreCase(value)) {
            return VIDEO;
        }
        return AUDIO;
    }
}

