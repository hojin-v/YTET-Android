package com.ytet.android.extract;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MediaTrackMuxerTest {
    @Test
    public void embedsSubtitlesIntoMp4AsMovText() {
        List<String> arguments = Arrays.asList(MediaTrackMuxer.buildArguments(
                new File("/tmp/video-track.mp4"),
                new File("/tmp/audio-track.m4a"),
                new File("/tmp/out.mp4"),
                List.of(new File("/tmp/title.ko.srt"), new File("/tmp/title.en-US.vtt"))
        ));

        assertContainsPair(arguments, "-map", "0:v:0");
        assertContainsPair(arguments, "-map", "1:a:0");
        assertContainsPair(arguments, "-map", "2:0");
        assertContainsPair(arguments, "-map", "3:0");
        assertContainsPair(arguments, "-c:s", "mov_text");
        assertContainsPair(arguments, "-metadata:s:s:0", "language=kor");
        assertContainsPair(arguments, "-metadata:s:s:0", "title=Korean");
        assertContainsPair(arguments, "-metadata:s:s:1", "language=eng");
        assertContainsPair(arguments, "-metadata:s:s:1", "title=English");
        assertContainsPair(arguments, "-disposition:s:0", "default");
        assertEquals("/tmp/out.mp4", arguments.get(arguments.size() - 1));
    }

    @Test
    public void remuxesSingleVideoAndKeepsExistingAudioWhenNoSeparateTrackExists() {
        List<String> arguments = Arrays.asList(MediaTrackMuxer.buildArguments(
                new File("/tmp/video-track.mp4"),
                null,
                new File("/tmp/out.mkv"),
                List.of(new File("/tmp/title.ko.srt"))
        ));

        assertContainsPair(arguments, "-map", "0:v:0");
        assertContainsPair(arguments, "-map", "0:a?");
        assertContainsPair(arguments, "-map", "1:0");
        assertContainsPair(arguments, "-c:s", "srt");
    }

    private void assertContainsPair(List<String> values, String left, String right) {
        for (int index = 0; index < values.size() - 1; index++) {
            if (left.equals(values.get(index)) && right.equals(values.get(index + 1))) {
                return;
            }
        }
        assertTrue("missing argument pair: " + left + " " + right + " in " + values, false);
    }
}
