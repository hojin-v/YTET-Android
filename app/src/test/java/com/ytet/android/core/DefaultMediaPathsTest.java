package com.ytet.android.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DefaultMediaPathsTest {
    private static final String MUSIC_TREE_URI =
            "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FYTET%2FMusic";
    private static final String MUSIC_DOCUMENT_URI =
            "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FYTET%2FMusic"
                    + "/document/primary%3ADownload%2FYTET%2FMusic%2FSong.m4a";

    @Test
    public void formatsDefaultRelativePaths() {
        assertEquals("Download/YTET/", DefaultMediaPaths.rootRelativePath());
        assertEquals("Download/YTET/Music/", DefaultMediaPaths.musicRelativePath());
        assertEquals("Download/YTET/Video/", DefaultMediaPaths.videoRelativePath());
        assertEquals("/Download/YTET/Music/", DefaultMediaPaths.displayPath(MediaType.AUDIO));
    }

    @Test
    public void readsPrimaryExternalStorageTreeUris() {
        assertEquals(
                "Download/YTET/Music",
                DefaultMediaPaths.primaryExternalStorageRelativePathFromTreeUri(MUSIC_TREE_URI)
        );
        assertEquals("/Download/YTET/Music", DefaultMediaPaths.displayTreePath(MUSIC_TREE_URI));
    }

    @Test
    public void readsPrimaryExternalStorageDocumentUris() {
        assertEquals(
                "Download/YTET/Music/Song.m4a",
                DefaultMediaPaths.primaryExternalStorageRelativePathFromDocumentUri(MUSIC_DOCUMENT_URI)
        );
    }

    @Test
    public void detectsDefaultTreeUrisForCurrentMediaType() {
        assertTrue(DefaultMediaPaths.isDefaultTreeUriFor(MediaType.AUDIO, MUSIC_TREE_URI));
        assertFalse(DefaultMediaPaths.isDefaultTreeUriFor(MediaType.VIDEO, MUSIC_TREE_URI));
    }
}
