package com.ytet.android.stream;

import com.ytet.android.library.DeviceAudioTrack;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StationCatalogTest {
    @Test
    public void recommendsOnlyLocalMetadataBackedMixes() {
        List<DeviceAudioTrack> tracks = List.of(
                track(1, "Alpha", "Artist One", "Camera"),
                track(2, "Beta", "Artist One", "Camera"),
                track(3, "Gamma", "Artist Two", "Downloads"),
                track(4, "Delta", "알 수 없는 아티스트", "Downloads")
        );

        List<MusicStation> stations = StationCatalog.recommendedStations(tracks);

        assertEquals("library-shuffle", stations.get(0).id());
        assertTrue(stations.stream().anyMatch(MusicStation::isArtistMix));
        assertTrue(stations.stream().anyMatch(MusicStation::isFolderMix));
        assertTrue(stations.stream().anyMatch(station ->
                station.mixType() == MusicStation.MixType.ARTIST
                        && "Artist One".equals(station.mixValue())
                        && station.description().contains("Artist One")
        ));
        assertTrue(stations.stream().anyMatch(station ->
                station.mixType() == MusicStation.MixType.FOLDER
                        && "Camera".equals(station.mixValue())
                        && station.description().contains("Camera")
        ));
        assertFalse(stations.stream().anyMatch(station -> station.title().contains("밤")
                || station.title().contains("힙합")
                || station.description().contains("차분")));
    }

    @Test
    public void stationIdsAreUnique() {
        List<MusicStation> stations = StationCatalog.recommendedStations(List.of(
                track(1, "Alpha", "Artist One", "Camera"),
                track(2, "Beta", "Artist One", "Camera"),
                track(3, "Gamma", "Artist Two", "Downloads")
        ));
        Set<String> ids = new HashSet<>();

        for (MusicStation station : stations) {
            assertFalse(ids.contains(station.id()));
            ids.add(station.id());
        }
    }

    @Test
    public void recommendsLongSinglePlaylistTracksFromLocalFiles() {
        DeviceAudioTrack playlist = new DeviceAudioTrack(
                10,
                "[Playlist] 퇴근 후 나만의 시간",
                "essential;",
                "Album",
                "[Playlist] 퇴근 후 나만의 시간.m4a",
                "RabbYT",
                "content://audio/10",
                2_807_000,
                10_000
        );

        List<MusicStation> stations = StationCatalog.recommendedStations(List.of(
                playlist,
                track(1, "Alpha", "Artist One", "Camera")
        ));

        assertTrue(stations.stream().anyMatch(station ->
                station.isPlaylist()
                        && station.mixType() == MusicStation.MixType.TRACK
                        && "10".equals(station.mixValue())
                        && "퇴근 후 나만의 시간".equals(station.title())
        ));
    }

    @Test
    public void returnsNoStationsForEmptyLibrary() {
        assertTrue(StationCatalog.recommendedStations(List.of()).isEmpty());
    }

    private DeviceAudioTrack track(long id, String title, String artist, String folder) {
        return new DeviceAudioTrack(
                id,
                title,
                artist,
                "Album",
                title + ".mp3",
                folder,
                "content://audio/" + id,
                1000,
                1024
        );
    }
}
