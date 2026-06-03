package com.ytet.android.extract;

import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.ytet.android.core.DefaultMediaPaths;
import com.ytet.android.core.MediaType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StorageWriter {
    public void ensureDefaultFolders() {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        new File(downloads, DefaultMediaPaths.APP_FOLDER + "/" + DefaultMediaPaths.MUSIC_FOLDER).mkdirs();
        new File(downloads, DefaultMediaPaths.APP_FOLDER + "/" + DefaultMediaPaths.VIDEO_FOLDER).mkdirs();
    }

    public List<CopiedFile> copyToDefaultPublicFolder(
            Context context,
            MediaType mediaType,
            File baseDir,
            List<File> files
    ) throws ExtractionException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return copyToDefaultMediaStore(context, mediaType, baseDir, files);
        }
        return copyToDefaultPublicDirectory(context, mediaType, baseDir, files);
    }

    public List<CopiedFile> copyToTree(Context context, Uri treeUri, List<File> files) throws ExtractionException {
        return copyToTree(context, treeUri, null, files);
    }

    public List<CopiedFile> copyToTree(Context context, Uri treeUri, File baseDir, List<File> files) throws ExtractionException {
        ContentResolver resolver = context.getContentResolver();
        String treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId);
        List<CopiedFile> copiedFiles = new ArrayList<>();
        List<String> scanPaths = new ArrayList<>();
        List<String> scanMimeTypes = new ArrayList<>();
        Map<String, Uri> folderCache = new HashMap<>();

        for (File file : files) {
            String mimeType = guessMimeType(file);
            String displayName = relativeDisplayName(baseDir, file);
            Uri targetParentUri = parentUriForFile(resolver, parentUri, baseDir, file, folderCache);
            Uri targetUri;
            try {
                targetUri = DocumentsContract.createDocument(resolver, targetParentUri, mimeType, file.getName());
            } catch (Exception exception) {
                throw new ExtractionException("저장 파일을 만들 수 없습니다: " + displayName, exception);
            }
            if (targetUri == null) {
                throw new ExtractionException("저장 파일 URI를 만들 수 없습니다: " + displayName);
            }

            try (InputStream input = Files.newInputStream(file.toPath());
                 OutputStream output = resolver.openOutputStream(targetUri, "w")) {
                if (output == null) {
                    throw new IOException("OutputStream is null.");
                }
                copy(input, output);
            } catch (IOException exception) {
                throw new ExtractionException("파일 저장 중 오류가 발생했습니다: " + displayName, exception);
            }

            String scanPath = scanPathForDocumentUri(targetUri);
            if (!scanPath.isEmpty()) {
                scanPaths.add(scanPath);
                scanMimeTypes.add(mimeType);
            }
            copiedFiles.add(new CopiedFile(displayName, mimeType, file.length(), targetUri.toString()));
        }

        scanCopiedFiles(context, scanPaths, scanMimeTypes);
        return copiedFiles;
    }

    private List<CopiedFile> copyToDefaultMediaStore(
            Context context,
            MediaType mediaType,
            File baseDir,
            List<File> files
    ) throws ExtractionException {
        ContentResolver resolver = context.getContentResolver();
        List<CopiedFile> copiedFiles = new ArrayList<>();
        for (File file : files) {
            String displayName = relativeDisplayName(baseDir, file);
            String mimeType = guessMimeType(file);
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath(mediaType, baseDir, file));
            values.put(MediaStore.MediaColumns.SIZE, file.length());
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri targetUri;
            try {
                targetUri = resolver.insert(collectionUriFor(mediaType, mimeType), values);
            } catch (Exception exception) {
                throw new ExtractionException("기본 저장소에 파일을 만들 수 없습니다: " + displayName, exception);
            }
            if (targetUri == null) {
                throw new ExtractionException("기본 저장소 파일 URI를 만들 수 없습니다: " + displayName);
            }

            try (InputStream input = Files.newInputStream(file.toPath());
                 OutputStream output = resolver.openOutputStream(targetUri, "w")) {
                if (output == null) {
                    throw new IOException("OutputStream is null.");
                }
                copy(input, output);
            } catch (IOException exception) {
                safeDelete(resolver, targetUri);
                throw new ExtractionException("기본 저장소에 파일을 복사할 수 없습니다: " + displayName, exception);
            }

            ContentValues complete = new ContentValues();
            complete.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(targetUri, complete, null, null);
            copiedFiles.add(new CopiedFile(displayName, mimeType, file.length(), targetUri.toString()));
        }
        return copiedFiles;
    }

    private List<CopiedFile> copyToDefaultPublicDirectory(
            Context context,
            MediaType mediaType,
            File baseDir,
            List<File> files
    ) throws ExtractionException {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File root = new File(downloads, mediaType == MediaType.VIDEO
                ? DefaultMediaPaths.APP_FOLDER + "/" + DefaultMediaPaths.VIDEO_FOLDER
                : DefaultMediaPaths.APP_FOLDER + "/" + DefaultMediaPaths.MUSIC_FOLDER);
        List<CopiedFile> copiedFiles = new ArrayList<>();
        List<String> scanPaths = new ArrayList<>();
        List<String> scanMimeTypes = new ArrayList<>();
        for (File file : files) {
            String displayName = relativeDisplayName(baseDir, file);
            String mimeType = guessMimeType(file);
            File target = new File(root, displayName);
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new ExtractionException("기본 저장 폴더를 만들 수 없습니다: " + parent.getAbsolutePath());
            }
            try {
                Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                throw new ExtractionException("기본 저장소에 파일을 복사할 수 없습니다: " + displayName, exception);
            }
            scanPaths.add(target.getAbsolutePath());
            scanMimeTypes.add(mimeType);
            copiedFiles.add(new CopiedFile(displayName, mimeType, target.length(), Uri.fromFile(target).toString()));
        }
        MediaScannerConnection.scanFile(
                context,
                scanPaths.toArray(new String[0]),
                scanMimeTypes.toArray(new String[0]),
                null
        );
        return copiedFiles;
    }

    private Uri parentUriForFile(
            ContentResolver resolver,
            Uri rootUri,
            File baseDir,
            File file,
            Map<String, Uri> folderCache
    ) throws ExtractionException {
        String relativeParent = relativeParentPath(baseDir, file);
        if (relativeParent.isEmpty()) {
            return rootUri;
        }

        Uri currentUri = rootUri;
        StringBuilder cacheKey = new StringBuilder();
        for (String segment : relativeParent.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            if (cacheKey.length() > 0) {
                cacheKey.append('/');
            }
            cacheKey.append(segment);
            String key = cacheKey.toString();
            Uri cached = folderCache.get(key);
            if (cached != null) {
                currentUri = cached;
                continue;
            }
            try {
                currentUri = DocumentsContract.createDocument(
                        resolver,
                        currentUri,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        segment
                );
            } catch (Exception exception) {
                throw new ExtractionException("저장 폴더를 만들 수 없습니다: " + key, exception);
            }
            if (currentUri == null) {
                throw new ExtractionException("저장 폴더 URI를 만들 수 없습니다: " + key);
            }
            folderCache.put(key, currentUri);
        }
        return currentUri;
    }

    private void scanCopiedFiles(Context context, List<String> scanPaths, List<String> scanMimeTypes) {
        if (scanPaths.isEmpty()) {
            return;
        }
        MediaScannerConnection.scanFile(
                context,
                scanPaths.toArray(new String[0]),
                scanMimeTypes.toArray(new String[0]),
                null
        );
    }

    private String scanPathForDocumentUri(Uri documentUri) {
        String relativePath = DefaultMediaPaths.primaryExternalStorageRelativePathFromDocumentUri(documentUri.toString());
        if (relativePath.isEmpty()) {
            return "";
        }
        return new File(Environment.getExternalStorageDirectory(), relativePath).getAbsolutePath();
    }

    private Uri collectionUriFor(MediaType mediaType, String mimeType) {
        if (mediaType == MediaType.VIDEO && mimeType != null && mimeType.startsWith("video/")) {
            return MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }
        if (mediaType == MediaType.AUDIO && mimeType != null && mimeType.startsWith("audio/")) {
            return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }
        return MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
    }

    private String targetRelativePath(MediaType mediaType, File baseDir, File file) {
        String parent = DefaultMediaPaths.normalizeRelativePath(relativeParentPath(baseDir, file));
        return DefaultMediaPaths.extractionRelativePath(mediaType) + parent;
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    private void safeDelete(ContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
        } catch (Exception ignored) {
            // Best-effort cleanup after a failed copy.
        }
    }

    private String relativeDisplayName(File baseDir, File file) {
        String relativePath = relativePath(baseDir, file);
        return relativePath.isEmpty() ? file.getName() : relativePath;
    }

    private String relativeParentPath(File baseDir, File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            return "";
        }
        return relativePath(baseDir, parent);
    }

    private String relativePath(File baseDir, File file) {
        if (baseDir == null || file == null) {
            return "";
        }
        try {
            Path basePath = baseDir.toPath().toAbsolutePath().normalize();
            Path filePath = file.toPath().toAbsolutePath().normalize();
            if (!filePath.startsWith(basePath)) {
                return "";
            }
            String relative = basePath.relativize(filePath).toString().replace(File.separatorChar, '/');
            return relative.equals(".") ? "" : relative;
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String guessMimeType(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > -1 && dot < name.length() - 1) {
            String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            if ("m4a".equals(extension)) {
                return "audio/mp4";
            }
            if ("mkv".equals(extension)) {
                return "video/x-matroska";
            }
            if ("webm".equals(extension)) {
                return "video/webm";
            }
            if ("srt".equals(extension)) {
                return "application/x-subrip";
            }
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    public static final class CopiedFile {
        private final String name;
        private final String mimeType;
        private final long bytes;
        private final String uri;

        CopiedFile(String name, String mimeType, long bytes, String uri) {
            this.name = name;
            this.mimeType = mimeType;
            this.bytes = bytes;
            this.uri = uri;
        }

        public String name() {
            return name;
        }

        public String mimeType() {
            return mimeType;
        }

        public long bytes() {
            return bytes;
        }

        public String uri() {
            return uri;
        }
    }
}
