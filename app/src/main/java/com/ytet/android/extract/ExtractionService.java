package com.ytet.android.extract;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.ytet.android.R;
import com.ytet.android.core.ExtractionRequest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExtractionService extends Service {
    public static final String ACTION_PROGRESS = "com.ytet.android.action.EXTRACTION_PROGRESS";
    public static final String EXTRA_PERCENT = "com.ytet.android.extra.PERCENT";
    public static final String EXTRA_STAGE = "com.ytet.android.extra.STAGE";
    public static final String EXTRA_MESSAGE = "com.ytet.android.extra.MESSAGE";
    public static final String EXTRA_RESULT = "com.ytet.android.extra.RESULT";
    public static final String EXTRA_ERROR = "com.ytet.android.extra.ERROR";
    public static final String EXTRA_DONE = "com.ytet.android.extra.DONE";

    private static final String CHANNEL_ID = "ytet_extraction";
    private static final int NOTIFICATION_ID = 4207;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExtractorEngine engine = new YtDlpPythonEngine();

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, notification(0, "준비", "추출 작업 준비 중"));

        final ExtractionRequest request;
        try {
            request = ExtractionRequest.fromIntent(intent);
        } catch (IllegalArgumentException exception) {
            sendError(exception.getMessage());
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        executor.execute(() -> {
            try {
                ExtractionResult result = engine.extract(this, request, this::publishProgress);
                publishDone(result.summary());
            } catch (ExtractionException exception) {
                sendError(exception.getMessage());
            } finally {
                stopSelf(startId);
            }
        });

        return START_NOT_STICKY;
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

    private void publishProgress(int percent, String stage, String message) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification(percent, stage, message));

        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PERCENT, percent);
        intent.putExtra(EXTRA_STAGE, stage);
        intent.putExtra(EXTRA_MESSAGE, message);
        sendBroadcast(intent);
    }

    private void publishDone(String result) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification(100, "완료", "저장 완료"));

        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PERCENT, 100);
        intent.putExtra(EXTRA_STAGE, "완료");
        intent.putExtra(EXTRA_MESSAGE, "저장 완료");
        intent.putExtra(EXTRA_RESULT, result);
        intent.putExtra(EXTRA_DONE, true);
        sendBroadcast(intent);
    }

    private void sendError(String message) {
        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PERCENT, 0);
        intent.putExtra(EXTRA_STAGE, "오류");
        intent.putExtra(EXTRA_ERROR, message == null ? "알 수 없는 오류가 발생했습니다." : message);
        intent.putExtra(EXTRA_DONE, true);
        sendBroadcast(intent);
    }

    private Notification notification(int percent, String stage, String message) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(R.drawable.ic_stat_extract)
                .setContentTitle("YTET Android - " + stage)
                .setContentText(message)
                .setOngoing(percent < 100)
                .setOnlyAlertOnce(true);

        if (percent > 0 && percent < 100) {
            builder.setProgress(100, percent, false);
        } else {
            builder.setProgress(0, 0, percent == 0);
        }

        return builder.build();
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("YouTube 추출 진행 상태");
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }
}
