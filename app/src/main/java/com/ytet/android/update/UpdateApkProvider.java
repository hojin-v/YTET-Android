package com.ytet.android.update;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class UpdateApkProvider extends ContentProvider {
    public static final String AUTHORITY_SUFFIX = ".updateapk";
    private static final String MIME_APK = "application/vnd.android.package-archive";

    public static Uri uriFor(Context context, File file) {
        File safeFile = resolveFile(context, file.getName());
        try {
            if (!safeFile.getCanonicalFile().equals(file.getCanonicalFile())) {
                throw new IllegalArgumentException("Unsupported update file path");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unsupported update file path", exception);
        }
        return new Uri.Builder()
                .scheme("content")
                .authority(authority(context))
                .appendPath(file.getName())
                .build();
    }

    public static String authority(Context context) {
        return context.getPackageName() + AUTHORITY_SUFFIX;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return MIME_APK;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Update APK is read-only");
        }
        File file = fileForUri(uri);
        if (!file.isFile()) {
            throw new FileNotFoundException("Update APK not found");
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        File file = fileForUri(uri);
        String[] columns = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        MatrixCursor cursor = new MatrixCursor(columns, 1);
        Object[] row = new Object[columns.length];
        for (int index = 0; index < columns.length; index++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[index])) {
                row[index] = file.getName();
            } else if (OpenableColumns.SIZE.equals(columns[index])) {
                row[index] = file.length();
            }
        }
        cursor.addRow(row);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Update APK provider is read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update APK provider is read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update APK provider is read-only");
    }

    private File fileForUri(Uri uri) {
        String name = uri == null ? "" : uri.getLastPathSegment();
        if (name == null || name.isEmpty() || name.contains("/") || name.contains("\\")) {
            throw new IllegalArgumentException("Invalid update file");
        }
        return resolveFile(getContext(), name);
    }

    private static File resolveFile(Context context, String name) {
        File root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (root == null) {
            root = context.getFilesDir();
        }
        return new File(new File(root, "updates"), name);
    }
}
