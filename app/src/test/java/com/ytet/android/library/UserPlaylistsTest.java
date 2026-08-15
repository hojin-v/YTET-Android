package com.ytet.android.library;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class UserPlaylistsTest {
    private static final String FIELD_SEPARATOR = "\u001f";

    @Test
    public void streamEntriesSurviveASaveAndLoadRoundTrip() {
        List<UserPlaylists.Entry> entries = new ArrayList<>();
        entries.add(new UserPlaylists.Entry(
                -4_211L,
                "https://www.youtube.com/watch?v=abc123",
                "밤 산책",
                "essential;",
                "essential;",
                "https://i.ytimg.com/vi/abc123/hq720.jpg",
                214_000L
        ));
        entries.add(new UserPlaylists.Entry(77L, "content://media/external/audio/media/77", "로컬 곡", "가수", "앨범", "", 180_000L));

        List<UserPlaylists.Entry> parsed = UserPlaylists.parseEntries(UserPlaylists.serializeEntries(entries));

        assertEquals(2, parsed.size());
        assertTrue(parsed.get(0).online());
        assertEquals("https://www.youtube.com/watch?v=abc123", parsed.get(0).uri());
        assertEquals("밤 산책", parsed.get(0).title());
        assertEquals(214_000L, parsed.get(0).durationMs());
        assertFalse(parsed.get(1).online());
        assertEquals(77L, parsed.get(1).trackId());
    }

    @Test
    public void streamEntriesAreValidWithoutAMediaStoreId() {
        UserPlaylists.Entry stream = new UserPlaylists.Entry(
                -1L,
                "https://www.youtube.com/watch?v=xyz",
                "스트림",
                "채널",
                "채널",
                "",
                0L
        );
        UserPlaylists.Entry nothing = new UserPlaylists.Entry(0L, "", "", "", "", "", 0L);

        assertTrue(stream.valid());
        assertFalse(nothing.valid());
        assertEquals(0, UserPlaylists.parseEntries(UserPlaylists.serializeEntries(List.of(nothing))).size());
    }

    @Test
    public void duplicateEntriesAreCollapsedByKey() {
        UserPlaylists.Entry first = new UserPlaylists.Entry(5L, "content://media/5", "곡", "가수", "앨범", "", 1_000L);
        UserPlaylists.Entry sameLocal = new UserPlaylists.Entry(5L, "content://media/5", "곡 (수정)", "가수", "앨범", "", 1_000L);
        UserPlaylists.Entry stream = new UserPlaylists.Entry(-2L, "https://youtu.be/a", "스트림", "채널", "채널", "", 0L);
        UserPlaylists.Entry sameStream = new UserPlaylists.Entry(-3L, "https://youtu.be/a", "스트림", "채널", "채널", "", 0L);

        UserPlaylists.Playlist playlist = new UserPlaylists.Playlist(
                "pl_1",
                "테스트",
                List.of(first, sameLocal, stream, sameStream),
                1L
        );

        assertEquals(2, playlist.size());
        assertTrue(playlist.contains(sameStream));
        assertEquals(1, playlist.localTrackIds().size());
    }

    @Test
    public void legacyTrackIdListsBecomeLocalEntries() {
        List<UserPlaylists.Entry> entries = UserPlaylists.legacyEntries("12, 34,0,-8, 56");

        assertEquals(3, entries.size());
        assertEquals(12L, entries.get(0).trackId());
        assertEquals(34L, entries.get(1).trackId());
        assertEquals(56L, entries.get(2).trackId());
        assertFalse(entries.get(0).online());
    }

    @Test
    public void separatorsInsideMetadataDoNotBreakParsing() {
        UserPlaylists.Entry entry = new UserPlaylists.Entry(
                9L,
                "content://media/9",
                "제목\n두번째 줄",
                "가수" + FIELD_SEPARATOR + "이상한값",
                "앨범",
                "",
                5_000L
        );

        List<UserPlaylists.Entry> parsed = UserPlaylists.parseEntries(UserPlaylists.serializeEntries(List.of(entry)));

        assertEquals(1, parsed.size());
        assertEquals(9L, parsed.get(0).trackId());
        assertEquals("앨범", parsed.get(0).album());
        assertEquals(5_000L, parsed.get(0).durationMs());
    }
}
