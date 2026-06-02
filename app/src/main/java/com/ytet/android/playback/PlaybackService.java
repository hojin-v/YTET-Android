package com.ytet.android.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import com.ytet.android.R;
import com.ytet.android.library.DeviceAudioTrack;
import com.ytet.android.stream.MusicStation;
import com.ytet.android.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

public final class PlaybackService extends Service {
    public static final String ACTION_PLAY_QUEUE = "com.ytet.android.action.PLAY_QUEUE";
    public static final String ACTION_TOGGLE = "com.ytet.android.action.PLAYBACK_TOGGLE";
    public static final String ACTION_PLAY = "com.ytet.android.action.PLAYBACK_PLAY";
    public static final String ACTION_PAUSE = "com.ytet.android.action.PLAYBACK_PAUSE";
    public static final String ACTION_NEXT = "com.ytet.android.action.PLAYBACK_NEXT";
    public static final String ACTION_PREVIOUS = "com.ytet.android.action.PLAYBACK_PREVIOUS";
    public static final String ACTION_STOP = "com.ytet.android.action.PLAYBACK_STOP";
    public static final String ACTION_REQUEST_STATE = "com.ytet.android.action.PLAYBACK_REQUEST_STATE";
    public static final String ACTION_STATE = "com.ytet.android.action.PLAYBACK_STATE";

    public static final String EXTRA_HAS_QUEUE = "com.ytet.android.extra.HAS_QUEUE";
    public static final String EXTRA_PLAYING = "com.ytet.android.extra.PLAYING";
    public static final String EXTRA_PREPARING = "com.ytet.android.extra.PREPARING";
    public static final String EXTRA_TITLE = "com.ytet.android.extra.PLAYBACK_TITLE";
    public static final String EXTRA_META = "com.ytet.android.extra.PLAYBACK_META";
    public static final String EXTRA_STATUS = "com.ytet.android.extra.PLAYBACK_STATUS";

    private static final String EXTRA_MIX_TITLE = "com.ytet.android.extra.MIX_TITLE";
    private static final String EXTRA_MIX_SUBTITLE = "com.ytet.android.extra.MIX_SUBTITLE";
    private static final String EXTRA_TRACK_IDS = "com.ytet.android.extra.TRACK_IDS";
    private static final String EXTRA_TRACK_TITLES = "com.ytet.android.extra.TRACK_TITLES";
    private static final String EXTRA_TRACK_ARTISTS = "com.ytet.android.extra.TRACK_ARTISTS";
    private static final String EXTRA_TRACK_ALBUMS = "com.ytet.android.extra.TRACK_ALBUMS";
    private static final String EXTRA_TRACK_DISPLAY_NAMES = "com.ytet.android.extra.TRACK_DISPLAY_NAMES";
    private static final String EXTRA_TRACK_FOLDERS = "com.ytet.android.extra.TRACK_FOLDERS";
    private static final String EXTRA_TRACK_URIS = "com.ytet.android.extra.TRACK_URIS";
    private static final String EXTRA_TRACK_DURATIONS = "com.ytet.android.extra.TRACK_DURATIONS";
    private static final String EXTRA_TRACK_SIZES = "com.ytet.android.extra.TRACK_SIZES";
    private static final String CHANNEL_ID = "ytet_playback";
    private static final int NOTIFICATION_ID = 4211;

    private final ArrayList<DeviceAudioTrack> queue = new ArrayList<>();
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = this::onAudioFocusChanged;

    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private AudioFocusRequest audioFocusRequest;
    private String mixTitle = "로컬 음악";
    private String mixSubtitle = "기기 저장 음악";
    private int queueIndex;
    private int playbackVersion;
    private boolean preparing;
    private boolean playing;
    private boolean startWhenPrepared;
    private boolean resumeOnAudioFocusGain;
    private String errorStatus;

    public static Intent playQueueIntent(Context context, MusicStation station, List<DeviceAudioTrack> tracks) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(ACTION_PLAY_QUEUE);
        intent.putExtra(EXTRA_MIX_TITLE, station == null ? "로컬 음악" : station.title());
        intent.putExtra(EXTRA_MIX_SUBTITLE, station == null ? "기기 저장 음악" : station.subtitle());

        int count = tracks == null ? 0 : tracks.size();
        long[] ids = new long[count];
        String[] titles = new String[count];
        String[] artists = new String[count];
        String[] albums = new String[count];
        String[] displayNames = new String[count];
        String[] folders = new String[count];
        String[] uris = new String[count];
        long[] durations = new long[count];
        long[] sizes = new long[count];

        for (int index = 0; index < count; index++) {
            DeviceAudioTrack track = tracks.get(index);
            ids[index] = track.id();
            titles[index] = track.title();
            artists[index] = track.artist();
            albums[index] = track.album();
            displayNames[index] = track.displayName();
            folders[index] = track.folder();
            uris[index] = track.contentUri();
            durations[index] = track.durationMs();
            sizes[index] = track.sizeBytes();
        }

        intent.putExtra(EXTRA_TRACK_IDS, ids);
        intent.putExtra(EXTRA_TRACK_TITLES, titles);
        intent.putExtra(EXTRA_TRACK_ARTISTS, artists);
        intent.putExtra(EXTRA_TRACK_ALBUMS, albums);
        intent.putExtra(EXTRA_TRACK_DISPLAY_NAMES, displayNames);
        intent.putExtra(EXTRA_TRACK_FOLDERS, folders);
        intent.putExtra(EXTRA_TRACK_URIS, uris);
        intent.putExtra(EXTRA_TRACK_DURATIONS, durations);
        intent.putExtra(EXTRA_TRACK_SIZES, sizes);
        return intent;
    }

    public static Intent commandIntent(Context context, String action) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ensureNotificationChannel();
        AudioAttributes attributes = audioAttributes();
        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setWillPauseWhenDucked(true)
                .build();
        mediaSession = new MediaSession(this, "YTETPlayback");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause(false);
            }

            @Override
            public void onSkipToNext() {
                playNext();
            }

            @Override
            public void onSkipToPrevious() {
                playPrevious();
            }

            @Override
            public void onStop() {
                stopPlayback();
            }
        });
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_TOGGLE : intent.getAction();
        if (ACTION_PLAY_QUEUE.equals(action)) {
            loadQueue(intent);
            prepareCurrentTrack();
        } else if (ACTION_PLAY.equals(action)) {
            play();
        } else if (ACTION_PAUSE.equals(action)) {
            pause(false);
        } else if (ACTION_NEXT.equals(action)) {
            playNext();
        } else if (ACTION_PREVIOUS.equals(action)) {
            playPrevious();
        } else if (ACTION_STOP.equals(action)) {
            stopPlayback();
        } else if (ACTION_REQUEST_STATE.equals(action)) {
            broadcastState();
            if (queue.isEmpty()) {
                stopSelf(startId);
            }
        } else {
            toggle();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        abandonAudioFocus();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        broadcastState();
        super.onDestroy();
    }

    private void loadQueue(Intent intent) {
        queue.clear();
        queueIndex = 0;
        errorStatus = null;
        mixTitle = safeExtra(intent, EXTRA_MIX_TITLE, "로컬 음악");
        mixSubtitle = safeExtra(intent, EXTRA_MIX_SUBTITLE, "기기 저장 음악");

        long[] ids = intent.getLongArrayExtra(EXTRA_TRACK_IDS);
        String[] titles = intent.getStringArrayExtra(EXTRA_TRACK_TITLES);
        String[] artists = intent.getStringArrayExtra(EXTRA_TRACK_ARTISTS);
        String[] albums = intent.getStringArrayExtra(EXTRA_TRACK_ALBUMS);
        String[] displayNames = intent.getStringArrayExtra(EXTRA_TRACK_DISPLAY_NAMES);
        String[] folders = intent.getStringArrayExtra(EXTRA_TRACK_FOLDERS);
        String[] uris = intent.getStringArrayExtra(EXTRA_TRACK_URIS);
        long[] durations = intent.getLongArrayExtra(EXTRA_TRACK_DURATIONS);
        long[] sizes = intent.getLongArrayExtra(EXTRA_TRACK_SIZES);

        int count = ids == null ? 0 : ids.length;
        for (int index = 0; index < count; index++) {
            String uri = stringAt(uris, index, "");
            if (uri.trim().isEmpty()) {
                continue;
            }
            queue.add(new DeviceAudioTrack(
                    ids[index],
                    stringAt(titles, index, "제목 없음"),
                    stringAt(artists, index, "알 수 없는 아티스트"),
                    stringAt(albums, index, "앨범 정보 없음"),
                    stringAt(displayNames, index, stringAt(titles, index, "음악 파일")),
                    stringAt(folders, index, "알 수 없는 폴더"),
                    uri,
                    longAt(durations, index),
                    longAt(sizes, index)
            ));
        }
    }

    private void prepareCurrentTrack() {
        if (queue.isEmpty()) {
            errorStatus = "재생할 로컬 음악이 없습니다.";
            preparing = false;
            playing = false;
            updateTransportState();
            showNotification();
            broadcastState();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return;
        }

        int version = ++playbackVersion;
        preparing = true;
        playing = false;
        startWhenPrepared = true;
        errorStatus = null;
        updateTransportState();
        showNotification();
        broadcastState();
        releasePlayer();

        DeviceAudioTrack track = currentTrack();
        try {
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(audioAttributes());
            player.setDataSource(this, Uri.parse(track.contentUri()));
            player.setOnPreparedListener(prepared -> {
                if (version != playbackVersion) {
                    prepared.release();
                    return;
                }
                mediaPlayer = prepared;
                preparing = false;
                if (startWhenPrepared && requestAudioFocus()) {
                    prepared.start();
                    playing = true;
                } else if (startWhenPrepared) {
                    errorStatus = "오디오 포커스를 얻을 수 없습니다.";
                    playing = false;
                } else {
                    playing = false;
                }
                updateTransportState();
                showNotification();
                broadcastState();
            });
            player.setOnCompletionListener(completed -> playNext());
            player.setOnErrorListener((failed, what, extra) -> {
                if (version == playbackVersion) {
                    handlePlaybackError("이 파일을 재생할 수 없습니다: " + track.displayName());
                }
                return true;
            });
            mediaPlayer = player;
            player.prepareAsync();
        } catch (Exception exception) {
            handlePlaybackError("재생 준비 실패: " + track.displayName());
        }
    }

    private void toggle() {
        if (playing) {
            pause(false);
        } else {
            play();
        }
    }

    private void play() {
        if (queue.isEmpty()) {
            return;
        }
        if (mediaPlayer == null || errorStatus != null) {
            prepareCurrentTrack();
            return;
        }
        if (preparing || playing) {
            startWhenPrepared = true;
            return;
        }
        if (!requestAudioFocus()) {
            errorStatus = "오디오 포커스를 얻을 수 없습니다.";
            updateTransportState();
            showNotification();
            broadcastState();
            return;
        }
        try {
            mediaPlayer.start();
            playing = true;
            errorStatus = null;
            updateTransportState();
            showNotification();
            broadcastState();
        } catch (IllegalStateException exception) {
            handlePlaybackError("재생을 다시 시작할 수 없습니다.");
        }
    }

    private void pause(boolean resumeAfterFocusGain) {
        if (mediaPlayer == null) {
            return;
        }
        boolean wasPreparing = preparing;
        try {
            if (playing || mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        } catch (IllegalStateException ignored) {
            // Treat a stale player as paused; the next play action will prepare the track again.
        }
        playing = false;
        preparing = wasPreparing;
        startWhenPrepared = false;
        resumeOnAudioFocusGain = resumeAfterFocusGain;
        updateTransportState();
        showNotification();
        broadcastState();
    }

    private void playNext() {
        if (queue.isEmpty()) {
            return;
        }
        queueIndex = (queueIndex + 1) % queue.size();
        prepareCurrentTrack();
    }

    private void playPrevious() {
        if (queue.isEmpty()) {
            return;
        }
        queueIndex = queueIndex == 0 ? queue.size() - 1 : queueIndex - 1;
        prepareCurrentTrack();
    }

    private void stopPlayback() {
        queue.clear();
        queueIndex = 0;
        preparing = false;
        playing = false;
        startWhenPrepared = false;
        errorStatus = null;
        releasePlayer();
        abandonAudioFocus();
        updateTransportState();
        broadcastState();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void handlePlaybackError(String message) {
        preparing = false;
        playing = false;
        startWhenPrepared = false;
        errorStatus = message;
        updateTransportState();
        showNotification();
        broadcastState();
    }

    private void onAudioFocusChanged(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            pause(false);
            abandonAudioFocus();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pause(true);
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            setPlayerVolume(0.25f);
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            setPlayerVolume(1f);
            if (resumeOnAudioFocusGain) {
                resumeOnAudioFocusGain = false;
                play();
            }
        }
    }

    private boolean requestAudioFocus() {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (manager == null) {
            return true;
        }
        return manager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (manager != null && audioFocusRequest != null) {
            manager.abandonAudioFocusRequest(audioFocusRequest);
        }
        resumeOnAudioFocusGain = false;
    }

    private void releasePlayer() {
        if (mediaPlayer == null) {
            return;
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.release();
        } catch (IllegalStateException ignored) {
            mediaPlayer.release();
        }
        mediaPlayer = null;
    }

    private void setPlayerVolume(float volume) {
        if (mediaPlayer == null) {
            return;
        }
        try {
            mediaPlayer.setVolume(volume, volume);
        } catch (IllegalStateException ignored) {
            // Volume changes are best effort while the player is transitioning.
        }
    }

    private void showNotification() {
        Notification notification = notification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification notification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        DeviceAudioTrack track = currentTrack();
        builder.setSmallIcon(R.drawable.ic_stat_playback)
                .setContentTitle(track == null ? "YTET 로컬 플레이어" : track.title())
                .setContentText(track == null ? "기기 저장 음악" : track.artist() + " · " + mixTitle)
                .setContentIntent(contentIntent())
                .setDeleteIntent(serviceAction(ACTION_STOP, 5))
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setOngoing(playing || preparing);

        builder.addAction(android.R.drawable.ic_media_previous, "이전", serviceAction(ACTION_PREVIOUS, 1));
        builder.addAction(
                playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                playing ? "일시정지" : "재생",
                serviceAction(ACTION_TOGGLE, 2)
        );
        builder.addAction(android.R.drawable.ic_media_next, "다음", serviceAction(ACTION_NEXT, 3));
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "닫기", serviceAction(ACTION_STOP, 4));

        if (mediaSession != null) {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2));
        }
        return builder.build();
    }

    private PendingIntent contentIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, intent, pendingIntentFlags());
    }

    private PendingIntent serviceAction(String action, int requestCode) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, requestCode, intent, pendingIntentFlags());
    }

    private int pendingIntentFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }

    private void updateTransportState() {
        if (mediaSession == null) {
            return;
        }
        DeviceAudioTrack track = currentTrack();
        if (track != null) {
            mediaSession.setMetadata(new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, track.title())
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album())
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, track.durationMs())
                    .build());
        }

        int state;
        if (errorStatus != null) {
            state = PlaybackState.STATE_ERROR;
        } else if (preparing) {
            state = PlaybackState.STATE_BUFFERING;
        } else if (playing) {
            state = PlaybackState.STATE_PLAYING;
        } else if (track != null) {
            state = PlaybackState.STATE_PAUSED;
        } else {
            state = PlaybackState.STATE_STOPPED;
        }

        long actions = PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_SKIP_TO_NEXT
                | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                | PlaybackState.ACTION_STOP;
        PlaybackState.Builder builder = new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, currentPosition(), playing ? 1f : 0f);
        if (errorStatus != null) {
            builder.setErrorMessage(errorStatus);
        }
        mediaSession.setPlaybackState(builder.build());
    }

    private long currentPosition() {
        if (mediaPlayer == null) {
            return 0;
        }
        try {
            return mediaPlayer.getCurrentPosition();
        } catch (IllegalStateException exception) {
            return 0;
        }
    }

    private void broadcastState() {
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(getPackageName());
        DeviceAudioTrack track = currentTrack();
        intent.putExtra(EXTRA_HAS_QUEUE, !queue.isEmpty());
        intent.putExtra(EXTRA_PLAYING, playing);
        intent.putExtra(EXTRA_PREPARING, preparing);
        intent.putExtra(EXTRA_TITLE, track == null ? "로컬 재생 대기" : track.title());
        intent.putExtra(EXTRA_META, track == null ? "기기 음악을 스캔하면 재생할 수 있습니다." : track.artist() + " · " + mixTitle);
        intent.putExtra(EXTRA_STATUS, statusText(track));
        sendBroadcast(intent);
    }

    private String statusText(DeviceAudioTrack track) {
        if (errorStatus != null) {
            return errorStatus;
        }
        if (preparing) {
            return "준비 중: " + (track == null ? mixTitle : track.title());
        }
        if (track == null) {
            return "로컬 재생 대기";
        }
        if (playing) {
            return "재생 중: " + track.title() + " · " + track.artist();
        }
        return "일시정지: " + track.title() + " · " + track.artist();
    }

    private DeviceAudioTrack currentTrack() {
        if (queue.isEmpty()) {
            return null;
        }
        if (queueIndex < 0 || queueIndex >= queue.size()) {
            queueIndex = 0;
        }
        return queue.get(queueIndex);
    }

    private AudioAttributes audioAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("기기 저장 음악 재생 컨트롤");
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private static String safeExtra(Intent intent, String key, String fallback) {
        String value = intent.getStringExtra(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String stringAt(String[] values, int index, String fallback) {
        if (values == null || index < 0 || index >= values.length || values[index] == null) {
            return fallback;
        }
        return values[index];
    }

    private static long longAt(long[] values, int index) {
        if (values == null || index < 0 || index >= values.length) {
            return 0;
        }
        return values[index];
    }
}
