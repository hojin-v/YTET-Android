package com.ytet.android.core;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DefaultMediaPathsTest {
    private static final String MUSIC_TREE_URI =
            "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FRabbYT%2FMusic";
    private static final String MUSIC_DOCUMENT_URI =
            "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FRabbYT%2FMusic"
                    + "/document/primary%3ADownload%2FRabbYT%2FMusic%2FSong.m4a";
    private static final String LEGACY_MUSIC_TREE_URI =
            "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FYTET%2FMusic";

    @Test
    public void formatsDefaultRelativePaths() {
        assertEquals("Download/RabbYT/", DefaultMediaPaths.rootRelativePath());
        assertEquals("Download/RabbYT/Music/", DefaultMediaPaths.musicRelativePath());
        assertEquals("Download/RabbYT/Video/", DefaultMediaPaths.videoRelativePath());
        assertEquals("/Download/RabbYT/Music/", DefaultMediaPaths.displayPath(MediaType.AUDIO));
    }

    @Test
    public void keepsLegacyYtetMusicPathForScanningCompatibility() {
        List<String> scanPaths = DefaultMediaPaths.musicRelativePaths();

        assertEquals("Download/RabbYT/Music/", scanPaths.get(0));
        assertEquals("Download/YTET/Music/", scanPaths.get(1));
        assertEquals("Download/RabbYT/Music/", DefaultMediaPaths.migratedRelativePath("Download/YTET/Music/"));
        assertEquals("Download/RabbYT/Music/Album/", DefaultMediaPaths.migratedRelativePath("Download/YTET/Music/Album/"));
    }

    @Test
    public void readsPrimaryExternalStorageTreeUris() {
        assertEquals(
                "Download/RabbYT/Music",
                DefaultMediaPaths.primaryExternalStorageRelativePathFromTreeUri(MUSIC_TREE_URI)
        );
        assertEquals("/Download/RabbYT/Music", DefaultMediaPaths.displayTreePath(MUSIC_TREE_URI));
    }

    @Test
    public void readsPrimaryExternalStorageDocumentUris() {
        assertEquals(
                "Download/RabbYT/Music/Song.m4a",
                DefaultMediaPaths.primaryExternalStorageRelativePathFromDocumentUri(MUSIC_DOCUMENT_URI)
        );
    }

    @Test
    public void detectsDefaultTreeUrisForCurrentMediaType() {
        assertTrue(DefaultMediaPaths.isDefaultTreeUriFor(MediaType.AUDIO, MUSIC_TREE_URI));
        assertTrue(DefaultMediaPaths.isDefaultTreeUriFor(MediaType.AUDIO, LEGACY_MUSIC_TREE_URI));
        assertFalse(DefaultMediaPaths.isDefaultTreeUriFor(MediaType.VIDEO, MUSIC_TREE_URI));
    }
}
