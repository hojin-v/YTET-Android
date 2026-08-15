package com.ytet.android.library;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * User created playlists.
 *
 * <p>Entries keep enough metadata to rebuild a track on their own, so online stream videos - which
 * have no MediaStore id - can be saved next to local files. Local entries are still identified by
 * their MediaStore id so edited tags and album art follow the library.</p>
 */
public final class UserPlaylists {
    private static final String PREFS = "ytet_user_playlists";
    private static final String KEY_IDS = "playlist_ids";
    private static final String TITLE_PREFIX = "playlist_title_";
    private static final String TRACKS_PREFIX = "playlist_tracks_";
    private static final String ENTRIES_PREFIX = "playlist_entries_";
    private static final String UPDATED_PREFIX = "playlist_updated_";
    private static final String FIELD_SEPARATOR = "\u001f";

    private UserPlaylists() {
    }

    public static List<Playlist> list(Context context) {
        SharedPreferences prefs = prefs(context);
        List<Playlist> playlists = new ArrayList<>();
        for (String id : splitLines(prefs.getString(KEY_IDS, ""))) {
            Playlist playlist = read(prefs, id);
            if (playlist != null) {
                playlists.add(playlist);
            }
        }
        return playlists;
    }

    public static Playlist create(Context context, String title, List<Entry> entries) {
        String id = "pl_" + Long.toString(System.currentTimeMillis(), 36);
        List<String> ids = splitLines(prefs(context).getString(KEY_IDS, ""));
        while (ids.contains(id)) {
            id = "pl_" + Long.toString(System.nanoTime(), 36);
        }
        ids.add(id);
        Playlist playlist = new Playlist(id, cleanTitle(title), uniqueEntries(entries), System.currentTimeMillis());
        prefs(context).edit()
                .putString(KEY_IDS, joinLines(ids))
                .putString(TITLE_PREFIX + id, playlist.title())
                .putString(ENTRIES_PREFIX + id, serializeEntries(playlist.entries()))
                .putString(TRACKS_PREFIX + id, joinTrackIds(playlist.localTrackIds()))
                .putLong(UPDATED_PREFIX + id, playlist.updatedAtMs())
                .apply();
        return playlist;
    }

    public static Playlist rename(Context context, String playlistId, String title) {
        Playlist current = find(context, playlistId);
        if (current == null) {
            return null;
        }
        Playlist renamed = new Playlist(current.id(), cleanTitle(title), current.entries(), System.currentTimeMillis());
        prefs(context).edit()
                .putString(TITLE_PREFIX + renamed.id(), renamed.title())
                .putLong(UPDATED_PREFIX + renamed.id(), renamed.updatedAtMs())
                .apply();
        return renamed;
    }

    /** Appends entries that are not in the playlist yet and returns the saved playlist. */
    public static Playlist addEntries(Context context, String playlistId, List<Entry> entries) {
        Playlist current = find(context, playlistId);
        if (current == null) {
            return null;
        }
        List<Entry> merged = new ArrayList<>(current.entries());
        Set<String> existing = new LinkedHashSet<>();
        for (Entry entry : merged) {
            existing.add(entry.key());
        }
        for (Entry entry : entries == null ? new ArrayList<Entry>() : entries) {
            if (entry != null && entry.valid() && existing.add(entry.key())) {
                merged.add(entry);
            }
        }
        Playlist updated = new Playlist(current.id(), current.title(), merged, System.currentTimeMillis());
        saveEntries(context, updated);
        return updated;
    }

    public static Playlist updateEntries(Context context, String playlistId, List<Entry> entries) {
        Playlist current = find(context, playlistId);
        if (current == null) {
            return null;
        }
        Playlist updated = new Playlist(current.id(), current.title(), uniqueEntries(entries), System.currentTimeMillis());
        saveEntries(context, updated);
        return updated;
    }

    public static Playlist removeEntry(Context context, String playlistId, String entryKey) {
        Playlist current = find(context, playlistId);
        if (current == null || entryKey == null || entryKey.trim().isEmpty()) {
            return current;
        }
        List<Entry> remaining = new ArrayList<>();
        for (Entry entry : current.entries()) {
            if (!entryKey.equals(entry.key())) {
                remaining.add(entry);
            }
        }
        if (remaining.size() == current.entries().size()) {
            return current;
        }
        Playlist updated = new Playlist(current.id(), current.title(), remaining, System.currentTimeMillis());
        saveEntries(context, updated);
        return updated;
    }

    /** Number of playlists that already contain the given entry. */
    public static int countPlaylistsContaining(Context context, Entry entry) {
        if (entry == null || !entry.valid()) {
            return 0;
        }
        int count = 0;
        for (Playlist playlist : list(context)) {
            if (playlist.contains(entry)) {
                count++;
            }
        }
        return count;
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
                .remove(ENTRIES_PREFIX + id)
                .remove(UPDATED_PREFIX + id)
                .apply();
    }

    public static Playlist find(Context context, String playlistId) {
        if (playlistId == null || playlistId.trim().isEmpty()) {
            return null;
        }
        return read(prefs(context), playlistId.trim());
    }

    private static Playlist read(SharedPreferences prefs, String id) {
        String title = prefs.getString(TITLE_PREFIX + id, "");
        if (title == null || title.trim().isEmpty()) {
            return null;
        }
        String serializedEntries = prefs.getString(ENTRIES_PREFIX + id, "");
        List<Entry> entries = serializedEntries == null || serializedEntries.trim().isEmpty()
                ? legacyEntries(prefs.getString(TRACKS_PREFIX + id, ""))
                : parseEntries(serializedEntries);
        return new Playlist(id, title.trim(), entries, prefs.getLong(UPDATED_PREFIX + id, 0L));
    }

    private static void saveEntries(Context context, Playlist playlist) {
        prefs(context).edit()
                .putString(ENTRIES_PREFIX + playlist.id(), serializeEntries(playlist.entries()))
                .putString(TRACKS_PREFIX + playlist.id(), joinTrackIds(playlist.localTrackIds()))
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

    private static List<Entry> uniqueEntries(List<Entry> entries) {
        Set<String> seen = new LinkedHashSet<>();
        List<Entry> unique = new ArrayList<>();
        if (entries == null) {
            return unique;
        }
        for (Entry entry : entries) {
            if (entry != null && entry.valid() && seen.add(entry.key())) {
                unique.add(entry);
            }
        }
        return unique;
    }

    static String serializeEntries(List<Entry> entries) {
        StringBuilder builder = new StringBuilder();
        for (Entry entry : entries == null ? new ArrayList<Entry>() : entries) {
            if (entry == null || !entry.valid()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(String.format(Locale.US, "%d", entry.trackId())).append(FIELD_SEPARATOR)
                    .append(field(entry.uri())).append(FIELD_SEPARATOR)
                    .append(field(entry.title())).append(FIELD_SEPARATOR)
                    .append(field(entry.artist())).append(FIELD_SEPARATOR)
                    .append(field(entry.album())).append(FIELD_SEPARATOR)
                    .append(field(entry.artUri())).append(FIELD_SEPARATOR)
                    .append(String.format(Locale.US, "%d", entry.durationMs()));
        }
        return builder.toString();
    }

    static List<Entry> parseEntries(String value) {
        List<Entry> entries = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return entries;
        }
        for (String line : value.split("\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split(FIELD_SEPARATOR, -1);
            if (parts.length < 2) {
                continue;
            }
            Entry entry = new Entry(
                    parseLong(parts[0]),
                    parts[1],
                    parts.length > 2 ? parts[2] : "",
                    parts.length > 3 ? parts[3] : "",
                    parts.length > 4 ? parts[4] : "",
                    parts.length > 5 ? parts[5] : "",
                    parts.length > 6 ? parseLong(parts[6]) : 0L
            );
            if (entry.valid()) {
                entries.add(entry);
            }
        }
        return uniqueEntries(entries);
    }

    static List<Entry> legacyEntries(String value) {
        List<Entry> entries = new ArrayList<>();
        for (Long id : parseTrackIds(value)) {
            entries.add(new Entry(id, "", "", "", "", "", 0L));
        }
        return uniqueEntries(entries);
    }

    private static String field(String value) {
        String clean = value == null ? "" : value.trim();
        return clean.replace(FIELD_SEPARATOR, " ").replace("\n", " ").replace("\r", " ");
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value == null ? "" : value.trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
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

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    /** One saved song. Either a MediaStore track or an online stream video. */
    public static final class Entry {
        private final long trackId;
        private final String uri;
        private final String title;
        private final String artist;
        private final String album;
        private final String artUri;
        private final long durationMs;

        public Entry(
                long trackId,
                String uri,
                String title,
                String artist,
                String album,
                String artUri,
                long durationMs
        ) {
            this.trackId = trackId;
            this.uri = clean(uri);
            this.title = clean(title);
            this.artist = clean(artist);
            this.album = clean(album);
            this.artUri = clean(artUri);
            this.durationMs = Math.max(0L, durationMs);
        }

        public static Entry of(DeviceAudioTrack track) {
            if (track == null) {
                return null;
            }
            return new Entry(
                    track.id(),
                    track.contentUri(),
                    track.title(),
                    track.artist(),
                    track.album(),
                    track.albumArtUri(),
                    track.durationMs()
            );
        }

        public DeviceAudioTrack toTrack() {
            return new DeviceAudioTrack(
                    trackId,
                    title,
                    artist,
                    album,
                    title,
                    online() ? "온라인 스트림" : "알 수 없는 폴더",
                    uri,
                    artUri,
                    0L,
                    0,
                    0L,
                    durationMs,
                    0L,
                    artist
            );
        }

        public long trackId() {
            return trackId;
        }

        public String uri() {
            return uri;
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

        public String artUri() {
            return artUri;
        }

        public long durationMs() {
            return durationMs;
        }

        public boolean online() {
            String lower = uri.toLowerCase(Locale.ROOT);
            return lower.startsWith("http://") || lower.startsWith("https://");
        }

        public boolean valid() {
            return trackId > 0L || online();
        }

        public String key() {
            return online() ? uri : "id:" + trackId;
        }
    }

    public static final class Playlist {
        private final String id;
        private final String title;
        private final List<Entry> entries;
        private final long updatedAtMs;

        public Playlist(String id, String title, List<Entry> entries, long updatedAtMs) {
            this.id = id == null ? "" : id.trim();
            this.title = cleanTitle(title);
            this.entries = uniqueEntries(entries);
            this.updatedAtMs = Math.max(0L, updatedAtMs);
        }

        public String id() {
            return id;
        }

        public String title() {
            return title;
        }

        public List<Entry> entries() {
            return new ArrayList<>(entries);
        }

        public int size() {
            return entries.size();
        }

        public boolean contains(Entry entry) {
            if (entry == null) {
                return false;
            }
            for (Entry candidate : entries) {
                if (candidate.key().equals(entry.key())) {
                    return true;
                }
            }
            return false;
        }

        public List<Long> localTrackIds() {
            List<Long> ids = new ArrayList<>();
            for (Entry entry : entries) {
                if (!entry.online() && entry.trackId() > 0L) {
                    ids.add(entry.trackId());
                }
            }
            return ids;
        }

        public long updatedAtMs() {
            return updatedAtMs;
        }
    }
}
