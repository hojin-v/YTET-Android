package com.ytet.android.core;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class FormatMappingTest {
    @Test
    public void audioFormatValuesAndLabelsRoundTrip() {
        assertEquals(AudioFormat.M4A, AudioFormat.fromValue("m4a"));
        assertEquals(AudioFormat.MP3, AudioFormat.fromValue("mp3"));
        assertEquals(AudioFormat.ORIGINAL, AudioFormat.fromValue("original"));
        assertEquals(AudioFormat.ORIGINAL, AudioFormat.fromValue("best"));
        assertEquals(AudioFormat.M4A, AudioFormat.fromValue("unknown"));

        assertEquals(AudioFormat.M4A, AudioFormat.fromLabel("M4A (AAC)"));
        assertEquals(AudioFormat.MP3, AudioFormat.fromLabel("MP3"));
        assertEquals(AudioFormat.ORIGINAL, AudioFormat.fromLabel("Original Opus"));
        assertEquals(AudioFormat.M4A, AudioFormat.fromLabel("missing"));
    }

    @Test
    public void videoQualityValuesAndLabelsRoundTrip() {
        assertEquals(VideoQuality.BEST, VideoQuality.fromValue("best"));
        assertEquals(VideoQuality.P1080, VideoQuality.fromValue("1080"));
        assertEquals(VideoQuality.P1080, VideoQuality.fromValue("1080p"));
        assertEquals(VideoQuality.P720, VideoQuality.fromValue("720"));
        assertEquals(VideoQuality.P480, VideoQuality.fromValue("480p"));
        assertEquals(VideoQuality.BEST, VideoQuality.fromValue("unknown"));

        assertArrayEquals(
                new String[]{"원본 최고품질", "1080p MP4", "720p MP4", "480p MP4"},
                VideoQuality.labels()
        );
        assertEquals(VideoQuality.P720, VideoQuality.fromLabel("720p MP4"));
        assertEquals(VideoQuality.BEST, VideoQuality.fromLabel("missing"));
    }

    @Test
    public void mediaTypeDefaultsToAudioUnlessVideoIsExplicit() {
        assertEquals(MediaType.VIDEO, MediaType.fromValue("video"));
        assertEquals(MediaType.VIDEO, MediaType.fromValue("VIDEO"));
        assertEquals(MediaType.AUDIO, MediaType.fromValue("audio"));
        assertEquals(MediaType.AUDIO, MediaType.fromValue(null));
        assertEquals(MediaType.AUDIO, MediaType.fromValue("anything"));
    }
}
