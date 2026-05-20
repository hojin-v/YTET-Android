package com.ytet.android.extract;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public final class StorageWriterTest {
    @Test
    public void guessesAndroidFriendlyMimeTypesForKnownOutputs() throws Exception {
        assertEquals("audio/mp4", mimeType("song.m4a"));
        assertEquals("video/x-matroska", mimeType("movie.mkv"));
        assertEquals("application/x-subrip", mimeType("caption.ko.srt"));
        assertEquals("application/octet-stream", mimeType("download"));
    }

    private String mimeType(String fileName) throws Exception {
        Method method = StorageWriter.class.getDeclaredMethod("guessMimeType", File.class);
        method.setAccessible(true);
        return (String) method.invoke(new StorageWriter(), new File(fileName));
    }
}
