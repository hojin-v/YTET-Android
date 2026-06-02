package com.ytet.android.stream;

public final class MusicStation {
    private final String id;
    private final String title;
    private final String category;
    private final String subtitle;
    private final String description;
    private final String streamUrl;
    private final int accentColor;

    public MusicStation(
            String id,
            String title,
            String category,
            String subtitle,
            String description,
            String streamUrl,
            int accentColor
    ) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        this.category = requireText(category, "category");
        this.subtitle = requireText(subtitle, "subtitle");
        this.description = requireText(description, "description");
        this.streamUrl = requireText(streamUrl, "streamUrl");
        this.accentColor = accentColor;
    }

    public static MusicStation custom(String streamUrl) {
        return new MusicStation(
                "custom",
                "직접 스트림",
                "스트리밍",
                "사용자 입력 URL",
                "입력한 라디오/오디오 스트림을 바로 재생합니다.",
                streamUrl,
                0xFFE50914
        );
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String category() {
        return category;
    }

    public String subtitle() {
        return subtitle;
    }

    public String description() {
        return description;
    }

    public String streamUrl() {
        return streamUrl;
    }

    public int accentColor() {
        return accentColor;
    }

    public boolean isArtistMix() {
        return "아티스트 믹스".equals(category);
    }

    public boolean isGenreMix() {
        return "장르 믹스".equals(category);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
