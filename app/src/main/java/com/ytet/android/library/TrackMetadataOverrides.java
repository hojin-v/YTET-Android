package com.ytet.android.library;

import android.content.Context;
import android.content.SharedPreferences;

public final class TrackMetadataOverrides {
    private static final String PREFS = "ytet_track_metadata";
    private static final String TITLE_PREFIX = "title:";
    private static final String ARTIST_PREFIX = "artist:";
    private static final String ALBUM_PREFIX = "album:";

    private TrackMetadataOverrides() {
    }

    public static DeviceAudioTrack apply(Context context, DeviceAudioTrack track) {
        if (context == null || track == null) {
            return track;
        }
        SharedPreferences prefs = prefs(context);
        String key = key(track);
        String title = clean(prefs.getString(TITLE_PREFIX + key, null), track.title());
        String artist = clean(prefs.getString(ARTIST_PREFIX + key, null), track.artist());
        String album = clean(prefs.getString(ALBUM_PREFIX + key, null), track.album());
        if (title.equals(track.title()) && artist.equals(track.artist()) && album.equals(track.album())) {
            return track;
        }
        String representativeArtist = artist.equals(track.artist()) ? track.representativeArtist() : null;
        return new DeviceAudioTrack(
                track.id(),
                title,
                artist,
                album,
                track.displayName(),
                track.folder(),
                track.contentUri(),
                track.albumArtUri(),
                track.albumId(),
                track.trackNumber(),
                track.dateAddedMs(),
                track.durationMs(),
                track.sizeBytes(),
                representativeArtist
        );
    }

    public static DeviceAudioTrack save(Context context, DeviceAudioTrack track, String title, String artist, String album) {
        if (context == null || track == null) {
            return track;
        }
        String cleanTitle = clean(title, track.title());
        String cleanArtist = clean(artist, track.artist());
        String cleanAlbum = clean(album, track.album());
        String key = key(track);
        prefs(context).edit()
                .putString(TITLE_PREFIX + key, cleanTitle)
                .putString(ARTIST_PREFIX + key, cleanArtist)
                .putString(ALBUM_PREFIX + key, cleanAlbum)
                .apply();
        return apply(context, track);
    }

    public static DeviceAudioTrack saveAlbum(Context context, DeviceAudioTrack track, String album) {
        if (context == null || track == null) {
            return track;
        }
        prefs(context).edit()
                .putString(ALBUM_PREFIX + key(track), clean(album, track.album()))
                .apply();
        return apply(context, track);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(DeviceAudioTrack track) {
        String contentUri = track.contentUri();
        return contentUri == null || contentUri.trim().isEmpty()
                ? Long.toString(track.id())
                : contentUri.trim();
    }

    private static String clean(String value, String fallback) {
        String clean = value == null ? "" : value.trim();
        if (!clean.isEmpty()) {
            return clean;
        }
        return fallback == null || fallback.trim().isEmpty() ? "" : fallback.trim();
    }
}
