package com.ytet.android.extract;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StorageWriter {
    public List<CopiedFile> copyToTree(Context context, Uri treeUri, List<File> files) throws ExtractionException {
        ContentResolver resolver = context.getContentResolver();
        String treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId);
        List<CopiedFile> copiedFiles = new ArrayList<>();

        for (File file : files) {
            String mimeType = guessMimeType(file);
            Uri targetUri;
            try {
                targetUri = DocumentsContract.createDocument(resolver, parentUri, mimeType, file.getName());
            } catch (Exception exception) {
                throw new ExtractionException("저장 파일을 만들 수 없습니다: " + file.getName(), exception);
            }
            if (targetUri == null) {
                throw new ExtractionException("저장 파일 URI를 만들 수 없습니다: " + file.getName());
            }

            try (InputStream input = Files.newInputStream(file.toPath());
                 OutputStream output = resolver.openOutputStream(targetUri, "w")) {
                if (output == null) {
                    throw new IOException("OutputStream is null.");
                }
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            } catch (IOException exception) {
                throw new ExtractionException("파일 저장 중 오류가 발생했습니다: " + file.getName(), exception);
            }

            copiedFiles.add(new CopiedFile(file.getName(), mimeType, file.length(), targetUri.toString()));
        }

        return copiedFiles;
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

