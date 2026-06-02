package com.ytet.android.stream;

import com.ytet.android.library.DeviceAudioTrack;
import com.ytet.android.library.MusicLibrary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StationCatalog {
    private static final int[] ACCENTS = {
            0xFFB84D5A,
            0xFFD08A2E,
            0xFF48A078,
            0xFF7D5CDB,
            0xFF3F7AD8
    };

    private StationCatalog() {
    }

    public static List<MusicStation> recommendedStations(List<DeviceAudioTrack> tracks) {
        List<MusicStation> stations = new ArrayList<>();
        if (tracks == null || tracks.isEmpty()) {
            return stations;
        }

        stations.add(new MusicStation(
                "library-shuffle",
                "내 라이브러리 셔플",
                "스트리밍",
                tracks.size() + "곡 전체 셔플",
                "디바이스에 저장된 모든 음악을 기준으로 만든 셔플입니다.",
                MusicStation.MixType.ALL,
                "",
                ACCENTS[0]
        ));

        addTopArtistStations(stations, tracks);
        addTopFolderStations(stations, tracks);
        return stations;
    }

    private static void addTopArtistStations(List<MusicStation> stations, List<DeviceAudioTrack> tracks) {
        Map<String, Integer> counts = countByArtist(tracks);
        for (String artist : topKeys(counts, 2)) {
            int count = counts.get(artist);
            stations.add(new MusicStation(
                    "artist-" + stableId(artist),
                    artist + " 믹스",
                    "아티스트 믹스",
                    count + "곡 기반 셔플",
                    "메타데이터의 아티스트 값이 " + artist + "인 곡만 묶은 셔플입니다.",
                    MusicStation.MixType.ARTIST,
                    artist,
                    ACCENTS[stations.size() % ACCENTS.length]
            ));
        }
    }

    private static void addTopFolderStations(List<MusicStation> stations, List<DeviceAudioTrack> tracks) {
        Map<String, Integer> counts = countByFolder(tracks);
        for (String folder : topKeys(counts, 2)) {
            if (MusicLibrary.ALL_FOLDERS.equals(folder)) {
                continue;
            }
            int count = counts.get(folder);
            stations.add(new MusicStation(
                    "folder-" + stableId(folder),
                    folder + " 믹스",
                    "폴더 믹스",
                    count + "곡 기반 셔플",
                    "디바이스 폴더명이 " + folder + "인 곡만 묶은 셔플입니다.",
                    MusicStation.MixType.FOLDER,
                    folder,
                    ACCENTS[stations.size() % ACCENTS.length]
            ));
        }
    }

    private static Map<String, Integer> countByArtist(List<DeviceAudioTrack> tracks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DeviceAudioTrack track : tracks) {
            String artist = track.artist();
            if ("알 수 없는 아티스트".equals(artist)) {
                continue;
            }
            counts.put(artist, counts.getOrDefault(artist, 0) + 1);
        }
        return counts;
    }

    private static Map<String, Integer> countByFolder(List<DeviceAudioTrack> tracks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DeviceAudioTrack track : tracks) {
            counts.put(track.folder(), counts.getOrDefault(track.folder(), 0) + 1);
        }
        return counts;
    }

    private static List<String> topKeys(Map<String, Integer> counts, int limit) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((first, second) -> Integer.compare(second.getValue(), first.getValue()));
        List<String> keys = new ArrayList<>();
        int count = Math.min(limit, entries.size());
        for (int index = 0; index < count; index++) {
            keys.add(entries.get(index).getKey());
        }
        return keys;
    }

    private static String stableId(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]+", "-");
        String trimmed = normalized.replaceAll("^-+|-+$", "");
        return trimmed.isEmpty() ? Integer.toHexString(value.hashCode()) : trimmed;
    }
}
