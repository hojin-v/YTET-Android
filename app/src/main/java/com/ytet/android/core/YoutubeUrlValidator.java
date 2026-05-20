package com.ytet.android.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class YoutubeUrlValidator {
    private static final Set<String> ALLOWED_HOSTS = new HashSet<>(Arrays.asList(
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "music.youtube.com",
            "youtu.be",
            "www.youtu.be",
            "youtube-nocookie.com",
            "www.youtube-nocookie.com"
    ));

    private YoutubeUrlValidator() {
    }

    public static String validate(String rawUrl) {
        String candidate = rawUrl == null ? "" : rawUrl.trim();
        if (candidate.isEmpty()) {
            throw new IllegalArgumentException("YouTube URL을 입력하세요.");
        }

        URI uri;
        try {
            uri = new URI(candidate);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("올바른 URL 형식이 아닙니다.");
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("http 또는 https로 시작하는 YouTube URL만 지원합니다.");
        }

        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("URL의 호스트를 확인할 수 없습니다.");
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.endsWith(".")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }
        if (!ALLOWED_HOSTS.contains(normalizedHost) && !normalizedHost.endsWith(".youtube.com")) {
            throw new IllegalArgumentException("YouTube, YouTube Music, youtu.be URL만 지원합니다.");
        }

        return candidate;
    }
}

