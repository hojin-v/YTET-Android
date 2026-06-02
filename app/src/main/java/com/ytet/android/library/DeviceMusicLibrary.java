package com.ytet.android.library;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DeviceMusicLibrary {
    private static final int QUERY_CHUNK_SIZE = 200;

    public List<DeviceAudioTrack> loadTracks(Context context) {
        ContentResolver resolver = context.getContentResolver();
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection().toArray(new String[0]),
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) {
                return tracks;
            }
            while (cursor.moveToNext()) {
                tracks.add(trackFromCursor(cursor));
            }
        }
        return tracks;
    }

    public List<DeviceAudioTrack> loadTracksByIds(Context context, long[] ids) {
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        if (ids == null || ids.length == 0) {
            return tracks;
        }

        ContentResolver resolver = context.getContentResolver();
        Map<Long, DeviceAudioTrack> loaded = new HashMap<>();
        for (int start = 0; start < ids.length; start += QUERY_CHUNK_SIZE) {
            int end = Math.min(ids.length, start + QUERY_CHUNK_SIZE);
            StringBuilder selection = new StringBuilder(MediaStore.Audio.Media.IS_MUSIC + " != 0 AND ");
            selection.append(MediaStore.Audio.Media._ID).append(" IN (");
            String[] args = new String[end - start];
            for (int index = start; index < end; index++) {
                if (index > start) {
                    selection.append(',');
                }
                selection.append('?');
                args[index - start] = Long.toString(ids[index]);
            }
            selection.append(')');

            try (Cursor cursor = resolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection().toArray(new String[0]),
                    selection.toString(),
                    args,
                    null
            )) {
                if (cursor == null) {
                    continue;
                }
                while (cursor.moveToNext()) {
                    DeviceAudioTrack track = trackFromCursor(cursor);
                    loaded.put(track.id(), track);
                }
            }
        }

        for (long id : ids) {
            DeviceAudioTrack track = loaded.get(id);
            if (track != null) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    private List<String> projection() {
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
        return projection;
    }

    private DeviceAudioTrack trackFromCursor(Cursor cursor) {
        long id = getLong(cursor, MediaStore.Audio.Media._ID);
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        return new DeviceAudioTrack(
                id,
                getString(cursor, MediaStore.Audio.Media.TITLE),
                getString(cursor, MediaStore.Audio.Media.ARTIST),
                getString(cursor, MediaStore.Audio.Media.ALBUM),
                getString(cursor, MediaStore.Audio.Media.DISPLAY_NAME),
                folderFromCursor(cursor),
                contentUri.toString(),
                getLong(cursor, MediaStore.Audio.Media.DURATION),
                getLong(cursor, MediaStore.Audio.Media.SIZE)
        );
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
