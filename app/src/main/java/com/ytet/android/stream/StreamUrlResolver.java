package com.ytet.android.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class StreamUrlResolver {
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final int MAX_REDIRECTS = 5;

    private StreamUrlResolver() {
    }

    public static String resolve(String url) throws IOException {
        String safeUrl = requireUrl(url);
        if (isHlsPlaylist(safeUrl)) {
            return safeUrl;
        }
        if (!shouldResolvePlaylist(safeUrl)) {
            return safeUrl;
        }

        return resolveFromPlaylist(safeUrl, readPlaylist(new URL(safeUrl), 0));
    }

    static boolean shouldResolvePlaylist(String url) {
        String path = pathOf(url);
        return path.endsWith(".m3u") || path.endsWith(".pls");
    }

    private static boolean isHlsPlaylist(String url) {
        return pathOf(url).endsWith(".m3u8");
    }

    private static String pathOf(String url) {
        try {
            return new URL(url).getPath().toLowerCase(Locale.ROOT);
        } catch (IOException exception) {
            return url.toLowerCase(Locale.ROOT);
        }
    }

    private static String readPlaylist(URL url, int redirectCount) throws IOException {
        if (redirectCount > MAX_REDIRECTS) {
            throw new IOException("스트림 리다이렉트가 너무 많습니다.");
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "YTET");
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                String location = connection.getHeaderField("Location");
                if (location == null || location.trim().isEmpty()) {
                    throw new IOException("스트림 리다이렉트 위치를 찾을 수 없습니다.");
                }
                return readPlaylist(new URL(url, location), redirectCount + 1);
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(),
                    StandardCharsets.UTF_8
            ))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line).append('\n');
                }
            }
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    public static String resolveFromPlaylist(String originalUrl, String playlistBody) {
        String safeOriginal = requireUrl(originalUrl);
        if (playlistBody == null || playlistBody.trim().isEmpty()) {
            return safeOriginal;
        }

        boolean plsPlaylist = pathOf(safeOriginal).endsWith(".pls");
        for (String rawLine : playlistBody.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.regionMatches(true, 0, "File", 0, 4)) {
                int equals = line.indexOf('=');
                if (equals > -1 && equals < line.length() - 1) {
                    String value = line.substring(equals + 1).trim();
                    String resolved = resolveCandidate(safeOriginal, value);
                    if (resolved != null) {
                        return resolved;
                    }
                }
            } else if (!plsPlaylist) {
                String resolved = resolveCandidate(safeOriginal, line);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return safeOriginal;
    }

    private static String resolveCandidate(String originalUrl, String value) {
        try {
            String resolved = URI.create(originalUrl).resolve(value).toString();
            return isNetworkUrl(resolved) ? resolved : null;
        } catch (IllegalArgumentException exception) {
            return isNetworkUrl(value) ? value : null;
        }
    }

    private static boolean isNetworkUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static String requireUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("스트림 URL을 입력하세요.");
        }
        return url.trim();
    }
}
