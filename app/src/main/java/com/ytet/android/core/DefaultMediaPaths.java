package com.ytet.android.core;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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

    public static boolean isDefaultTreeUriFor(MediaType mediaType, String treeUri) {
        String relativePath = primaryExternalStorageRelativePathFromTreeUri(treeUri);
        return !relativePath.isEmpty()
                && normalizeRelativePath(relativePath).equals(extractionRelativePath(mediaType));
    }

    public static String displayTreePath(String treeUri) {
        String relativePath = primaryExternalStorageRelativePathFromTreeUri(treeUri);
        return relativePath.isEmpty() ? treeUri : "/" + relativePath;
    }

    public static String primaryExternalStorageRelativePathFromTreeUri(String treeUri) {
        return primaryExternalStorageRelativePathFromUriSegment(treeUri, "/tree/");
    }

    public static String primaryExternalStorageRelativePathFromDocumentUri(String documentUri) {
        String relativePath = primaryExternalStorageRelativePathFromUriSegment(documentUri, "/document/");
        return relativePath.isEmpty()
                ? primaryExternalStorageRelativePathFromTreeUri(documentUri)
                : relativePath;
    }

    public static String normalizeRelativePath(String path) {
        String normalized = cleanRelativePath(path);
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    public static String cleanRelativePath(String path) {
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
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized;
    }

    private static String primaryExternalStorageRelativePathFromUriSegment(String uri, String marker) {
        String encodedDocumentId = encodedUriSegment(uri, marker);
        if (encodedDocumentId.isEmpty()) {
            return "";
        }
        String documentId = decodeUriSegment(encodedDocumentId);
        int separator = documentId.indexOf(':');
        if (separator <= 0 || separator == documentId.length() - 1) {
            return "";
        }
        String volume = documentId.substring(0, separator);
        if (!"primary".equalsIgnoreCase(volume)) {
            return "";
        }
        return cleanRelativePath(documentId.substring(separator + 1));
    }

    private static String encodedUriSegment(String uri, String marker) {
        if (uri == null || uri.trim().isEmpty()) {
            return "";
        }
        int start = uri.indexOf(marker);
        if (start < 0) {
            return "";
        }
        start += marker.length();
        int end = uri.indexOf('/', start);
        int query = uri.indexOf('?', start);
        int fragment = uri.indexOf('#', start);
        if (end < 0 || (query >= 0 && query < end)) {
            end = query;
        }
        if (end < 0 || (fragment >= 0 && fragment < end)) {
            end = fragment;
        }
        if (end < 0) {
            end = uri.length();
        }
        return uri.substring(start, end);
    }

    private static String decodeUriSegment(String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8.name());
        } catch (Exception exception) {
            return value;
        }
    }
}
