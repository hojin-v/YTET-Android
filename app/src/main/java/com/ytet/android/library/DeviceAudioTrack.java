package com.ytet.android.library;

public final class DeviceAudioTrack {
    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final String displayName;
    private final String folder;
    private final String contentUri;
    private final String albumArtUri;
    private final long albumId;
    private final int trackNumber;
    private final long dateAddedMs;
    private final long durationMs;
    private final long sizeBytes;

    public DeviceAudioTrack(
            long id,
            String title,
            String artist,
            String album,
            String displayName,
            String folder,
            String contentUri,
            long durationMs,
            long sizeBytes
    ) {
        this(id, title, artist, album, displayName, folder, contentUri, "", 0L, 0, 0L, durationMs, sizeBytes);
    }

    public DeviceAudioTrack(
            long id,
            String title,
            String artist,
            String album,
            String displayName,
            String folder,
            String contentUri,
            String albumArtUri,
            long durationMs,
            long sizeBytes
    ) {
        this(id, title, artist, album, displayName, folder, contentUri, albumArtUri, 0L, 0, 0L, durationMs, sizeBytes);
    }

    public DeviceAudioTrack(
            long id,
            String title,
            String artist,
            String album,
            String displayName,
            String folder,
            String contentUri,
            String albumArtUri,
            long albumId,
            int trackNumber,
            long dateAddedMs,
            long durationMs,
            long sizeBytes
    ) {
        this.id = id;
        this.title = clean(title, "제목 없음");
        this.artist = MusicLibrary.normalizeArtistDisplay(clean(artist, "알 수 없는 아티스트"));
        this.album = clean(album, "앨범 정보 없음");
        this.displayName = clean(displayName, this.title);
        this.folder = clean(folder, "알 수 없는 폴더");
        this.contentUri = clean(contentUri, "");
        this.albumArtUri = clean(albumArtUri, "");
        this.albumId = Math.max(0L, albumId);
        this.trackNumber = Math.max(0, trackNumber);
        this.dateAddedMs = Math.max(0L, dateAddedMs);
        this.durationMs = Math.max(0, durationMs);
        this.sizeBytes = Math.max(0, sizeBytes);
    }

    public long id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String artist() {
        return artist;
    }

    public String album() {
        return album;
    }

    public String displayName() {
        return displayName;
    }

    public String folder() {
        return folder;
    }

    public String contentUri() {
        return contentUri;
    }

    public String albumArtUri() {
        return albumArtUri;
    }

    public long albumId() {
        return albumId;
    }

    public int trackNumber() {
        return trackNumber;
    }

    public long dateAddedMs() {
        return dateAddedMs;
    }

    public long durationMs() {
        return durationMs;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    private static String clean(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
