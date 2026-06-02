package com.ytet.android.stream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class StreamUrlResolverTest {
    @Test
    public void resolvesFirstPlayableM3uEntry() {
        String playlist = "#EXTM3U\n#EXTINF:-1,Station\nhttps://example.com/live.mp3\n";

        assertEquals(
                "https://example.com/live.mp3",
                StreamUrlResolver.resolveFromPlaylist("https://example.com/station.m3u", playlist)
        );
    }

    @Test
    public void resolvesPlsFileEntry() {
        String playlist = "[playlist]\nNumberOfEntries=1\nFile1=https://example.com/live-aac\nTitle1=Live\n";

        assertEquals(
                "https://example.com/live-aac",
                StreamUrlResolver.resolveFromPlaylist("https://example.com/station.pls", playlist)
        );
    }

    @Test
    public void fallsBackToOriginalWhenPlaylistHasNoNetworkEntry() {
        assertEquals(
                "https://example.com/station.m3u",
                StreamUrlResolver.resolveFromPlaylist("https://example.com/station.m3u", "#EXTM3U\n")
        );
    }

    @Test
    public void leavesHlsPlaylistsForMediaPlayer() throws Exception {
        assertEquals(
                "https://example.com/live.m3u8",
                StreamUrlResolver.resolve("https://example.com/live.m3u8")
        );
    }
}
