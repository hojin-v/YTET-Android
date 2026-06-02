package com.ytet.android.library;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public final class DeviceMusicLibrary {
    public List<DeviceAudioTrack> loadTracks(Context context) {
        ContentResolver resolver = context.getContentResolver();
        List<String> projection = new ArrayList<>();
        projection.add(MediaStore.Audio.Media._ID);
        projection.add(MediaStore.Audio.Media.TITLE);
        projection.add(MediaStore.Audio.Media.ARTIST);
        projection.add(MediaStore.Audio.Media.ALBUM);
        projection.add(MediaStore.Audio.Media.DISPLAY_NAME);
        projection.add(MediaStore.Audio.Media.DURATION);
        projection.add(MediaStore.Audio.Media.SIZE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Audio.Media.RELATIVE_PATH);
        } else {
            projection.add(MediaStore.Audio.Media.DATA);
        }

        List<DeviceAudioTrack> tracks = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection.toArray(new String[0]),
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) {
                return tracks;
            }
            while (cursor.moveToNext()) {
                long id = getLong(cursor, MediaStore.Audio.Media._ID);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                String folder = folderFromCursor(cursor);
                tracks.add(new DeviceAudioTrack(
                        id,
                        getString(cursor, MediaStore.Audio.Media.TITLE),
                        getString(cursor, MediaStore.Audio.Media.ARTIST),
                        getString(cursor, MediaStore.Audio.Media.ALBUM),
                        getString(cursor, MediaStore.Audio.Media.DISPLAY_NAME),
                        folder,
                        contentUri.toString(),
                        getLong(cursor, MediaStore.Audio.Media.DURATION),
                        getLong(cursor, MediaStore.Audio.Media.SIZE)
                ));
            }
        }
        return tracks;
    }

    private String folderFromCursor(Cursor cursor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MusicLibrary.folderLabelFromPath(
                    getString(cursor, MediaStore.Audio.Media.RELATIVE_PATH),
                    "Music"
            );
        }
        String data = getString(cursor, MediaStore.Audio.Media.DATA);
        if (data == null || data.trim().isEmpty()) {
            return "Music";
        }
        int lastSlash = data.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "Music";
        }
        String parent = data.substring(0, lastSlash);
        int parentSlash = parent.lastIndexOf('/');
        return parentSlash > -1 && parentSlash < parent.length() - 1
                ? parent.substring(parentSlash + 1)
                : parent;
    }

    private static String getString(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return null;
        }
        return cursor.getString(index);
    }

    private static long getLong(Cursor cursor, String column) {
        int index = cursor.getColumnIndex(column);
        if (index < 0 || cursor.isNull(index)) {
            return 0;
        }
        return cursor.getLong(index);
    }
}
