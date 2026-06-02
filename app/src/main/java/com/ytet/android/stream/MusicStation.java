package com.ytet.android.stream;

public final class MusicStation {
    public enum MixType {
        ALL,
        ARTIST,
        FOLDER,
        TRACK
    }

    private final String id;
    private final String title;
    private final String category;
    private final String subtitle;
    private final String description;
    private final MixType mixType;
    private final String mixValue;
    private final int accentColor;

    public MusicStation(
            String id,
            String title,
            String category,
            String subtitle,
            String description,
            MixType mixType,
            String mixValue,
            int accentColor
    ) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        this.category = requireText(category, "category");
        this.subtitle = requireText(subtitle, "subtitle");
        this.description = requireText(description, "description");
        this.mixType = mixType == null ? MixType.ALL : mixType;
        this.mixValue = mixValue == null ? "" : mixValue.trim();
        this.accentColor = accentColor;
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

    public MixType mixType() {
        return mixType;
    }

    public String mixValue() {
        return mixValue;
    }

    public int accentColor() {
        return accentColor;
    }

    public boolean isArtistMix() {
        return "아티스트 믹스".equals(category);
    }

    public boolean isFolderMix() {
        return "폴더 믹스".equals(category);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
