package com.ytet.android.core;

import android.content.Intent;

public final class ExtractionRequest {
    public static final String EXTRA_URL = "com.ytet.android.extra.URL";
    public static final String EXTRA_OUTPUT_TREE_URI = "com.ytet.android.extra.OUTPUT_TREE_URI";
    public static final String EXTRA_MEDIA_TYPE = "com.ytet.android.extra.MEDIA_TYPE";
    public static final String EXTRA_OPTION = "com.ytet.android.extra.OPTION";
    public static final String EXTRA_INCLUDE_SUBTITLES = "com.ytet.android.extra.INCLUDE_SUBTITLES";

    private final String url;
    private final String outputTreeUri;
    private final MediaType mediaType;
    private final String option;
    private final boolean includeSubtitles;

    public ExtractionRequest(
            String url,
            String outputTreeUri,
            MediaType mediaType,
            String option,
            boolean includeSubtitles
    ) {
        this.url = YoutubeUrlValidator.validate(url);
        this.outputTreeUri = outputTreeUri;
        this.mediaType = mediaType == null ? MediaType.AUDIO : mediaType;
        this.option = option == null || option.trim().isEmpty() ? defaultOption(this.mediaType) : option;
        this.includeSubtitles = includeSubtitles;
    }

    public String url() {
        return url;
    }

    public String outputTreeUri() {
        return outputTreeUri;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public String option() {
        return option;
    }

    public boolean includeSubtitles() {
        return includeSubtitles;
    }

    public void writeTo(Intent intent) {
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_OUTPUT_TREE_URI, outputTreeUri);
        intent.putExtra(EXTRA_MEDIA_TYPE, mediaType.value());
        intent.putExtra(EXTRA_OPTION, option);
        intent.putExtra(EXTRA_INCLUDE_SUBTITLES, includeSubtitles);
    }

    public static ExtractionRequest fromIntent(Intent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("추출 요청이 없습니다.");
        }
        String url = intent.getStringExtra(EXTRA_URL);
        String outputTreeUri = intent.getStringExtra(EXTRA_OUTPUT_TREE_URI);
        if (outputTreeUri == null || outputTreeUri.trim().isEmpty()) {
            throw new IllegalArgumentException("저장 폴더를 선택하세요.");
        }
        MediaType mediaType = MediaType.fromValue(intent.getStringExtra(EXTRA_MEDIA_TYPE));
        return new ExtractionRequest(
                url,
                outputTreeUri,
                mediaType,
                intent.getStringExtra(EXTRA_OPTION),
                intent.getBooleanExtra(EXTRA_INCLUDE_SUBTITLES, false)
        );
    }

    private static String defaultOption(MediaType mediaType) {
        return mediaType == MediaType.VIDEO ? VideoQuality.BEST.value() : AudioFormat.M4A.value();
    }
}
