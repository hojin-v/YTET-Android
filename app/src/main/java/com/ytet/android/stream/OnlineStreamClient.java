package com.ytet.android.stream;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

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
                    JSONObject video = videosJson.optJSONObject(videoIndex);
                    if (video == null) {
                        continue;
                    }
                    String url = video.optString("url", "");
                    if (url.trim().isEmpty()) {
                        continue;
                    }
                    videos.add(new OnlineStreamVideo(
                            video.optString("id", ""),
                            video.optString("title", ""),
                            video.optString("channel_title", section.optString("title", "")),
                            url,
                            video.optString("thumbnail", ""),
                            Math.max(0L, video.optLong("duration", 0L)) * 1000L
                    ));
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

    private static void ensurePython(Context context) {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
        }
    }
}
