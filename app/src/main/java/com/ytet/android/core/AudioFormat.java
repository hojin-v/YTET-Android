package com.ytet.android.core;

public enum AudioFormat {
    M4A("m4a", "M4A (AAC)"),
    MP3("mp3", "MP3"),
    ORIGINAL("original", "Original Opus");

    private final String value;
    private final String label;

    AudioFormat(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public static AudioFormat fromValue(String value) {
        if ("mp3".equalsIgnoreCase(value)) {
            return MP3;
        }
        if ("original".equalsIgnoreCase(value) || "best".equalsIgnoreCase(value)) {
            return ORIGINAL;
        }
        return M4A;
    }

    public static String[] labels() {
        AudioFormat[] values = values();
        String[] labels = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            labels[index] = values[index].label;
        }
        return labels;
    }

    public static AudioFormat fromLabel(String label) {
        for (AudioFormat format : values()) {
            if (format.label.equals(label)) {
                return format;
            }
        }
        return M4A;
    }
}

