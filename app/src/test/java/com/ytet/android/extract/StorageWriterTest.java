package com.ytet.android.extract;

import com.ytet.android.core.MediaType;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void legacyMigrationDoesNotCreateEmptyTargetFolders() throws Exception {
        Path root = Files.createTempDirectory("ytet-storage");
        try {
            File sourceRoot = root.resolve("YTET/Music").toFile();
            File emptyArtistFolder = root.resolve("YTET/Music/Lauv").toFile();
            assertTrue(emptyArtistFolder.mkdirs());

            File targetRoot = root.resolve("RabbYT/Music").toFile();
            moveDirectoryContents(sourceRoot, targetRoot, new ArrayList<>());

            assertFalse(new File(targetRoot, "Lauv").exists());
        } finally {
            deleteRecursively(root.toFile());
        }
    }

    @Test
    public void legacyMigrationCreatesTargetFolderOnlyForMovedFiles() throws Exception {
        Path root = Files.createTempDirectory("ytet-storage");
        try {
            File sourceFile = root.resolve("YTET/Music/Lauv/001 - Song.m4a").toFile();
            assertTrue(sourceFile.getParentFile().mkdirs());
            Files.write(sourceFile.toPath(), "audio".getBytes(StandardCharsets.UTF_8));

            File targetRoot = root.resolve("RabbYT/Music").toFile();
            List<String> scanPaths = new ArrayList<>();
            moveDirectoryContents(root.resolve("YTET/Music").toFile(), targetRoot, scanPaths);

            File targetFile = new File(targetRoot, "Lauv/001 - Song.m4a");
            assertTrue(targetFile.isFile());
            assertEquals(1, scanPaths.size());
            assertEquals(targetFile.getAbsolutePath(), scanPaths.get(0));
        } finally {
            deleteRecursively(root.toFile());
        }
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

    private void moveDirectoryContents(File sourceDirectory, File targetDirectory, List<String> scanPaths) throws Exception {
        Method method = StorageWriter.class.getDeclaredMethod("moveDirectoryContents", File.class, File.class, List.class);
        method.setAccessible(true);
        method.invoke(new StorageWriter(), sourceDirectory, targetDirectory, scanPaths);
    }

    private void deleteRecursively(File file) throws Exception {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(file.toPath());
    }
}
