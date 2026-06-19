package com.ytet.android.update;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import com.ytet.android.R;
import com.ytet.android.library.MusicLibrary;
import com.ytet.android.ui.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateDownloadService extends Service {
    public static final String ACTION_START = "com.ytet.android.action.START_UPDATE_DOWNLOAD";
    public static final String ACTION_PROGRESS = "com.ytet.android.action.UPDATE_DOWNLOAD_PROGRESS";
    public static final String ACTION_CANCEL = "com.ytet.android.action.CANCEL_UPDATE_DOWNLOAD";
    public static final String ACTION_INSTALL_UPDATE = "com.ytet.android.action.INSTALL_UPDATE";
    public static final String EXTRA_TAG = "com.ytet.android.extra.UPDATE_TAG";
    public static final String EXTRA_APK_NAME = "com.ytet.android.extra.UPDATE_APK_NAME";
    public static final String EXTRA_APK_URL = "com.ytet.android.extra.UPDATE_APK_URL";
    public static final String EXTRA_APK_PATH = "com.ytet.android.extra.UPDATE_APK_PATH";
    public static final String EXTRA_PERCENT = "com.ytet.android.extra.UPDATE_PERCENT";
    public static final String EXTRA_MESSAGE = "com.ytet.android.extra.UPDATE_MESSAGE";
    public static final String EXTRA_DONE = "com.ytet.android.extra.UPDATE_DONE";
    public static final String EXTRA_ERROR = "com.ytet.android.extra.UPDATE_ERROR";
    public static final String EXTRA_CANCELED = "com.ytet.android.extra.UPDATE_CANCELED";

    private static final String CHANNEL_ID = "ytet_updates";
    private static final int NOTIFICATION_ID = 4213;
    private static final String PREFS = "ytet_android";
    private static final String PREF_UPDATE_APK_PATH = "update_apk_path";
    private static final String PREF_UPDATE_DOWNLOAD_ID = "update_download_id";
    private static final String PREF_UPDATE_TAG = "update_tag";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private volatile Future<?> currentTask;
    private volatile int latestStartId;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            latestStartId = startId;
            requestCancellation();
            return START_NOT_STICKY;
        }

        if (intent == null || !ACTION_START.equals(intent.getAction())) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        latestStartId = startId;
        if (currentTask != null && !currentTask.isDone()) {
            return START_REDELIVER_INTENT;
        }

        String tag = clean(intent.getStringExtra(EXTRA_TAG));
        String apkName = clean(intent.getStringExtra(EXTRA_APK_NAME));
        String apkUrl = clean(intent.getStringExtra(EXTRA_APK_URL));
        startForegroundNotification(notification("업데이트 다운로드", "다운로드 준비 중입니다.", -1, false, false));
        if (apkUrl.isEmpty()) {
            publishError("다운로드할 업데이트 파일이 없습니다.", startId);
            return START_NOT_STICKY;
        }

        cancelRequested.set(false);
        currentTask = executor.submit(() -> downloadUpdate(tag, apkName, apkUrl, startId));
        return START_REDELIVER_INTENT;
    }

    public static Intent downloadIntent(Context context, UpdateInfo update) {
        Intent intent = new Intent(context, UpdateDownloadService.class);
        intent.setAction(ACTION_START);
        if (update != null) {
            intent.putExtra(EXTRA_TAG, update.tagName());
            intent.putExtra(EXTRA_APK_NAME, update.apkName());
            intent.putExtra(EXTRA_APK_URL, update.apkUrl());
        }
        return intent;
    }

    public static Intent installIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(ACTION_INSTALL_UPDATE);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void downloadUpdate(String tag, String apkName, String apkUrl, int startId) {
        File targetFile = new File(updateDownloadDir(), updateApkFileName(tag, apkName));
        File temporaryFile = new File(targetFile.getParentFile(), targetFile.getName() + ".part");
        try {
            downloadApk(apkUrl, temporaryFile, tag);
            if (cancelRequested.get()) {
                publishCanceled(startId);
                return;
            }
            if (targetFile.exists() && !targetFile.delete()) {
                throw new IOException("이전 업데이트 파일을 교체할 수 없습니다.");
            }
            if (!temporaryFile.renameTo(targetFile)) {
                throw new IOException("업데이트 파일을 완료할 수 없습니다.");
            }
            deleteOtherUpdateApks(targetFile);
            saveDownloadedUpdate(targetFile, tag);
            publishDone(targetFile, tag, startId);
        } catch (Exception exception) {
            if (temporaryFile.exists()) {
                temporaryFile.delete();
            }
            if (cancelRequested.get()) {
                publishCanceled(startId);
            } else {
                if (targetFile.exists()) {
                    targetFile.delete();
                }
                publishError("업데이트 다운로드에 실패했습니다: " + safeMessage(exception), startId);
            }
        } finally {
            currentTask = null;
        }
    }

    private void downloadApk(String apkUrl, File targetFile, String tag) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(apkUrl).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "YTET-Android-Updater");
        try {
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_PARTIAL) {
                throw new IOException("부분 다운로드 응답은 지원하지 않습니다.");
            }
            if (status < 200 || status >= 300) {
                throw new IOException("서버 응답 " + status);
            }
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("업데이트 폴더를 만들 수 없습니다.");
            }
            long totalBytes = Math.max(0L, connection.getContentLengthLong());
            publishProgress(tag, 0, totalBytes, 0L);
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(targetFile, false)) {
                byte[] buffer = new byte[64 * 1024];
                long downloadedBytes = 0L;
                long lastPublishedBytes = 0L;
                int lastPublishedPercent = -1;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (cancelRequested.get() || Thread.currentThread().isInterrupted()) {
                        throw new InterruptedIOException();
                    }
                    output.write(buffer, 0, read);
                    downloadedBytes += read;
                    int percent = downloadPercent(downloadedBytes, totalBytes);
                    if (downloadedBytes - lastPublishedBytes >= 256L * 1024L
                            || (percent >= 0 && percent != lastPublishedPercent)) {
                        lastPublishedBytes = downloadedBytes;
                        lastPublishedPercent = percent;
                        publishProgress(tag, percent, totalBytes, downloadedBytes);
                    }
                }
                publishProgress(tag, downloadPercent(downloadedBytes, totalBytes), totalBytes, downloadedBytes);
                if (totalBytes > 0L && downloadedBytes != totalBytes) {
                    throw new IOException("다운로드 크기가 일치하지 않습니다.");
                }
            }
            if (targetFile.length() <= 0L) {
                throw new IOException("빈 업데이트 파일입니다.");
            }
        } finally {
            connection.disconnect();
        }
    }

    private void publishProgress(String tag, int percent, long totalBytes, long downloadedBytes) {
        String message = progressMessage(percent, totalBytes, downloadedBytes);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification("업데이트 다운로드", message, percent, false, false));

        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_TAG, tag);
        intent.putExtra(EXTRA_PERCENT, percent);
        intent.putExtra(EXTRA_MESSAGE, message);
        sendBroadcast(intent);
    }

    private void publishDone(File apkFile, String tag, int startId) {
        detachForegroundNotification();
        String message = "다운로드 완료. 설치할 수 있습니다.";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification("업데이트 설치 준비 완료", message, 100, true, false));

        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_TAG, tag);
        intent.putExtra(EXTRA_APK_PATH, apkFile.getAbsolutePath());
        intent.putExtra(EXTRA_PERCENT, 100);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_DONE, true);
        sendBroadcast(intent);
        stopSelf(terminalStartId(startId));
    }

    private void publishError(String message, int startId) {
        detachForegroundNotification();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification("업데이트 다운로드 실패", message, 0, true, true));

        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_ERROR, message);
        intent.putExtra(EXTRA_DONE, true);
        sendBroadcast(intent);
        stopSelf(terminalStartId(startId));
    }

    private void publishCanceled(int startId) {
        detachForegroundNotification();
        clearPartialDownloads();
        String message = "업데이트 다운로드를 취소했습니다.";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification("업데이트 다운로드 취소됨", message, 0, true, false));

        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_DONE, true);
        intent.putExtra(EXTRA_CANCELED, true);
        sendBroadcast(intent);
        stopSelf(terminalStartId(startId));
    }

    private Notification notification(String title, String message, int percent, boolean finished, boolean failed) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(R.drawable.ic_stat_extract)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(finished ? contentIntent() : openAppIntent())
                .setAutoCancel(finished)
                .setOngoing(!finished)
                .setOnlyAlertOnce(!finished);

        if (finished && !failed) {
            builder.addAction(R.drawable.ic_play_arrow, "설치", installPendingIntent());
        } else if (!finished) {
            builder.addAction(R.drawable.ic_close, "취소", cancelPendingIntent());
        }

        if (failed) {
            builder.setProgress(0, 0, false);
        } else if (percent >= 0 && !finished) {
            builder.setProgress(100, Math.max(0, Math.min(100, percent)), false);
        } else if (finished) {
            builder.setProgress(0, 0, false);
        } else {
            builder.setProgress(0, 0, percent <= 0 && !finished);
        }

        return builder.build();
    }

    private PendingIntent openAppIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent installPendingIntent() {
        return PendingIntent.getActivity(
                this,
                1,
                installIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent cancelPendingIntent() {
        Intent intent = new Intent(this, UpdateDownloadService.class);
        intent.setAction(ACTION_CANCEL);
        return PendingIntent.getService(
                this,
                2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent contentIntent() {
        return openAppIntent();
    }

    private void requestCancellation() {
        cancelRequested.set(true);
        Future<?> task = currentTask;
        if (task == null || task.isDone()) {
            stopSelf();
            return;
        }
        task.cancel(true);
        publishProgress("", -1, 0L, 0L);
    }

    private void startForegroundNotification(Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void detachForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH);
        } else {
            stopForeground(false);
        }
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "업데이트",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("업데이트 다운로드 진행률과 설치 알림");
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private void saveDownloadedUpdate(File targetFile, String tag) throws IOException {
        boolean saved = prefs().edit()
                .putString(PREF_UPDATE_APK_PATH, targetFile.getAbsolutePath())
                .putString(PREF_UPDATE_TAG, tag)
                .remove(PREF_UPDATE_DOWNLOAD_ID)
                .commit();
        if (!saved) {
            throw new IOException("업데이트 상태를 저장할 수 없습니다.");
        }
    }

    private int terminalStartId(int fallbackStartId) {
        return latestStartId > 0 ? latestStartId : fallbackStartId;
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private File updateDownloadDir() {
        File root = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (root == null) {
            root = getFilesDir();
        }
        return new File(root, "updates");
    }

    private String updateApkFileName(String tagName, String apkName) {
        String tag = sanitizeFileSegment(tagName.isEmpty() ? "update" : tagName);
        String assetName = sanitizeFileSegment(apkName.isEmpty() ? "YTET.apk" : apkName);
        if (assetName.startsWith(tag + "-")) {
            return assetName;
        }
        return tag + "-" + assetName;
    }

    private String sanitizeFileSegment(String value) {
        String clean = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._-]+", "_");
        clean = clean.replaceAll("_+", "_");
        return clean.isEmpty() ? "YTET.apk" : clean;
    }

    private void deleteOtherUpdateApks(File keepFile) {
        File directory = keepFile == null ? null : keepFile.getParentFile();
        File[] files = directory == null ? null : directory.listFiles();
        if (files == null) {
            return;
        }
        String keepName = keepFile.getName();
        for (File file : files) {
            if (!file.isFile() || keepName.equals(file.getName())) {
                continue;
            }
            String name = file.getName().toLowerCase();
            if (name.endsWith(".apk") || name.endsWith(".apk.part")) {
                file.delete();
            }
        }
    }

    private void clearPartialDownloads() {
        File[] files = updateDownloadDir().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".part")) {
                file.delete();
            }
        }
    }

    private int downloadPercent(long downloadedBytes, long totalBytes) {
        if (totalBytes <= 0L) {
            return -1;
        }
        return Math.max(0, Math.min(100, Math.round(downloadedBytes * 100f / totalBytes)));
    }

    private String progressMessage(int percent, long totalBytes, long downloadedBytes) {
        if (percent >= 0 && totalBytes > 0L) {
            return percent + "% · " + MusicLibrary.formatBytes(downloadedBytes)
                    + " / " + MusicLibrary.formatBytes(totalBytes);
        }
        if (downloadedBytes > 0L) {
            return "다운로드 중 · " + MusicLibrary.formatBytes(downloadedBytes);
        }
        return "다운로드를 시작하는 중입니다.";
    }

    private String safeMessage(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        return message == null || message.trim().isEmpty() ? "알 수 없는 오류" : message.trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class InterruptedIOException extends IOException {
    }
}
