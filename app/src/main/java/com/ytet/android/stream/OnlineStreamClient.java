package com.ytet.android.stream;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.ytet.android.extract.YtDlpUpdater;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class OnlineStreamClient {
    private OnlineStreamClient() {
    }

    public static List<OnlineStreamSection> loadSections(
            Context context,
            List<OnlineStreamChannel> channels,
            int videosPerChannel
    ) throws Exception {
        ensurePython(context);
        JSONArray request = new JSONArray();
        for (OnlineStreamChannel channel : channels == null ? new ArrayList<OnlineStreamChannel>() : channels) {
            JSONObject item = new JSONObject();
            item.put("id", channel.id());
            item.put("title", channel.title());
            item.put("url", channel.url());
            request.put(item);
        }

        PyObject module = Python.getInstance().getModule("ytet_ydl");
        String json = module.callAttr("stream_channels", request.toString(), Math.max(1, videosPerChannel)).toString();
        return parseSections(json);
    }

    public static List<OnlineStreamSection> loadCandidateSections(
            Context context,
            List<OnlineStreamChannel> channels,
            int candidateLimit
    ) throws Exception {
        ensurePython(context);
        JSONArray request = new JSONArray();
        for (OnlineStreamChannel channel : channels == null ? new ArrayList<OnlineStreamChannel>() : channels) {
            JSONObject item = new JSONObject();
            item.put("id", channel.id());
            item.put("title", channel.title());
            item.put("url", channel.url());
            request.put(item);
        }

        PyObject module = Python.getInstance().getModule("ytet_ydl");
        String json = module.callAttr("stream_channel_candidates", request.toString(), Math.max(1, candidateLimit)).toString();
        return parseSections(json);
    }

    private static List<OnlineStreamSection> parseSections(String json) throws Exception {
        JSONArray sections = new JSONArray(json);
        List<OnlineStreamSection> result = new ArrayList<>();
        for (int sectionIndex = 0; sectionIndex < sections.length(); sectionIndex++) {
            JSONObject section = sections.optJSONObject(sectionIndex);
            if (section == null) {
                continue;
            }
            JSONArray videosJson = section.optJSONArray("videos");
            List<OnlineStreamVideo> videos = new ArrayList<>();
            if (videosJson != null) {
                for (int videoIndex = 0; videoIndex < videosJson.length(); videoIndex++) {
                    OnlineStreamVideo video = parseVideo(
                            videosJson.optJSONObject(videoIndex),
                            section.optString("title", ""),
                            videoIndex
                    );
                    if (video != null) {
                        videos.add(video);
                    }
                }
            }
            if (videos.isEmpty()) {
                continue;
            }
            result.add(new OnlineStreamSection(
                    section.optString("id", "channel-" + sectionIndex),
                    section.optString("title", "YouTube 채널"),
                    section.optString("url", ""),
                    section.optString("avatar", ""),
                    videos
            ));
        }
        return result;
    }

    public static List<OnlineStreamVideo> enrichVideos(Context context, List<OnlineStreamVideo> videos) throws Exception {
        ensurePython(context);
        JSONArray request = new JSONArray();
        for (OnlineStreamVideo video : videos == null ? new ArrayList<OnlineStreamVideo>() : videos) {
            JSONObject item = new JSONObject();
            item.put("id", video.id());
            item.put("title", video.title());
            item.put("channel_title", video.channelTitle());
            item.put("url", video.watchUrl());
            item.put("thumbnail", video.thumbnailUrl());
            item.put("duration", Math.max(0L, video.durationMs() / 1000L));
            item.put("view_count", video.viewCount());
            item.put("published", video.publishedRank());
            item.put("source_index", video.sourceIndex());
            item.put("popular_rank", video.popularRank());
            request.put(item);
        }

        PyObject module = Python.getInstance().getModule("ytet_ydl");
        String json = module.callAttr("enrich_stream_videos", request.toString()).toString();
        JSONArray videosJson = new JSONArray(json);
        List<OnlineStreamVideo> result = new ArrayList<>();
        for (int videoIndex = 0; videoIndex < videosJson.length(); videoIndex++) {
            OnlineStreamVideo video = parseVideo(videosJson.optJSONObject(videoIndex), "", videoIndex);
            if (video != null) {
                result.add(video);
            }
        }
        return result;
    }

    private static OnlineStreamVideo parseVideo(JSONObject video, String fallbackChannelTitle, int fallbackIndex) {
        if (video == null) {
            return null;
        }
        String url = video.optString("url", "");
        if (url.trim().isEmpty()) {
            return null;
        }
        return new OnlineStreamVideo(
                video.optString("id", ""),
                video.optString("title", ""),
                video.optString("channel_title", fallbackChannelTitle),
                url,
                video.optString("thumbnail", ""),
                Math.max(0L, video.optLong("duration", 0L)) * 1000L,
                Math.max(0L, video.optLong("view_count", 0L)),
                Math.max(0L, video.optLong("published", 0L)),
                Math.max(0, video.optInt("source_index", fallbackIndex)),
                Math.max(0, video.optInt("popular_rank", 0))
        );
    }

    private static void ensurePython(Context context) {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
        }
        YtDlpUpdater.applyRuntimeOverride(context);
        YtDlpUpdater.scheduleBackgroundUpdate(context);
    }
}
