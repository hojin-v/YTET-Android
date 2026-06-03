package com.ytet.android.core;

public final class DefaultMediaPaths {
    public static final String DEFAULT_OUTPUT_URI = "ytet://default-output";
    public static final String DOWNLOADS_FOLDER = "Download";
    public static final String APP_FOLDER = "YTET";
    public static final String MUSIC_FOLDER = "Music";
    public static final String VIDEO_FOLDER = "Video";

    private DefaultMediaPaths() {
    }

    public static boolean isDefaultOutput(String outputUri) {
        return outputUri == null
                || outputUri.trim().isEmpty()
                || DEFAULT_OUTPUT_URI.equals(outputUri.trim());
    }

    public static String rootRelativePath() {
        return normalizeRelativePath(DOWNLOADS_FOLDER + "/" + APP_FOLDER);
    }

    public static String musicRelativePath() {
        return normalizeRelativePath(DOWNLOADS_FOLDER + "/" + APP_FOLDER + "/" + MUSIC_FOLDER);
    }

    public static String videoRelativePath() {
        return normalizeRelativePath(DOWNLOADS_FOLDER + "/" + APP_FOLDER + "/" + VIDEO_FOLDER);
    }

    public static String extractionRelativePath(MediaType mediaType) {
        return mediaType == MediaType.VIDEO ? videoRelativePath() : musicRelativePath();
    }

    public static String displayPath(MediaType mediaType) {
        return "/" + extractionRelativePath(mediaType);
    }

    public static String normalizeRelativePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
}
