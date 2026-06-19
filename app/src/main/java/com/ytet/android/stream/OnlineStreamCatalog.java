package com.ytet.android.stream;

import java.util.ArrayList;
import java.util.List;

public final class OnlineStreamCatalog {
    private OnlineStreamCatalog() {
    }

    public static List<OnlineStreamChannel> defaultChannels() {
        List<OnlineStreamChannel> channels = new ArrayList<>();
        channels.add(new OnlineStreamChannel(
                "essentialme",
                "essential;",
                "https://www.youtube.com/@essentialme"
        ));
        channels.add(new OnlineStreamChannel(
                "layback",
                "레이백",
                "https://www.youtube.com/@%EB%A0%88%EC%9D%B4%EB%B0%B1"
        ));
        channels.add(new OnlineStreamChannel(
                "minplay",
                "민플리",
                "https://www.youtube.com/@%EB%AF%BC%ED%94%8C%EB%A6%AC"
        ));
        return channels;
    }
}
