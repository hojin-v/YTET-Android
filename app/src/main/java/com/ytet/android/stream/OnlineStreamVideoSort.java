package com.ytet.android.stream;

/**
 * Total orderings for stream videos.
 *
 * <p>Every comparison uses a fixed lexicographic key so the orderings stay transitive even when
 * only part of the pool carries publish dates or view counts. Mixing "compare by date when both
 * have one, otherwise by channel rank" would break the comparator contract and crash sorting of
 * larger channel pools.</p>
 */
public final class OnlineStreamVideoSort {
    private OnlineStreamVideoSort() {
    }

    /**
     * YouTube's own popular ordering first, then the most viewed rest.
     *
     * <p>Videos listed on the channel's popular tab keep that tab's ranking; everything else falls
     * back to the view count.</p>
     */
    public static int comparePopular(OnlineStreamVideo first, OnlineStreamVideo second) {
        int missingRank = Integer.compare(rankGroup(first.popularRank()), rankGroup(second.popularRank()));
        if (missingRank != 0) {
            return missingRank;
        }
        if (first.popularRank() > 0 && second.popularRank() > 0) {
            int rankCompare = Integer.compare(first.popularRank(), second.popularRank());
            if (rankCompare != 0) {
                return rankCompare;
            }
        }
        return compareMostViewed(first, second);
    }

    /** Most viewed first. Videos without a view count keep the latest ordering after them. */
    public static int compareMostViewed(OnlineStreamVideo first, OnlineStreamVideo second) {
        int missingViews = Integer.compare(rankGroup(first.viewCount()), rankGroup(second.viewCount()));
        if (missingViews != 0) {
            return missingViews;
        }
        int viewCompare = Long.compare(second.viewCount(), first.viewCount());
        if (viewCompare != 0) {
            return viewCompare;
        }
        return compareLatest(first, second);
    }

    /** Newest first. Dated videos come first, undated ones follow in channel order. */
    public static int compareLatest(OnlineStreamVideo first, OnlineStreamVideo second) {
        int missingDate = Integer.compare(rankGroup(first.publishedRank()), rankGroup(second.publishedRank()));
        if (missingDate != 0) {
            return missingDate;
        }
        int publishedCompare = Long.compare(second.publishedRank(), first.publishedRank());
        if (publishedCompare != 0) {
            return publishedCompare;
        }
        int sourceCompare = Integer.compare(first.sourceIndex(), second.sourceIndex());
        return sourceCompare != 0 ? sourceCompare : compareIdentity(first, second);
    }

    /** Oldest first. Dated videos come first, undated ones follow in reverse channel order. */
    public static int compareOldest(OnlineStreamVideo first, OnlineStreamVideo second) {
        int missingDate = Integer.compare(rankGroup(first.publishedRank()), rankGroup(second.publishedRank()));
        if (missingDate != 0) {
            return missingDate;
        }
        int publishedCompare = Long.compare(first.publishedRank(), second.publishedRank());
        if (publishedCompare != 0) {
            return publishedCompare;
        }
        int sourceCompare = Integer.compare(second.sourceIndex(), first.sourceIndex());
        return sourceCompare != 0 ? sourceCompare : compareIdentity(first, second);
    }

    private static int rankGroup(long value) {
        return value > 0L ? 0 : 1;
    }

    private static int compareIdentity(OnlineStreamVideo first, OnlineStreamVideo second) {
        int urlCompare = first.watchUrl().compareTo(second.watchUrl());
        return urlCompare != 0 ? urlCompare : first.id().compareTo(second.id());
    }
}
