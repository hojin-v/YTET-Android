package com.ytet.android.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class StreamUrlResolver {
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;

    private StreamUrlResolver() {
    }

    public static String resolve(String url) throws IOException {
        String safeUrl = requireUrl(url);
        String lower = safeUrl.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".m3u8")) {
            return safeUrl;
        }
        if (!lower.endsWith(".m3u") && !lower.endsWith(".pls")) {
            return safeUrl;
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(safeUrl).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "YTET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append('\n');
            }
            return resolveFromPlaylist(safeUrl, body.toString());
        } finally {
            connection.disconnect();
        }
    }

    public static String resolveFromPlaylist(String originalUrl, String playlistBody) {
        String safeOriginal = requireUrl(originalUrl);
        if (playlistBody == null || playlistBody.trim().isEmpty()) {
            return safeOriginal;
        }

        for (String rawLine : playlistBody.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.regionMatches(true, 0, "File", 0, 4)) {
                int equals = line.indexOf('=');
                if (equals > -1 && equals < line.length() - 1) {
                    String value = line.substring(equals + 1).trim();
                    if (isNetworkUrl(value)) {
                        return value;
                    }
                }
            } else if (isNetworkUrl(line)) {
                return line;
            }
        }
        return safeOriginal;
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
