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
                true
        );

        assertEquals(MediaType.AUDIO, request.mediaType());
        assertEquals(AudioFormat.M4A.value(), request.option());
        assertTrue(request.includeSubtitles());
    }

    @Test
    public void defaultsVideoOptionToBest() {
        ExtractionRequest request = new ExtractionRequest(
                "https://youtube.com/watch?v=abc123",
                "content://tree/output",
                MediaType.VIDEO,
                null,
                false
        );

        assertEquals(MediaType.VIDEO, request.mediaType());
        assertEquals(VideoQuality.BEST.value(), request.option());
        assertFalse(request.includeSubtitles());
    }

    @Test
    public void preservesExplicitOption() {
        ExtractionRequest request = new ExtractionRequest(
                "https://youtube.com/watch?v=abc123",
                "content://tree/output",
                MediaType.VIDEO,
                VideoQuality.P720.value(),
                true
        );

        assertEquals(VideoQuality.P720.value(), request.option());
        assertTrue(request.includeSubtitles());
    }

    @Test
    public void preservesPlaylistMode() {
        ExtractionRequest request = new ExtractionRequest(
                "https://youtube.com/watch?v=abc123&list=PL123",
                "content://tree/output",
                MediaType.AUDIO,
                AudioFormat.M4A.value(),
                false,
                true,
                true
        );

        assertTrue(request.includePlaylist());
        assertTrue(request.enhanceMetadata());
        assertFalse(request.includeSubtitles());
    }

    @Test
    public void defaultsBlankOutputToYtetPublicFolder() {
        ExtractionRequest request = new ExtractionRequest(
                "https://youtube.com/watch?v=abc123",
                "",
                MediaType.AUDIO,
                AudioFormat.M4A.value(),
                false
        );

        assertEquals(DefaultMediaPaths.DEFAULT_OUTPUT_URI, request.outputTreeUri());
        assertTrue(request.usesDefaultOutput());
    }

    @Test
    public void rejectsInvalidUrlBeforeWorkStarts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExtractionRequest("https://example.com/a", "content://tree/output", MediaType.AUDIO, "m4a", false)
        );
    }
}
