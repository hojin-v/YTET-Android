package com.ytet.android.stream;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class OnlineStreamVideoSortTest {
    @Test
    public void popularSortFallsBackToLatestSortWhenViewsAreMissing() {
        OnlineStreamVideo oldPopular = video("old-popular", 0L, 20250101L, 8, 1);
        OnlineStreamVideo newLatest = video("new-latest", 0L, 20260620L, 0, 9);

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
    public void popularSortUsesViewCountWithoutPopularRankFallback() {
        OnlineStreamVideo highViews = video("high-views", 1000L, 20250101L, 4, 9);
        OnlineStreamVideo lowViewsRanked = video("low-views-ranked", 10L, 20260620L, 0, 1);

        List<OnlineStreamVideo> videos = new ArrayList<>();
        videos.add(lowViewsRanked);
        videos.add(highViews);
        videos.sort(OnlineStreamVideoSort::comparePopular);

        assertEquals("high-views", videos.get(0).id());
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
