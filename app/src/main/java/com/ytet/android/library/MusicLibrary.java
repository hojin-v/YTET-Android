package com.ytet.android.library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ytet.android.stream.MusicStation;

public final class MusicLibrary {
    public static final String ALL_FOLDERS = "전체";
    public static final long LONG_PLAYLIST_MIN_DURATION_MS = 20 * 60 * 1000L;

    private MusicLibrary() {
    }

    public static List<String> folderNames(List<DeviceAudioTrack> tracks) {
        Set<String> folders = new LinkedHashSet<>();
        folders.add(ALL_FOLDERS);
        if (tracks != null) {
            for (DeviceAudioTrack track : tracks) {
                folders.add(track.folder());
            }
        }
        return new ArrayList<>(folders);
    }

    public static List<DeviceAudioTrack> filterByFolder(List<DeviceAudioTrack> tracks, String folder) {
        List<DeviceAudioTrack> filtered = new ArrayList<>();
        if (tracks == null) {
            return filtered;
        }
        if (folder == null || folder.trim().isEmpty() || ALL_FOLDERS.equals(folder)) {
            filtered.addAll(tracks);
            return filtered;
        }
        for (DeviceAudioTrack track : tracks) {
            if (folder.equals(track.folder())) {
                filtered.add(track);
            }
        }
        return filtered;
    }

    public static List<DeviceAudioTrack> tracksForStation(List<DeviceAudioTrack> tracks, MusicStation station) {
        List<DeviceAudioTrack> selected = new ArrayList<>();
        if (tracks == null || station == null) {
            return selected;
        }
        for (DeviceAudioTrack track : tracks) {
            if (station.mixType() == MusicStation.MixType.ALL
                    || (station.mixType() == MusicStation.MixType.ARTIST && station.mixValue().equals(representativeArtist(track)))
                    || (station.mixType() == MusicStation.MixType.FOLDER && station.mixValue().equals(track.folder()))
                    || (station.mixType() == MusicStation.MixType.TRACK && station.mixValue().equals(Long.toString(track.id())))) {
                selected.add(track);
            }
        }
        Collections.shuffle(selected);
        return selected;
    }

    public static String representativeArtist(DeviceAudioTrack track) {
        return representativeArtist(track == null ? null : track.artist());
    }

    public static String representativeArtist(String artist) {
        String clean = artist == null || artist.trim().isEmpty() ? "알 수 없는 아티스트" : artist.trim();
        String[] parts = clean.split("(?i)\\s*(?:,|，|、|;|；|\\||\\s+/\\s+|\\s+w\\.?\\s+|\\s+w\\s*/\\s*|\\s+feat\\.?\\s+|\\s+ft\\.?\\s+|\\s+featuring\\s+|\\s+with\\s+|\\s+[x×&]\\s+)\\s*");
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                return part.trim();
            }
        }
        return clean;
    }

    public static String folderLabelFromPath(String relativePath, String fallback) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return fallback == null || fallback.trim().isEmpty() ? "알 수 없는 폴더" : fallback.trim();
        }
        String clean = relativePath.trim();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        int slash = clean.lastIndexOf('/');
        return slash > -1 && slash < clean.length() - 1 ? clean.substring(slash + 1) : clean;
    }

    public static boolean isLongSinglePlaylistTrack(DeviceAudioTrack track) {
        if (track == null || track.durationMs() < LONG_PLAYLIST_MIN_DURATION_MS) {
            return false;
        }
        return hasPlaylistMarker(track.title())
                || hasPlaylistMarker(track.displayName())
                || hasPlaylistMarker(track.album())
                || hasPlaylistMarker(track.folder());
    }

    public static String playlistTitle(DeviceAudioTrack track) {
        if (track == null) {
            return "긴 플레이리스트";
        }
        String title = cleanPlaylistTitle(track.title());
        if (title.isEmpty() || "제목 없음".equals(title)) {
            title = cleanPlaylistTitle(track.displayName());
        }
        return title.isEmpty() ? "긴 플레이리스트" : title;
    }

    public static String cleanPlaylistTitle(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String text = value.trim();
        int dot = text.lastIndexOf('.');
        if (dot > 0 && dot < text.length() - 1) {
            String extension = text.substring(dot + 1).toLowerCase(Locale.ROOT);
            if ("m4a".equals(extension) || "mp3".equals(extension) || "opus".equals(extension)
                    || "ogg".equals(extension) || "webm".equals(extension) || "wav".equals(extension)) {
                text = text.substring(0, dot);
            }
        }
        text = text.replaceAll("(?i)^\\s*[\\[(（【]\\s*playlist\\s*[\\])）】]\\s*", "");
        text = text.replaceAll("(?i)^\\s*playlist\\s*([|:：\\-–—]+)\\s*", "");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    private static boolean hasPlaylistMarker(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.matches(".*(^|[\\s\\[({【|:：\\-–—])playlist([\\s\\])}】|:：\\-–—]|$).*")
                || normalized.contains("플레이리스트");
    }

    public static String formatDuration(long durationMs) {
        if (durationMs <= 0) {
            return "--:--";
        }
        long totalSeconds = durationMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        if (unit == 0) {
            return String.format(Locale.ROOT, "%.0f %s", value, units[unit]);
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }
}
