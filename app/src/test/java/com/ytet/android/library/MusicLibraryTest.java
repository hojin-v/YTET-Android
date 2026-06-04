package com.ytet.android.library;

import com.ytet.android.stream.MusicStation;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MusicLibraryTest {
    @Test
    public void derivesReadableFolderLabelsFromRelativePaths() {
        assertEquals("Jazz", MusicLibrary.folderLabelFromPath("Music/Jazz/", "Music"));
        assertEquals("Downloads", MusicLibrary.folderLabelFromPath("", "Downloads"));
    }

    @Test
    public void filtersTracksByFolder() {
        DeviceAudioTrack first = track(1, "Alpha", "Jazz");
        DeviceAudioTrack second = track(2, "Beta", "Downloads");

        assertEquals(2, MusicLibrary.filterByFolder(List.of(first, second), MusicLibrary.ALL_FOLDERS).size());
        assertEquals(List.of(first), MusicLibrary.filterByFolder(List.of(first, second), "Jazz"));
    }

    @Test
    public void formatsDurationAndByteCountsForFileRows() {
        assertEquals("3:05", MusicLibrary.formatDuration(185_000));
        assertEquals("1:02:03", MusicLibrary.formatDuration(3_723_000));
        assertEquals("1.5 MB", MusicLibrary.formatBytes(1_572_864));
    }

    @Test
    public void detectsAndCleansLongSinglePlaylistTitles() {
        DeviceAudioTrack track = new DeviceAudioTrack(
                1,
                "[Playlist] 퇴근 후 나만의 시간",
                "essential;",
                "Album",
                "[Playlist] 퇴근 후 나만의 시간.m4a",
                "YTET",
                "content://audio/1",
                2_807_000,
                10_000
        );

        assertTrue(MusicLibrary.isLongSinglePlaylistTrack(track));
        assertEquals("퇴근 후 나만의 시간", MusicLibrary.playlistTitle(track));
        assertEquals("퇴근 후 나만의 시간", MusicLibrary.cleanPlaylistTitle("[Playlist] 퇴근 후 나만의 시간.m4a"));
        assertEquals("퇴근 후 나만의 시간", MusicLibrary.cleanPlaylistTitle("playlist | 퇴근 후 나만의 시간"));
    }

    @Test
    public void selectsStationQueuesFromExactLocalFields() {
        DeviceAudioTrack first = track(1, "Alpha", "Artist A", "Camera");
        DeviceAudioTrack second = track(2, "Beta", "Artist A", "Downloads");
        DeviceAudioTrack third = track(3, "Gamma", "Artist B", "Camera");

        MusicStation artist = station(MusicStation.MixType.ARTIST, "Artist A");
        MusicStation folder = station(MusicStation.MixType.FOLDER, "Camera");
        MusicStation trackStation = station(MusicStation.MixType.TRACK, "2");
        MusicStation all = station(MusicStation.MixType.ALL, "");

        List<DeviceAudioTrack> artistTracks = MusicLibrary.tracksForStation(List.of(first, second, third), artist);
        List<DeviceAudioTrack> folderTracks = MusicLibrary.tracksForStation(List.of(first, second, third), folder);
        List<DeviceAudioTrack> trackQueue = MusicLibrary.tracksForStation(List.of(first, second, third), trackStation);

        assertEquals(2, artistTracks.size());
        assertTrue(artistTracks.stream().allMatch(track -> "Artist A".equals(track.artist())));
        assertEquals(2, folderTracks.size());
        assertTrue(folderTracks.stream().allMatch(track -> "Camera".equals(track.folder())));
        assertEquals(List.of(second), trackQueue);
        assertEquals(3, MusicLibrary.tracksForStation(List.of(first, second, third), all).size());
    }

    @Test
    public void usesRepresentativeArtistForArtistStations() {
        DeviceAudioTrack first = track(1, "Alpha", "Artist A, Guest One", "Camera");
        DeviceAudioTrack second = track(2, "Beta", "Artist A ft Guest Two", "Downloads");
        DeviceAudioTrack third = track(3, "Gamma", "Artist A feat Guest Three", "Camera");
        DeviceAudioTrack fourth = track(4, "Delta", "Artist A featuring Guest Four", "Downloads");
        DeviceAudioTrack fifth = track(5, "Epsilon", "Artist A w. Guest Five", "Camera");
        DeviceAudioTrack sixth = track(6, "Zeta", "Artist A w/ Guest Six", "Downloads");
        DeviceAudioTrack seventh = track(7, "Eta", "Artist B", "Camera");

        MusicStation artist = station(MusicStation.MixType.ARTIST, "Artist A");
        List<DeviceAudioTrack> artistTracks = MusicLibrary.tracksForStation(List.of(first, second, third, fourth, fifth, sixth, seventh), artist);

        assertEquals(6, artistTracks.size());
        assertEquals("Artist A", MusicLibrary.representativeArtist(first));
        assertEquals("Artist A feat. Guest Two", second.artist());
        assertEquals("Artist A feat. Guest Three", third.artist());
        assertEquals("Artist A feat. Guest Four", fourth.artist());
        assertEquals("Artist A with. Guest Five", fifth.artist());
        assertEquals("Artist A with. Guest Six", sixth.artist());
        assertEquals("Artist A", MusicLibrary.representativeArtist(second));
        assertEquals("Artist A", MusicLibrary.representativeArtist(third));
        assertEquals("Artist A", MusicLibrary.representativeArtist(fourth));
        assertEquals("Artist A", MusicLibrary.representativeArtist(fifth));
        assertEquals("Artist A", MusicLibrary.representativeArtist(sixth));
    }

    private DeviceAudioTrack track(long id, String title, String folder) {
        return track(id, title, "Artist", folder);
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

    private MusicStation station(MusicStation.MixType mixType, String mixValue) {
        return new MusicStation(
                "test",
                "Test",
                "Test",
                "Test",
                "Test",
                mixType,
                mixValue,
                0
        );
    }
}
