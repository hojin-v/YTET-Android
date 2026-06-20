package com.ytet.android.extract;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import com.ytet.android.R;
import com.ytet.android.core.ExtractionRequest;
import com.ytet.android.ui.MainActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ExtractionService extends Service {
    public static final String ACTION_PROGRESS = "com.ytet.android.action.EXTRACTION_PROGRESS";
    public static final String ACTION_CANCEL = "com.ytet.android.action.CANCEL_EXTRACTION";
    public static final String EXTRA_PERCENT = "com.ytet.android.extra.PERCENT";
    public static final String EXTRA_STAGE = "com.ytet.android.extra.STAGE";
    public static final String EXTRA_MESSAGE = "com.ytet.android.extra.MESSAGE";
    public static final String EXTRA_RESULT = "com.ytet.android.extra.RESULT";
    public static final String EXTRA_ERROR = "com.ytet.android.extra.ERROR";
    public static final String EXTRA_DONE = "com.ytet.android.extra.DONE";
    public static final String EXTRA_CANCELED = "com.ytet.android.extra.CANCELED";

    private static final String CHANNEL_ID = "ytet_extraction";
    private static final int NOTIFICATION_ID = 4207;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExtractorEngine engine = new YtDlpPythonEngine();
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private volatile Future<?> currentTask;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            requestCancellation();
            return START_NOT_STICKY;
        }

        cancelRequested.set(false);
        startForeground(NOTIFICATION_ID, notification(0, "준비", "추출 작업 준비 중"));

        final ExtractionRequest request;
        try {
            request = ExtractionRequest.fromIntent(intent);
        } catch (IllegalArgumentException exception) {
            sendError(exception.getMessage());
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        currentTask = executor.submit(() -> {
            try {
                ExtractionResult result = engine.extract(this, request, this::publishProgress, this::isCancellationRequested);
                publishDone(result.summary(), result.hasPartialFailure());
            } catch (ExtractionCanceledException exception) {
                publishCanceled();
            } catch (ExtractionException exception) {
                sendError(exception.getMessage());
            } finally {
                currentTask = null;
                stopSelf(startId);
            }
        });

        return START_NOT_STICKY;
    }

    public static Intent cancelIntent(Context context) {
        Intent intent = new Intent(context, ExtractionService.class);
        intent.setAction(ACTION_CANCEL);
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

    private void requestCancellation() {
        cancelRequested.set(true);
        Future<?> task = currentTask;
        if (task == null || task.isDone()) {
            publishCanceled();
            stopSelf();
            return;
        }
        publishProgress(0, "취소", "추출을 취소하는 중");
    }

    private boolean isCancellationRequested() {
        return cancelRequested.get() || Thread.currentThread().isInterrupted();
    }

    private void publishDone(String result, boolean partialFailure) {
        String stage = partialFailure ? "부분 완료" : "완료";
        String message = partialFailure ? "일부 항목 실패" : "저장 완료";
        detachForegroundNotification();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification(100, stage, message, true, false));

        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PERCENT, 100);
        intent.putExtra(EXTRA_STAGE, stage);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_RESULT, result);
        intent.putExtra(EXTRA_DONE, true);
        sendBroadcast(intent);
    }

    private void sendError(String message) {
        String safeMessage = message == null ? "알 수 없는 오류가 발생했습니다." : message;
        detachForegroundNotification();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification(0, "실패", safeMessage, true, true));

        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PERCENT, 0);
        intent.putExtra(EXTRA_STAGE, "오류");
        intent.putExtra(EXTRA_ERROR, safeMessage);
        intent.putExtra(EXTRA_DONE, true);
        sendBroadcast(intent);
    }

    private void publishCanceled() {
        detachForegroundNotification();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification(0, "취소됨", "추출을 취소했습니다.", true, false));

        Intent intent = new Intent(ACTION_PROGRESS);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PERCENT, 0);
        intent.putExtra(EXTRA_STAGE, "취소됨");
        intent.putExtra(EXTRA_MESSAGE, "추출을 취소했습니다.");
        intent.putExtra(EXTRA_RESULT, "추출을 취소했습니다.");
        intent.putExtra(EXTRA_DONE, true);
        intent.putExtra(EXTRA_CANCELED, true);
        sendBroadcast(intent);
    }

    private Notification notification(int percent, String stage, String message) {
        return notification(percent, stage, message, percent >= 100, false);
    }

    private Notification notification(int percent, String stage, String message, boolean finished, boolean failed) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(R.drawable.ic_stat_extract)
                .setContentTitle("RabbYT - " + stage)
                .setContentText(message)
                .setContentIntent(contentIntent())
                .setAutoCancel(finished)
                .setOngoing(!finished)
                .setOnlyAlertOnce(!finished);

        if (!finished) {
            builder.addAction(R.drawable.ic_close, "취소", cancelPendingIntent());
        }

        if (failed) {
            builder.setProgress(0, 0, false);
        } else if (percent > 0 && percent < 100) {
            builder.setProgress(100, percent, false);
        } else {
            builder.setProgress(0, 0, percent == 0 && !finished);
        }

        return builder.build();
    }

    private PendingIntent cancelPendingIntent() {
        return PendingIntent.getService(
                this,
                1,
                cancelIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent contentIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void detachForegroundNotification() {
        stopForeground(STOP_FOREGROUND_DETACH);
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
