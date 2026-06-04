package com.ytet.android.library;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class UserPlaylists {
    private static final String PREFS = "ytet_user_playlists";
    private static final String KEY_IDS = "playlist_ids";
    private static final String TITLE_PREFIX = "playlist_title_";
    private static final String TRACKS_PREFIX = "playlist_tracks_";
    private static final String UPDATED_PREFIX = "playlist_updated_";

    private UserPlaylists() {
    }

    public static List<Playlist> list(Context context) {
        SharedPreferences prefs = prefs(context);
        List<Playlist> playlists = new ArrayList<>();
        for (String id : splitLines(prefs.getString(KEY_IDS, ""))) {
            String title = prefs.getString(TITLE_PREFIX + id, "");
            if (title == null || title.trim().isEmpty()) {
                continue;
            }
            playlists.add(new Playlist(
                    id,
                    title.trim(),
                    parseTrackIds(prefs.getString(TRACKS_PREFIX + id, "")),
                    prefs.getLong(UPDATED_PREFIX + id, 0L)
            ));
        }
        return playlists;
    }

    public static Playlist create(Context context, String title, List<Long> trackIds) {
        String id = "pl_" + Long.toString(System.currentTimeMillis(), 36);
        List<String> ids = splitLines(prefs(context).getString(KEY_IDS, ""));
        while (ids.contains(id)) {
            id = "pl_" + Long.toString(System.nanoTime(), 36);
        }
        ids.add(id);
        Playlist playlist = new Playlist(id, cleanTitle(title), uniqueTrackIds(trackIds), System.currentTimeMillis());
        prefs(context).edit()
                .putString(KEY_IDS, joinLines(ids))
                .putString(TITLE_PREFIX + id, playlist.title())
                .putString(TRACKS_PREFIX + id, joinTrackIds(playlist.trackIds()))
                .putLong(UPDATED_PREFIX + id, playlist.updatedAtMs())
                .apply();
        return playlist;
    }

    public static Playlist rename(Context context, String playlistId, String title) {
        Playlist current = find(context, playlistId);
        if (current == null) {
            return null;
        }
        Playlist renamed = new Playlist(current.id(), cleanTitle(title), current.trackIds(), System.currentTimeMillis());
        prefs(context).edit()
                .putString(TITLE_PREFIX + renamed.id(), renamed.title())
                .putLong(UPDATED_PREFIX + renamed.id(), renamed.updatedAtMs())
                .apply();
        return renamed;
    }

    public static Playlist addTracks(Context context, String playlistId, List<Long> trackIds) {
        Playlist current = find(context, playlistId);
        if (current == null) {
            return null;
        }
        List<Long> merged = new ArrayList<>(current.trackIds());
        Set<Long> existing = new LinkedHashSet<>(merged);
        for (Long id : trackIds == null ? new ArrayList<Long>() : trackIds) {
            if (id != null && id > 0L && existing.add(id)) {
                merged.add(id);
            }
        }
        Playlist updated = new Playlist(current.id(), current.title(), merged, System.currentTimeMillis());
        saveTracks(context, updated);
        return updated;
    }

    public static Playlist updateTracks(Context context, String playlistId, List<Long> trackIds) {
        Playlist current = find(context, playlistId);
        if (current == null) {
            return null;
        }
        Playlist updated = new Playlist(current.id(), current.title(), uniqueTrackIds(trackIds), System.currentTimeMillis());
        saveTracks(context, updated);
        return updated;
    }

    public static void delete(Context context, String playlistId) {
        if (playlistId == null || playlistId.trim().isEmpty()) {
            return;
        }
        String id = playlistId.trim();
        List<String> ids = splitLines(prefs(context).getString(KEY_IDS, ""));
        ids.remove(id);
        prefs(context).edit()
                .putString(KEY_IDS, joinLines(ids))
                .remove(TITLE_PREFIX + id)
                .remove(TRACKS_PREFIX + id)
                .remove(UPDATED_PREFIX + id)
                .apply();
    }

    public static Playlist find(Context context, String playlistId) {
        if (playlistId == null || playlistId.trim().isEmpty()) {
            return null;
        }
        String id = playlistId.trim();
        SharedPreferences prefs = prefs(context);
        String title = prefs.getString(TITLE_PREFIX + id, "");
        if (title == null || title.trim().isEmpty()) {
            return null;
        }
        return new Playlist(
                id,
                title.trim(),
                parseTrackIds(prefs.getString(TRACKS_PREFIX + id, "")),
                prefs.getLong(UPDATED_PREFIX + id, 0L)
        );
    }

    private static void saveTracks(Context context, Playlist playlist) {
        prefs(context).edit()
                .putString(TRACKS_PREFIX + playlist.id(), joinTrackIds(playlist.trackIds()))
                .putLong(UPDATED_PREFIX + playlist.id(), playlist.updatedAtMs())
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String cleanTitle(String title) {
        String clean = title == null ? "" : title.trim();
        return clean.isEmpty() ? "새 재생목록" : clean;
    }

    private static List<Long> uniqueTrackIds(List<Long> trackIds) {
        Set<Long> unique = new LinkedHashSet<>();
        if (trackIds != null) {
            for (Long id : trackIds) {
                if (id != null && id > 0L) {
                    unique.add(id);
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private static List<Long> parseTrackIds(String value) {
        List<Long> ids = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return ids;
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            try {
                long id = Long.parseLong(part.trim());
                if (id > 0L) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private static String joinTrackIds(List<Long> ids) {
        StringBuilder builder = new StringBuilder();
        for (Long id : ids == null ? new ArrayList<Long>() : ids) {
            if (id == null || id <= 0L) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.US, "%d", id));
        }
        return builder.toString();
    }

    private static List<String> splitLines(String value) {
        List<String> lines = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return lines;
        }
        for (String line : value.split("\n")) {
            String clean = line.trim();
            if (!clean.isEmpty() && !lines.contains(clean)) {
                lines.add(clean);
            }
        }
        return lines;
    }

    private static String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines == null ? new ArrayList<String>() : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line.trim());
        }
        return builder.toString();
    }

    public static final class Playlist {
        private final String id;
        private final String title;
        private final List<Long> trackIds;
        private final long updatedAtMs;

        public Playlist(String id, String title, List<Long> trackIds, long updatedAtMs) {
            this.id = id == null ? "" : id.trim();
            this.title = cleanTitle(title);
            this.trackIds = uniqueTrackIds(trackIds);
            this.updatedAtMs = Math.max(0L, updatedAtMs);
        }

        public String id() {
            return id;
        }

        public String title() {
            return title;
        }

        public List<Long> trackIds() {
            return new ArrayList<>(trackIds);
        }

        public long updatedAtMs() {
            return updatedAtMs;
        }
    }
}
