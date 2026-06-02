package com.ytet.android.library;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

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

    private DeviceAudioTrack track(long id, String title, String folder) {
        return new DeviceAudioTrack(
                id,
                title,
                "Artist",
                "Album",
                title + ".mp3",
                folder,
                "content://audio/" + id,
                1000,
                1024
        );
    }
}
