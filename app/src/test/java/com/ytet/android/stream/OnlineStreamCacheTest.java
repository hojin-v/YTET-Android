package com.ytet.android.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class OnlineStreamCacheTest {
    @Test
    public void selectDisplayVideosKeepsDisplayCountBelowLimit() {
        List<OnlineStreamVideo> videos = new ArrayList<>();
        for (int index = 0; index < 60; index++) {
            videos.add(video("video-" + index, 60_000L - index, 20260601L - index));
        }

        List<OnlineStreamVideo> display = OnlineStreamCache.selectDisplayVideos(
                videos,
                40,
                1_800_000_000_000L,
                new Random(7)
        );

        assertEquals(40, display.size());
        assertTrue(containsVideo(display, "video-0"));
    }

    @Test
    public void selectDisplayVideosDeduplicatesByWatchUrl() {
        List<OnlineStreamVideo> videos = new ArrayList<>();
        videos.add(video("first", "same", 100L, 20260620L));
        videos.add(video("second", "same", 200L, 20260619L));
        videos.add(video("third", "third", 300L, 20260618L));

        List<OnlineStreamVideo> display = OnlineStreamCache.selectDisplayVideos(
                videos,
                40,
                1_800_000_000_000L,
                new Random(7)
        );

        assertEquals(2, display.size());
    }

    @Test
    public void selectDisplayVideosHandlesBlankOptionalFields() {
        List<OnlineStreamVideo> display = OnlineStreamCache.selectDisplayVideos(
                List.of(new OnlineStreamVideo("", "", "", "", "", 0L, 0L, 0L, 0, 0)),
                40,
                1_800_000_000_000L,
                new Random(7)
        );

        assertEquals(1, display.size());
    }

    @Test
    public void selectDisplaySectionsCapsDirectNetworkFallback() {
        List<OnlineStreamVideo> videos = new ArrayList<>();
        for (int index = 0; index < 60; index++) {
            videos.add(video("network-" + index, 60_000L - index, 20260601L - index));
        }

        List<OnlineStreamSection> sections = OnlineStreamCache.selectDisplaySections(
                List.of(section(videos)),
                40
        );

        assertEquals(1, sections.size());
        assertEquals(40, sections.get(0).videos().size());
    }

    @Test
    public void selectDisplayVideosKeepsTheChannelRankOfEachVideo() {
        List<OnlineStreamVideo> videos = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            videos.add(new OnlineStreamVideo(
                    "video-" + index,
                    "video-" + index,
                    "Channel",
                    "https://www.youtube.com/watch?v=video-" + index,
                    "",
                    180_000L,
                    100L + index,
                    0L,
                    index,
                    0
            ));
        }

        List<OnlineStreamVideo> display = OnlineStreamCache.selectDisplayVideos(
                videos,
                6,
                1_800_000_000_000L,
                new Random(11)
        );

        assertEquals(6, display.size());
        for (OnlineStreamVideo video : display) {
            int expectedRank = Integer.parseInt(video.id().substring("video-".length()));
            assertEquals(expectedRank, video.sourceIndex());
        }
    }

    @Test
    public void selectDisplayVideosPicksTheNewestChannelVideosWithoutUploadDates() {
        List<OnlineStreamVideo> videos = new ArrayList<>();
        for (int index = 0; index < 40; index++) {
            videos.add(new OnlineStreamVideo(
                    "video-" + index,
                    "video-" + index,
                    "Channel",
                    "https://www.youtube.com/watch?v=video-" + index,
                    "",
                    180_000L,
                    0L,
                    0L,
                    index,
                    0
            ));
        }

        List<OnlineStreamVideo> display = OnlineStreamCache.selectDisplayVideos(
                videos,
                8,
                1_800_000_000_000L,
                new Random(3)
        );

        assertTrue(containsVideo(display, "video-0"));
    }

    private static boolean containsVideo(List<OnlineStreamVideo> videos, String id) {
        for (OnlineStreamVideo video : videos) {
            if (id.equals(video.id())) {
                return true;
            }
        }
        return false;
    }

    private static OnlineStreamVideo video(String id, long views, long published) {
        return video(id, id, views, published);
    }

    private static OnlineStreamVideo video(String id, String urlId, long views, long published) {
        return new OnlineStreamVideo(
                id,
                id,
                "Channel",
                "https://www.youtube.com/watch?v=" + urlId,
                "https://example.test/" + id + ".jpg",
                180_000L,
                views,
                published,
                0,
                0
        );
    }

    private static OnlineStreamSection section(List<OnlineStreamVideo> videos) {
        return new OnlineStreamSection(
                "channel",
                "Channel",
                "https://www.youtube.com/@channel",
                "https://example.test/avatar.jpg",
                videos
        );
    }
}
