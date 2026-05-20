package com.ytet.android.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ExtractionRequestTest {
    @Test
    public void defaultsNullModeToAudioM4a() {
        ExtractionRequest request = new ExtractionRequest(
                "https://youtu.be/abc123",
                "content://tree/output",
                null,
                "",
                true,
                true
        );

        assertEquals(MediaType.AUDIO, request.mediaType());
        assertEquals(AudioFormat.M4A.value(), request.option());
        assertTrue(request.includeSubtitles());
        assertTrue(request.includeMultiAudio());
    }

    @Test
    public void defaultsVideoOptionToBest() {
        ExtractionRequest request = new ExtractionRequest(
                "https://youtube.com/watch?v=abc123",
                "content://tree/output",
                MediaType.VIDEO,
                null,
                false,
                false
        );

        assertEquals(MediaType.VIDEO, request.mediaType());
        assertEquals(VideoQuality.BEST.value(), request.option());
        assertFalse(request.includeSubtitles());
        assertFalse(request.includeMultiAudio());
    }

    @Test
    public void preservesExplicitOption() {
        ExtractionRequest request = new ExtractionRequest(
                "https://youtube.com/watch?v=abc123",
                "content://tree/output",
                MediaType.VIDEO,
                VideoQuality.P720.value(),
                true,
                false
        );

        assertEquals(VideoQuality.P720.value(), request.option());
        assertTrue(request.includeSubtitles());
    }

    @Test
    public void rejectsInvalidUrlBeforeWorkStarts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExtractionRequest("https://example.com/a", "content://tree/output", MediaType.AUDIO, "m4a", false, false)
        );
    }
}
