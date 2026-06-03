package com.ytet.android.core;

import android.content.Intent;

public final class ExtractionRequest {
    public static final String EXTRA_URL = "com.ytet.android.extra.URL";
    public static final String EXTRA_OUTPUT_TREE_URI = "com.ytet.android.extra.OUTPUT_TREE_URI";
    public static final String EXTRA_MEDIA_TYPE = "com.ytet.android.extra.MEDIA_TYPE";
    public static final String EXTRA_OPTION = "com.ytet.android.extra.OPTION";
    public static final String EXTRA_INCLUDE_SUBTITLES = "com.ytet.android.extra.INCLUDE_SUBTITLES";
    public static final String EXTRA_INCLUDE_PLAYLIST = "com.ytet.android.extra.INCLUDE_PLAYLIST";
    public static final String EXTRA_ENHANCE_METADATA = "com.ytet.android.extra.ENHANCE_METADATA";

    private final String url;
    private final String outputTreeUri;
    private final MediaType mediaType;
    private final String option;
    private final boolean includeSubtitles;
    private final boolean includePlaylist;
    private final boolean enhanceMetadata;

    public ExtractionRequest(
            String url,
            String outputTreeUri,
            MediaType mediaType,
            String option,
            boolean includeSubtitles
    ) {
        this(url, outputTreeUri, mediaType, option, includeSubtitles, false, false);
    }

    public ExtractionRequest(
            String url,
            String outputTreeUri,
            MediaType mediaType,
            String option,
            boolean includeSubtitles,
            boolean includePlaylist
    ) {
        this(url, outputTreeUri, mediaType, option, includeSubtitles, includePlaylist, false);
    }

    public ExtractionRequest(
            String url,
            String outputTreeUri,
            MediaType mediaType,
            String option,
            boolean includeSubtitles,
            boolean includePlaylist,
            boolean enhanceMetadata
    ) {
        this.url = YoutubeUrlValidator.validate(url);
        this.outputTreeUri = DefaultMediaPaths.isDefaultOutput(outputTreeUri)
                ? DefaultMediaPaths.DEFAULT_OUTPUT_URI
                : outputTreeUri.trim();
        this.mediaType = mediaType == null ? MediaType.AUDIO : mediaType;
        this.option = option == null || option.trim().isEmpty() ? defaultOption(this.mediaType) : option;
        this.includeSubtitles = includeSubtitles;
        this.includePlaylist = includePlaylist;
        this.enhanceMetadata = enhanceMetadata;
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

    public boolean includePlaylist() {
        return includePlaylist;
    }

    public boolean enhanceMetadata() {
        return enhanceMetadata;
    }

    public boolean usesDefaultOutput() {
        return DefaultMediaPaths.isDefaultOutput(outputTreeUri);
    }

    public void writeTo(Intent intent) {
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_OUTPUT_TREE_URI, outputTreeUri);
        intent.putExtra(EXTRA_MEDIA_TYPE, mediaType.value());
        intent.putExtra(EXTRA_OPTION, option);
        intent.putExtra(EXTRA_INCLUDE_SUBTITLES, includeSubtitles);
        intent.putExtra(EXTRA_INCLUDE_PLAYLIST, includePlaylist);
        intent.putExtra(EXTRA_ENHANCE_METADATA, enhanceMetadata);
    }

    public static ExtractionRequest fromIntent(Intent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("추출 요청이 없습니다.");
        }
        String url = intent.getStringExtra(EXTRA_URL);
        String outputTreeUri = intent.getStringExtra(EXTRA_OUTPUT_TREE_URI);
        MediaType mediaType = MediaType.fromValue(intent.getStringExtra(EXTRA_MEDIA_TYPE));
        return new ExtractionRequest(
                url,
                outputTreeUri,
                mediaType,
                intent.getStringExtra(EXTRA_OPTION),
                intent.getBooleanExtra(EXTRA_INCLUDE_SUBTITLES, false),
                intent.getBooleanExtra(EXTRA_INCLUDE_PLAYLIST, false),
                intent.getBooleanExtra(EXTRA_ENHANCE_METADATA, false)
        );
    }

    private static String defaultOption(MediaType mediaType) {
        return mediaType == MediaType.VIDEO ? VideoQuality.BEST.value() : AudioFormat.M4A.value();
    }
}
