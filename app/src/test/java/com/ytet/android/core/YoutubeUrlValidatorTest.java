package com.ytet.android.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class YoutubeUrlValidatorTest {
    @Test
    public void acceptsKnownYoutubeHosts() {
        assertEquals(
                "https://www.youtube.com/watch?v=abc123",
                YoutubeUrlValidator.validate(" https://www.youtube.com/watch?v=abc123 ")
        );
        assertEquals(
                "https://youtu.be/abc123",
                YoutubeUrlValidator.validate("https://youtu.be/abc123")
        );
        assertEquals(
                "https://music.youtube.com/watch?v=abc123",
                YoutubeUrlValidator.validate("https://music.youtube.com/watch?v=abc123")
        );
        assertEquals(
                "https://studio.youtube.com/video/abc123",
                YoutubeUrlValidator.validate("https://studio.youtube.com/video/abc123")
        );
    }

    @Test
    public void rejectsMissingOrUnsupportedUrls() {
        assertThrows(IllegalArgumentException.class, () -> YoutubeUrlValidator.validate(null));
        assertThrows(IllegalArgumentException.class, () -> YoutubeUrlValidator.validate("   "));
        assertThrows(IllegalArgumentException.class, () -> YoutubeUrlValidator.validate("ftp://youtube.com/watch?v=abc123"));
        assertThrows(IllegalArgumentException.class, () -> YoutubeUrlValidator.validate("https://example.com/watch?v=abc123"));
        assertThrows(IllegalArgumentException.class, () -> YoutubeUrlValidator.validate("https://youtube.com.evil.test/watch?v=abc123"));
        assertThrows(IllegalArgumentException.class, () -> YoutubeUrlValidator.validate("not a url"));
    }
}
