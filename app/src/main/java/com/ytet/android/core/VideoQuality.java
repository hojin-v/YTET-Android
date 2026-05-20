package com.ytet.android.core;

public enum VideoQuality {
    BEST("best", "원본 최고품질"),
    P1080("1080", "1080p MP4"),
    P720("720", "720p MP4"),
    P480("480", "480p MP4");

    private final String value;
    private final String label;

    VideoQuality(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public static VideoQuality fromValue(String value) {
        if ("1080".equalsIgnoreCase(value) || "1080p".equalsIgnoreCase(value)) {
            return P1080;
        }
        if ("720".equalsIgnoreCase(value) || "720p".equalsIgnoreCase(value)) {
            return P720;
        }
        if ("480".equalsIgnoreCase(value) || "480p".equalsIgnoreCase(value)) {
            return P480;
        }
        return BEST;
    }

    public static String[] labels() {
        VideoQuality[] values = values();
        String[] labels = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            labels[index] = values[index].label;
        }
        return labels;
    }

    public static VideoQuality fromLabel(String label) {
        for (VideoQuality quality : values()) {
            if (quality.label.equals(label)) {
                return quality;
            }
        }
        return BEST;
    }
}

