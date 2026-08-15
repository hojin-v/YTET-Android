package com.ytet.android.stream;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class OnlineStreamCache {
    private static final String CACHE_FILE_NAME = "online_stream_cache.json";
    private static final int CACHE_VERSION = 1;
    private static final int MAX_CACHED_VIDEOS_PER_CHANNEL = 1000;
    private static final long RECENT_DISPLAY_WINDOW_MS = 12L * 60L * 60L * 1000L;

    private OnlineStreamCache() {
    }

    public static List<OnlineStreamSection> loadDisplaySections(
            Context context,
            List<OnlineStreamChannel> channels,
            int displayCount
    ) throws Exception {
        return loadDisplaySections(cacheFile(context), channels, displayCount);
    }

    /**
     * Every cached video of one channel in channel order, newest known upload first.
     *
     * <p>Unlike {@link #loadDisplaySections} this keeps the harvested ordering instead of picking a
     * varied sample, so the channel detail screen can sort the real list.</p>
     */
    public static List<OnlineStreamVideo> loadChannelVideos(
            Context context,
            OnlineStreamChannel channel,
            int limit
    ) throws Exception {
        return loadChannelVideos(cacheFile(context), channel, limit);
    }

    public static void mergeSections(Context context, List<OnlineStreamSection> sections) throws Exception {
        mergeSections(cacheFile(context), sections);
    }

    public static List<OnlineStreamSection> selectDisplaySections(
            List<OnlineStreamSection> sections,
            int displayCount
    ) {
        long now = System.currentTimeMillis();
        Random random = new Random(now);
        List<OnlineStreamSection> result = new ArrayList<>();
        for (OnlineStreamSection section : sections == null ? new ArrayList<OnlineStreamSection>() : sections) {
            if (section == null) {
                continue;
            }
            List<OnlineStreamVideo> selected = selectDisplayVideos(section.videos(), displayCount, now, random);
            if (selected.isEmpty()) {
                continue;
            }
            result.add(new OnlineStreamSection(
                    section.channelId(),
                    section.channelTitle(),
                    section.channelUrl(),
                    section.avatarUrl(),
                    selected
            ));
        }
        return result;
    }

    static List<OnlineStreamSection> loadDisplaySections(
            File cacheFile,
            List<OnlineStreamChannel> channels,
            int displayCount
    ) throws Exception {
        CacheState state = readState(cacheFile);
        if (state.channels.isEmpty()) {
            return new ArrayList<>();
        }

        long now = System.currentTimeMillis();
        Random random = new Random(now ^ cacheFile.getAbsolutePath().hashCode());
        List<OnlineStreamSection> result = new ArrayList<>();
        for (OnlineStreamChannel channel : channels == null ? new ArrayList<OnlineStreamChannel>() : channels) {
            CachedChannel cached = state.channels.get(channel.id());
            if (cached == null || cached.videos.isEmpty()) {
                continue;
            }
            List<CachedVideo> selected = selectVideos(cached.videos, displayCount, now, random);
            if (selected.isEmpty()) {
                continue;
            }
            List<OnlineStreamVideo> videos = new ArrayList<>();
            for (CachedVideo video : selected) {
                video.lastDisplayedAt = now;
                videos.add(video.toOnlineStreamVideo(channel.title()));
            }
            result.add(new OnlineStreamSection(
                    channel.id(),
                    channel.title(),
                    channel.url(),
                    cached.avatarUrl,
                    videos
            ));
        }
        try {
            writeState(cacheFile, state);
        } catch (IOException ignored) {
        }
        return result;
    }

    static List<OnlineStreamVideo> loadChannelVideos(
            File cacheFile,
            OnlineStreamChannel channel,
            int limit
    ) throws Exception {
        List<OnlineStreamVideo> videos = new ArrayList<>();
        if (channel == null) {
            return videos;
        }
        CachedChannel cached = readState(cacheFile).channels.get(channel.id());
        if (cached == null) {
            return videos;
        }
        List<CachedVideo> ordered = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CachedVideo video : cached.videos) {
            String key = video.key();
            if (!key.isEmpty() && seen.add(key)) {
                ordered.add(video);
            }
        }
        ordered.sort(OnlineStreamCache::compareChannelOrder);
        int max = limit <= 0 ? ordered.size() : Math.min(limit, ordered.size());
        for (int index = 0; index < max; index++) {
            videos.add(ordered.get(index).toOnlineStreamVideo(channel.title()));
        }
        return videos;
    }

    private static int compareChannelOrder(CachedVideo first, CachedVideo second) {
        int missingDate = Integer.compare(first.publishedRank > 0L ? 0 : 1, second.publishedRank > 0L ? 0 : 1);
        if (missingDate != 0) {
            return missingDate;
        }
        int publishedCompare = Long.compare(second.publishedRank, first.publishedRank);
        if (publishedCompare != 0) {
            return publishedCompare;
        }
        int sourceCompare = Integer.compare(first.sourceIndex, second.sourceIndex);
        return sourceCompare != 0 ? sourceCompare : first.key().compareTo(second.key());
    }

    static void mergeSections(File cacheFile, List<OnlineStreamSection> sections) throws Exception {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        CacheState state = readState(cacheFile);
        long now = System.currentTimeMillis();
        for (OnlineStreamSection section : sections) {
            if (section == null) {
                continue;
            }
            CachedChannel cached = state.channels.get(section.channelId());
            if (cached == null) {
                cached = new CachedChannel(section.channelId(), section.channelTitle(), section.channelUrl(), section.avatarUrl());
                state.channels.put(cached.id, cached);
            }
            cached.title = clean(section.channelTitle(), cached.title);
            cached.url = clean(section.channelUrl(), cached.url);
            cached.avatarUrl = clean(section.avatarUrl(), cached.avatarUrl);
            mergeVideos(cached, section.videos(), now);
        }
        writeState(cacheFile, state);
    }

    static List<OnlineStreamVideo> selectDisplayVideos(
            List<OnlineStreamVideo> videos,
            int displayCount,
            long now,
            Random random
    ) {
        List<CachedVideo> cached = new ArrayList<>();
        for (OnlineStreamVideo video : videos == null ? new ArrayList<OnlineStreamVideo>() : videos) {
            if (video != null) {
                cached.add(CachedVideo.fromOnlineStreamVideo(video, now));
            }
        }
        List<CachedVideo> selected = selectVideos(cached, displayCount, now, random);
        List<OnlineStreamVideo> result = new ArrayList<>();
        for (CachedVideo video : selected) {
            result.add(video.toOnlineStreamVideo(""));
        }
        return result;
    }

    private static void mergeVideos(CachedChannel channel, List<OnlineStreamVideo> videos, long now) {
        Map<String, CachedVideo> byKey = new HashMap<>();
        List<CachedVideo> unique = new ArrayList<>();
        for (CachedVideo video : channel.videos) {
            String key = video.key();
            if (key.isEmpty() || byKey.containsKey(key)) {
                continue;
            }
            byKey.put(key, video);
            unique.add(video);
        }
        channel.videos = unique;

        for (OnlineStreamVideo video : videos == null ? new ArrayList<OnlineStreamVideo>() : videos) {
            if (video == null || video.watchUrl().trim().isEmpty()) {
                continue;
            }
            CachedVideo incoming = CachedVideo.fromOnlineStreamVideo(video, now);
            String key = incoming.key();
            CachedVideo existing = byKey.get(key);
            if (existing == null) {
                byKey.put(key, incoming);
                channel.videos.add(incoming);
            } else {
                existing.merge(incoming, now);
            }
        }
        pruneChannel(channel);
    }

    private static void pruneChannel(CachedChannel channel) {
        List<CachedVideo> cleaned = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CachedVideo video : channel.videos) {
            String key = video.key();
            if (key.isEmpty() || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            cleaned.add(video);
        }
        cleaned.sort((first, second) -> {
            int seenCompare = Long.compare(second.lastSeenAt, first.lastSeenAt);
            if (seenCompare != 0) {
                return seenCompare;
            }
            int viewCompare = Long.compare(second.viewCount, first.viewCount);
            if (viewCompare != 0) {
                return viewCompare;
            }
            return Long.compare(second.publishedRank, first.publishedRank);
        });
        if (cleaned.size() > MAX_CACHED_VIDEOS_PER_CHANNEL) {
            cleaned = new ArrayList<>(cleaned.subList(0, MAX_CACHED_VIDEOS_PER_CHANNEL));
        }
        channel.videos = cleaned;
    }

    private static List<CachedVideo> selectVideos(
            List<CachedVideo> videos,
            int displayCount,
            long now,
            Random random
    ) {
        List<CachedVideo> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CachedVideo video : videos == null ? new ArrayList<CachedVideo>() : videos) {
            String key = video.key();
            if (key.isEmpty() || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            candidates.add(video);
        }
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        int target = Math.max(1, Math.min(displayCount, candidates.size()));
        int popularQuota = Math.max(1, target / 4);
        int latestQuota = Math.max(1, target / 4);
        List<CachedVideo> selected = new ArrayList<>();
        Set<String> selectedKeys = new HashSet<>();

        List<CachedVideo> popular = preferredPool(candidates, now);
        popular.removeIf(video -> video.viewCount <= 0L);
        popular.sort((first, second) -> {
            int viewCompare = Long.compare(second.viewCount, first.viewCount);
            if (viewCompare != 0) {
                return viewCompare;
            }
            return Long.compare(second.publishedRank, first.publishedRank);
        });
        addVideos(selected, selectedKeys, popular, popularQuota);

        List<CachedVideo> latest = preferredPool(candidates, now);
        latest.sort(OnlineStreamCache::compareChannelOrder);
        addVideos(selected, selectedKeys, latest, latestQuota);

        List<CachedVideo> rest = new ArrayList<>(candidates);
        Collections.shuffle(rest, random);
        rest.sort(Comparator.comparingLong(video -> video.lastDisplayedAt));
        addVideos(selected, selectedKeys, rest, target - selected.size());

        Collections.shuffle(selected, random);
        return selected;
    }

    private static List<CachedVideo> preferredPool(List<CachedVideo> candidates, long now) {
        List<CachedVideo> fresh = new ArrayList<>();
        for (CachedVideo video : candidates) {
            if (video.lastDisplayedAt <= 0L || now - video.lastDisplayedAt > RECENT_DISPLAY_WINDOW_MS) {
                fresh.add(video);
            }
        }
        return fresh.size() >= Math.max(4, candidates.size() / 4) ? fresh : new ArrayList<>(candidates);
    }

    private static void addVideos(
            List<CachedVideo> selected,
            Set<String> selectedKeys,
            List<CachedVideo> source,
            int limit
    ) {
        if (limit <= 0) {
            return;
        }
        int added = 0;
        for (CachedVideo video : source) {
            String key = video.key();
            if (!key.isEmpty() && selectedKeys.add(key)) {
                selected.add(video);
                added++;
                if (added >= limit) {
                    return;
                }
            }
        }
    }

    private static CacheState readState(File file) throws Exception {
        if (file == null || !file.isFile() || file.length() <= 0L) {
            return new CacheState();
        }
        JSONObject root;
        try {
            root = new JSONObject(readText(file));
        } catch (JSONException exception) {
            return new CacheState();
        }
        CacheState state = new CacheState();
        JSONArray channels = root.optJSONArray("channels");
        if (channels == null) {
            return state;
        }
        for (int index = 0; index < channels.length(); index++) {
            CachedChannel channel = CachedChannel.fromJson(channels.optJSONObject(index));
            if (channel != null && !channel.id.isEmpty()) {
                state.channels.put(channel.id, channel);
            }
        }
        return state;
    }

    private static void writeState(File file, CacheState state) throws Exception {
        if (file == null || state == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("캐시 디렉터리를 만들 수 없습니다: " + parent);
        }
        JSONObject root = new JSONObject();
        root.put("version", CACHE_VERSION);
        JSONArray channels = new JSONArray();
        for (CachedChannel channel : state.channels.values()) {
            channels.put(channel.toJson());
        }
        root.put("channels", channels);
        File temp = new File(parent == null ? new File(".") : parent, file.getName() + ".tmp");
        writeText(temp, root.toString());
        if (file.exists() && !file.delete()) {
            throw new IOException("기존 캐시 파일을 교체할 수 없습니다: " + file);
        }
        if (!temp.renameTo(file)) {
            writeText(file, root.toString());
            temp.delete();
        }
    }

    private static File cacheFile(Context context) {
        return new File(context.getFilesDir(), CACHE_FILE_NAME);
    }

    private static String readText(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void writeText(File file, String text) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String clean(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        if (!text.isEmpty()) {
            return text;
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static long positive(long value, long fallback) {
        return value > 0L ? value : Math.max(0L, fallback);
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : Math.max(0, fallback);
    }

    private static final class CacheState {
        private final Map<String, CachedChannel> channels = new HashMap<>();
    }

    private static final class CachedChannel {
        private final String id;
        private String title;
        private String url;
        private String avatarUrl;
        private List<CachedVideo> videos;

        private CachedChannel(String id, String title, String url, String avatarUrl) {
            this.id = clean(id, "");
            this.title = clean(title, "YouTube 채널");
            this.url = clean(url, "");
            this.avatarUrl = clean(avatarUrl, "");
            this.videos = new ArrayList<>();
        }

        private static CachedChannel fromJson(JSONObject object) {
            if (object == null) {
                return null;
            }
            CachedChannel channel = new CachedChannel(
                    object.optString("id", ""),
                    object.optString("title", ""),
                    object.optString("url", ""),
                    object.optString("avatar", "")
            );
            JSONArray videos = object.optJSONArray("videos");
            if (videos != null) {
                for (int index = 0; index < videos.length(); index++) {
                    CachedVideo video = CachedVideo.fromJson(videos.optJSONObject(index));
                    if (video != null && !video.key().isEmpty()) {
                        channel.videos.add(video);
                    }
                }
            }
            return channel;
        }

        private JSONObject toJson() throws Exception {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("title", title);
            object.put("url", url);
            object.put("avatar", avatarUrl);
            JSONArray items = new JSONArray();
            for (CachedVideo video : videos) {
                items.put(video.toJson());
            }
            object.put("videos", items);
            return object;
        }
    }

    private static final class CachedVideo {
        private String id;
        private String title;
        private String channelTitle;
        private String url;
        private String thumbnail;
        private long durationMs;
        private long viewCount;
        private long publishedRank;
        private int sourceIndex;
        private int popularRank;
        private long lastSeenAt;
        private long lastDisplayedAt;

        private static CachedVideo fromOnlineStreamVideo(OnlineStreamVideo video, long now) {
            CachedVideo cached = new CachedVideo();
            cached.id = clean(video.id(), "");
            cached.title = clean(video.title(), "");
            cached.channelTitle = clean(video.channelTitle(), "");
            cached.url = clean(video.watchUrl(), "");
            cached.thumbnail = clean(video.thumbnailUrl(), "");
            cached.durationMs = Math.max(0L, video.durationMs());
            cached.viewCount = Math.max(0L, video.viewCount());
            cached.publishedRank = Math.max(0L, video.publishedRank());
            cached.sourceIndex = Math.max(0, video.sourceIndex());
            cached.popularRank = Math.max(0, video.popularRank());
            cached.lastSeenAt = now;
            cached.lastDisplayedAt = 0L;
            return cached;
        }

        private static CachedVideo fromJson(JSONObject object) {
            if (object == null) {
                return null;
            }
            CachedVideo video = new CachedVideo();
            video.id = clean(object.optString("id", ""), "");
            video.title = clean(object.optString("title", ""), "");
            video.channelTitle = clean(object.optString("channel_title", ""), "");
            video.url = clean(object.optString("url", ""), "");
            video.thumbnail = clean(object.optString("thumbnail", ""), "");
            long durationMs = object.optLong("duration_ms", 0L);
            if (durationMs <= 0L) {
                durationMs = Math.max(0L, object.optLong("duration", 0L)) * 1000L;
            }
            video.durationMs = durationMs;
            video.viewCount = Math.max(0L, object.optLong("view_count", 0L));
            video.publishedRank = Math.max(0L, object.optLong("published", 0L));
            video.sourceIndex = Math.max(0, object.optInt("source_index", 0));
            video.popularRank = Math.max(0, object.optInt("popular_rank", 0));
            video.lastSeenAt = Math.max(0L, object.optLong("last_seen_at", 0L));
            video.lastDisplayedAt = Math.max(0L, object.optLong("last_displayed_at", 0L));
            return video;
        }

        private void merge(CachedVideo incoming, long now) {
            title = clean(incoming.title, title);
            channelTitle = clean(incoming.channelTitle, channelTitle);
            thumbnail = clean(incoming.thumbnail, thumbnail);
            durationMs = positive(incoming.durationMs, durationMs);
            viewCount = positive(incoming.viewCount, viewCount);
            publishedRank = positive(incoming.publishedRank, publishedRank);
            // A fresh harvest carries the current channel position, including 0 for the newest upload.
            sourceIndex = Math.max(0, incoming.sourceIndex);
            popularRank = positive(incoming.popularRank, popularRank);
            lastSeenAt = now;
        }

        private OnlineStreamVideo toOnlineStreamVideo(String fallbackChannelTitle) {
            return new OnlineStreamVideo(
                    id,
                    title,
                    clean(channelTitle, fallbackChannelTitle),
                    url,
                    thumbnail,
                    durationMs,
                    viewCount,
                    publishedRank,
                    sourceIndex,
                    popularRank
            );
        }

        private JSONObject toJson() throws Exception {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("title", title);
            object.put("channel_title", channelTitle);
            object.put("url", url);
            object.put("thumbnail", thumbnail);
            object.put("duration_ms", durationMs);
            object.put("view_count", viewCount);
            object.put("published", publishedRank);
            object.put("source_index", sourceIndex);
            object.put("popular_rank", popularRank);
            object.put("last_seen_at", lastSeenAt);
            object.put("last_displayed_at", lastDisplayedAt);
            return object;
        }

        private String key() {
            return clean(url, id);
        }
    }
}
