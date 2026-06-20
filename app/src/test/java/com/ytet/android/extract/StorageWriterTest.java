package com.ytet.android.extract;

import com.ytet.android.core.MediaType;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public final class StorageWriterTest {
    @Test
    public void guessesAndroidFriendlyMimeTypesForKnownOutputs() throws Exception {
        assertEquals("audio/mp4", mimeType("song.m4a"));
        assertEquals("video/x-matroska", mimeType("movie.mkv"));
        assertEquals("video/webm", mimeType("movie.webm"));
        assertEquals("application/x-subrip", mimeType("caption.ko.srt"));
        assertEquals("application/octet-stream", mimeType("download"));
    }

    @Test
    public void relativeDisplayNamePreservesPlaylistFolder() throws Exception {
        File workspace = new File("/tmp/workspace");
        File output = new File(workspace, "Album/001 - Song.m4a");

        assertEquals("Album/001 - Song.m4a", relativeDisplayName(workspace, output));
    }

    @Test
    public void relativeDisplayNameSanitizesPlaylistFolderAndFileName() throws Exception {
        File workspace = new File("/tmp/workspace");
        File output = new File(workspace, "Album: Best?/001 - A*B?C.m4a");

        assertEquals("Album Best/001 - A B C.m4a", relativeDisplayName(workspace, output));
    }

    @Test
    public void defaultAudioPathPreservesPlaylistFolderUnderRabbytMusic() throws Exception {
        File workspace = new File("/tmp/workspace");
        File output = new File(workspace, "Album/001 - Song.m4a");

        assertEquals("Download/RabbYT/Music/Album/", targetRelativePath(MediaType.AUDIO, workspace, output));
    }

    @Test
    public void defaultAudioPathSanitizesPlaylistFolderUnderRabbytMusic() throws Exception {
        File workspace = new File("/tmp/workspace");
        File output = new File(workspace, "Album: Best?/001 - Song.m4a");

        assertEquals("Download/RabbYT/Music/Album Best/", targetRelativePath(MediaType.AUDIO, workspace, output));
    }

    @Test
    public void defaultVideoPathUsesRabbytVideo() throws Exception {
        File workspace = new File("/tmp/workspace");
        File output = new File(workspace, "Clip.mp4");

        assertEquals("Download/RabbYT/Video/", targetRelativePath(MediaType.VIDEO, workspace, output));
    }

    private String mimeType(String fileName) throws Exception {
        Method method = StorageWriter.class.getDeclaredMethod("guessMimeType", File.class);
        method.setAccessible(true);
        return (String) method.invoke(new StorageWriter(), new File(fileName));
    }

    private String relativeDisplayName(File baseDir, File file) throws Exception {
        Method method = StorageWriter.class.getDeclaredMethod("relativeDisplayName", File.class, File.class);
        method.setAccessible(true);
        return (String) method.invoke(new StorageWriter(), baseDir, file);
    }

    private String targetRelativePath(MediaType mediaType, File baseDir, File file) throws Exception {
        Method method = StorageWriter.class.getDeclaredMethod("targetRelativePath", MediaType.class, File.class, File.class);
        method.setAccessible(true);
        return (String) method.invoke(new StorageWriter(), mediaType, baseDir, file);
    }
}
