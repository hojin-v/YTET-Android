package com.ytet.android.stream;

public final class OnlineStreamVideoSort {
    private OnlineStreamVideoSort() {
    }

    public static int comparePopular(OnlineStreamVideo first, OnlineStreamVideo second) {
        int viewCompare = Long.compare(second.viewCount(), first.viewCount());
        if (viewCompare != 0) {
            return viewCompare;
        }
        return compareLatest(first, second);
    }

    public static int compareLatest(OnlineStreamVideo first, OnlineStreamVideo second) {
        if (first.publishedRank() > 0L || second.publishedRank() > 0L) {
            int publishedCompare = Long.compare(second.publishedRank(), first.publishedRank());
            if (publishedCompare != 0) {
                return publishedCompare;
            }
        }
        return Integer.compare(first.sourceIndex(), second.sourceIndex());
    }

    public static int compareOldest(OnlineStreamVideo first, OnlineStreamVideo second) {
        if (first.publishedRank() > 0L && second.publishedRank() > 0L) {
            int publishedCompare = Long.compare(first.publishedRank(), second.publishedRank());
            if (publishedCompare != 0) {
                return publishedCompare;
            }
        } else if (first.publishedRank() > 0L) {
            return -1;
        } else if (second.publishedRank() > 0L) {
            return 1;
        }
        return Integer.compare(second.sourceIndex(), first.sourceIndex());
    }
}
