package com.ytet.android.playback;

import android.content.Context;
import android.content.SharedPreferences;

public final class PlaybackStats {
    private static final String PREFS = "ytet_android";
    private static final String PLAY_COUNT_PREFIX = "play_count_";

    private PlaybackStats() {
    }

    public static void recordTrackStarted(Context context, long trackId) {
        if (context == null || trackId <= 0L) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = playCountKey(trackId);
        int count = prefs.getInt(key, 0);
        if (count < Integer.MAX_VALUE) {
            prefs.edit().putInt(key, count + 1).apply();
        }
    }

    public static int playCount(Context context, long trackId) {
        if (context == null || trackId <= 0L) {
            return 0;
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(playCountKey(trackId), 0);
    }

    private static String playCountKey(long trackId) {
        return PLAY_COUNT_PREFIX + trackId;
    }
}
