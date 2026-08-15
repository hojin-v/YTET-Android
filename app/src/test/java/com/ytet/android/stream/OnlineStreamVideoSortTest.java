package com.ytet.android.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class OnlineStreamVideoSortTest {
    @Test
    public void popularSortFallsBackToLatestSortWhenViewsAreMissing() {
        OnlineStreamVideo oldPopular = video("old-popular", 0L, 20250101L, 8, 0);
        OnlineStreamVideo newLatest = video("new-latest", 0L, 20260620L, 0, 0);

        List<OnlineStreamVideo> popular = new ArrayList<>();
        popular.add(newLatest);
        popular.add(oldPopular);
        popular.sort(OnlineStreamVideoSort::comparePopular);

        List<OnlineStreamVideo> latest = new ArrayList<>();
        latest.add(oldPopular);
        latest.add(newLatest);
        latest.sort(OnlineStreamVideoSort::compareLatest);

        assertEquals("new-latest", popular.get(0).id());
        assertEquals("new-latest", latest.get(0).id());
    }

    @Test
    public void popularSortFollowsTheChannelPopularTabRanking() {
        OnlineStreamVideo highViews = video("high-views", 1000L, 20250101L, 4, 9);
        OnlineStreamVideo topRanked = video("top-ranked", 10L, 20260620L, 0, 1);

        List<OnlineStreamVideo> videos = new ArrayList<>();
        videos.add(highViews);
        videos.add(topRanked);
        videos.sort(OnlineStreamVideoSort::comparePopular);

        assertEquals("top-ranked", videos.get(0).id());
    }

    @Test
    public void viewSortIgnoresThePopularTabRankAndUsesViewCounts() {
        OnlineStreamVideo highViews = video("high-views", 1000L, 20250101L, 4, 9);
        OnlineStreamVideo lowViewsRanked = video("low-views-ranked", 10L, 20260620L, 0, 1);

        List<OnlineStreamVideo> videos = new ArrayList<>();
        videos.add(lowViewsRanked);
        videos.add(highViews);
        videos.sort(OnlineStreamVideoSort::compareMostViewed);

        assertEquals("high-views", videos.get(0).id());
    }

    @Test
    public void unrankedButPopularVideosStillSortByViews() {
        OnlineStreamVideo unrankedHigh = video("unranked-high", 5_000L, 20250101L, 6, 0);
        OnlineStreamVideo unrankedLow = video("unranked-low", 50L, 20260101L, 1, 0);

        List<OnlineStreamVideo> videos = new ArrayList<>();
        videos.add(unrankedLow);
        videos.add(unrankedHigh);
        videos.sort(OnlineStreamVideoSort::comparePopular);

        assertEquals("unranked-high", videos.get(0).id());
    }

    @Test
    public void popularSortKeepsCountedVideosAheadOfUncountedOnes() {
        OnlineStreamVideo counted = video("counted", 5L, 20200101L, 40, 0);
        OnlineStreamVideo uncounted = video("uncounted", 0L, 20260620L, 0, 0);

        List<OnlineStreamVideo> videos = new ArrayList<>();
        videos.add(uncounted);
        videos.add(counted);
        videos.sort(OnlineStreamVideoSort::comparePopular);

        assertEquals("counted", videos.get(0).id());
    }

    @Test
    public void latestAndOldestSortUseChannelOrderWhenNoVideoHasADate() {
        OnlineStreamVideo newest = video("newest", 100L, 0L, 0, 0);
        OnlineStreamVideo middle = video("middle", 900L, 0L, 1, 0);
        OnlineStreamVideo oldest = video("oldest", 500L, 0L, 2, 0);

        List<OnlineStreamVideo> latest = new ArrayList<>();
        latest.add(oldest);
        latest.add(newest);
        latest.add(middle);
        latest.sort(OnlineStreamVideoSort::compareLatest);
        assertEquals("newest", latest.get(0).id());
        assertEquals("middle", latest.get(1).id());
        assertEquals("oldest", latest.get(2).id());

        List<OnlineStreamVideo> reversed = new ArrayList<>(latest);
        reversed.sort(OnlineStreamVideoSort::compareOldest);
        assertEquals("oldest", reversed.get(0).id());
        assertEquals("middle", reversed.get(1).id());
        assertEquals("newest", reversed.get(2).id());
    }

    @Test
    public void oldestSortOrdersDatedVideosFromTheOldestUpload() {
        OnlineStreamVideo first = video("2024", 10L, 20240301L, 30, 0);
        OnlineStreamVideo second = video("2025", 10L, 20250301L, 20, 0);
        OnlineStreamVideo third = video("2026", 10L, 20260301L, 10, 0);

        List<OnlineStreamVideo> videos = new ArrayList<>();
        videos.add(third);
        videos.add(first);
        videos.add(second);
        videos.sort(OnlineStreamVideoSort::compareOldest);

        assertEquals("2024", videos.get(0).id());
        assertEquals("2025", videos.get(1).id());
        assertEquals("2026", videos.get(2).id());
    }

    /**
     * A comparator that switched between dates and channel order per pair would be intransitive and
     * make {@link List#sort} throw on larger channel pools.
     */
    @Test
    public void mixedDatedAndUndatedPoolsKeepAConsistentOrdering() {
        List<OnlineStreamVideo> videos = new ArrayList<>();
        for (int index = 0; index < 80; index++) {
            long published = index % 3 == 0 ? 0L : 20260101L + (index % 28);
            videos.add(video("video-" + index, index % 5 == 0 ? 0L : 1_000L - index, published, index, 0));
        }

        videos.sort(OnlineStreamVideoSort::compareLatest);
        videos.sort(OnlineStreamVideoSort::compareOldest);
        videos.sort(OnlineStreamVideoSort::comparePopular);

        assertEquals(80, videos.size());
    }

    private static OnlineStreamVideo video(
            String id,
            long viewCount,
            long publishedRank,
            int sourceIndex,
            int popularRank
    ) {
        return new OnlineStreamVideo(
                id,
                id,
                "channel",
                "https://www.youtube.com/watch?v=" + id,
                "",
                0L,
                viewCount,
                publishedRank,
                sourceIndex,
                popularRank
        );
    }
}
