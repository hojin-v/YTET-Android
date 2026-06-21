package com.ytet.android.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.ytet.android.R;
import com.ytet.android.library.DeviceAudioTrack;
import com.ytet.android.library.DeviceMusicLibrary;
import com.ytet.android.stream.MusicStation;
import com.ytet.android.stream.OnlineStreamResolver;
import com.ytet.android.ui.MainActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PlaybackService extends Service {
    public static final String ACTION_PLAY_QUEUE = "com.ytet.android.action.PLAY_QUEUE";
    public static final String ACTION_PLAY_ONLINE_QUEUE = "com.ytet.android.action.PLAY_ONLINE_QUEUE";
    public static final String ACTION_TOGGLE = "com.ytet.android.action.PLAYBACK_TOGGLE";
    public static final String ACTION_PLAY = "com.ytet.android.action.PLAYBACK_PLAY";
    public static final String ACTION_PAUSE = "com.ytet.android.action.PLAYBACK_PAUSE";
    public static final String ACTION_NEXT = "com.ytet.android.action.PLAYBACK_NEXT";
    public static final String ACTION_PREVIOUS = "com.ytet.android.action.PLAYBACK_PREVIOUS";
    public static final String ACTION_STOP = "com.ytet.android.action.PLAYBACK_STOP";
    public static final String ACTION_SEEK_TO = "com.ytet.android.action.PLAYBACK_SEEK_TO";
    public static final String ACTION_SEEK_TO_QUEUE_INDEX = "com.ytet.android.action.PLAYBACK_SEEK_TO_QUEUE_INDEX";
    public static final String ACTION_TOGGLE_SHUFFLE = "com.ytet.android.action.PLAYBACK_TOGGLE_SHUFFLE";
    public static final String ACTION_TOGGLE_REPEAT = "com.ytet.android.action.PLAYBACK_TOGGLE_REPEAT";
    private static final String ACTION_PREVIOUS_UNAVAILABLE = "com.ytet.android.action.PLAYBACK_PREVIOUS_UNAVAILABLE";
    private static final String ACTION_NEXT_UNAVAILABLE = "com.ytet.android.action.PLAYBACK_NEXT_UNAVAILABLE";
    public static final String ACTION_PLAY_NEXT = "com.ytet.android.action.PLAYBACK_PLAY_NEXT";
    public static final String ACTION_ADD_TO_QUEUE = "com.ytet.android.action.PLAYBACK_ADD_TO_QUEUE";
    public static final String ACTION_REORDER_QUEUE = "com.ytet.android.action.PLAYBACK_REORDER_QUEUE";
    public static final String ACTION_SET_SLEEP_TIMER = "com.ytet.android.action.PLAYBACK_SET_SLEEP_TIMER";
    public static final String ACTION_CANCEL_SLEEP_TIMER = "com.ytet.android.action.PLAYBACK_CANCEL_SLEEP_TIMER";
    public static final String ACTION_TOGGLE_SLEEP_TIMER_PAUSE = "com.ytet.android.action.PLAYBACK_TOGGLE_SLEEP_TIMER_PAUSE";
    public static final String ACTION_SLEEP_TIMER_FINISHED = "com.ytet.android.action.PLAYBACK_SLEEP_TIMER_FINISHED";
    public static final String ACTION_REQUEST_STATE = "com.ytet.android.action.PLAYBACK_REQUEST_STATE";
    public static final String ACTION_STATE = "com.ytet.android.action.PLAYBACK_STATE";

    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;

    public static final String EXTRA_HAS_QUEUE = "com.ytet.android.extra.HAS_QUEUE";
    public static final String EXTRA_PLAYING = "com.ytet.android.extra.PLAYING";
    public static final String EXTRA_PREPARING = "com.ytet.android.extra.PREPARING";
    public static final String EXTRA_WILL_PLAY = "com.ytet.android.extra.WILL_PLAY";
    public static final String EXTRA_ERROR = "com.ytet.android.extra.PLAYBACK_ERROR";
    public static final String EXTRA_TITLE = "com.ytet.android.extra.PLAYBACK_TITLE";
    public static final String EXTRA_META = "com.ytet.android.extra.PLAYBACK_META";
    public static final String EXTRA_STATUS = "com.ytet.android.extra.PLAYBACK_STATUS";
    public static final String EXTRA_TRACK_ID = "com.ytet.android.extra.PLAYBACK_TRACK_ID";
    public static final String EXTRA_ARTIST = "com.ytet.android.extra.PLAYBACK_ARTIST";
    public static final String EXTRA_ALBUM = "com.ytet.android.extra.PLAYBACK_ALBUM";
    public static final String EXTRA_FOLDER = "com.ytet.android.extra.PLAYBACK_FOLDER";
    public static final String EXTRA_ALBUM_ART_URI = "com.ytet.android.extra.PLAYBACK_ALBUM_ART_URI";
    public static final String EXTRA_DURATION_MS = "com.ytet.android.extra.PLAYBACK_DURATION_MS";
    public static final String EXTRA_POSITION_MS = "com.ytet.android.extra.PLAYBACK_POSITION_MS";
    public static final String EXTRA_QUEUE_INDEX = "com.ytet.android.extra.PLAYBACK_QUEUE_INDEX";
    public static final String EXTRA_QUEUE_SIZE = "com.ytet.android.extra.PLAYBACK_QUEUE_SIZE";
    public static final String EXTRA_MIX = "com.ytet.android.extra.PLAYBACK_MIX";
    public static final String EXTRA_SHUFFLE_ENABLED = "com.ytet.android.extra.PLAYBACK_SHUFFLE_ENABLED";
    public static final String EXTRA_REPEAT_MODE = "com.ytet.android.extra.PLAYBACK_REPEAT_MODE";
    public static final String EXTRA_QUEUE_TRACK_IDS = "com.ytet.android.extra.PLAYBACK_QUEUE_TRACK_IDS";
    public static final String EXTRA_QUEUE_TITLES = "com.ytet.android.extra.PLAYBACK_QUEUE_TITLES";
    public static final String EXTRA_QUEUE_ARTISTS = "com.ytet.android.extra.PLAYBACK_QUEUE_ARTISTS";
    public static final String EXTRA_QUEUE_ALBUMS = "com.ytet.android.extra.PLAYBACK_QUEUE_ALBUMS";
    public static final String EXTRA_QUEUE_URLS = "com.ytet.android.extra.PLAYBACK_QUEUE_URLS";
    public static final String EXTRA_QUEUE_THUMBNAILS = "com.ytet.android.extra.PLAYBACK_QUEUE_THUMBNAILS";
    public static final String EXTRA_QUEUE_DURATIONS = "com.ytet.android.extra.PLAYBACK_QUEUE_DURATIONS";
    public static final String EXTRA_SLEEP_TIMER_MINUTES = "com.ytet.android.extra.PLAYBACK_SLEEP_TIMER_MINUTES";
    public static final String EXTRA_SLEEP_TIMER_END_AT_MS = "com.ytet.android.extra.PLAYBACK_SLEEP_TIMER_END_AT_MS";
    public static final String EXTRA_SLEEP_TIMER_REMAINING_MS = "com.ytet.android.extra.PLAYBACK_SLEEP_TIMER_REMAINING_MS";
    public static final String EXTRA_SLEEP_TIMER_PAUSED = "com.ytet.android.extra.PLAYBACK_SLEEP_TIMER_PAUSED";
    public static final String EXTRA_SEEK_POSITION_MS = "com.ytet.android.extra.SEEK_POSITION_MS";

    private static final String EXTRA_MIX_TITLE = "com.ytet.android.extra.MIX_TITLE";
    private static final String EXTRA_MIX_SUBTITLE = "com.ytet.android.extra.MIX_SUBTITLE";
    private static final String EXTRA_TRACK_IDS = "com.ytet.android.extra.TRACK_IDS";
    private static final String EXTRA_ONLINE_TITLES = "com.ytet.android.extra.ONLINE_TITLES";
    private static final String EXTRA_ONLINE_ARTISTS = "com.ytet.android.extra.ONLINE_ARTISTS";
    private static final String EXTRA_ONLINE_ALBUMS = "com.ytet.android.extra.ONLINE_ALBUMS";
    private static final String EXTRA_ONLINE_URLS = "com.ytet.android.extra.ONLINE_URLS";
    private static final String EXTRA_ONLINE_THUMBNAILS = "com.ytet.android.extra.ONLINE_THUMBNAILS";
    private static final String EXTRA_ONLINE_DURATIONS = "com.ytet.android.extra.ONLINE_DURATIONS";
    private static final String EXTRA_START_INDEX = "com.ytet.android.extra.START_INDEX";
    private static final String EXTRA_QUEUE_SOURCE_INDICES = "com.ytet.android.extra.QUEUE_SOURCE_INDICES";
    private static final String EXTRA_CURRENT_QUEUE_SOURCE_INDEX = "com.ytet.android.extra.CURRENT_QUEUE_SOURCE_INDEX";
    private static final String CHANNEL_ID = "ytet_playback";
    private static final int NOTIFICATION_ID = 4211;
    private static final String PREFS = "ytet_android";
    private static final String PREF_PLAYBACK_QUEUE_IDS = "playback_queue_ids";
    private static final String PREF_PLAYBACK_ORIGINAL_QUEUE_IDS = "playback_original_queue_ids";
    private static final String PREF_PLAYBACK_QUEUE_INDEX = "playback_queue_index";
    private static final String PREF_PLAYBACK_TRACK_ID = "playback_track_id";
    private static final String PREF_PLAYBACK_MIX_TITLE = "playback_mix_title";
    private static final String PREF_PLAYBACK_MIX_SUBTITLE = "playback_mix_subtitle";
    private static final String PREF_PLAYBACK_SHUFFLE = "playback_shuffle";
    private static final String PREF_PLAYBACK_REPEAT = "playback_repeat";
    private static final String PREF_PLAYBACK_QUEUE_SNAPSHOT = "playback_queue_snapshot";
    private static final String PREF_PLAYBACK_ORIGINAL_QUEUE_SNAPSHOT = "playback_original_queue_snapshot";

    private final ArrayList<DeviceAudioTrack> queue = new ArrayList<>();
    private final ArrayList<DeviceAudioTrack> originalQueue = new ArrayList<>();
    private final DeviceMusicLibrary musicLibrary = new DeviceMusicLibrary();
    private final ExecutorService queueExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService onlineResolveExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService artworkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = this::onAudioFocusChanged;
    private final BroadcastReceiver noisyAudioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                pauseForOutputDisconnect();
            }
        }
    };
    private final Runnable stateTick = new Runnable() {
        @Override
        public void run() {
            if (playing || preparing || isSleepTimerRunning()) {
                broadcastState();
            }
        }
    };
    private final Runnable sleepTimerRunnable = new Runnable() {
        @Override
        public void run() {
            sleepTimerEndAtMs = 0L;
            sleepTimerRemainingMs = 0L;
            sleepTimerPaused = false;
            stopPlayback();
            Intent finished = new Intent(ACTION_SLEEP_TIMER_FINISHED);
            finished.setPackage(getPackageName());
            sendBroadcast(finished);
        }
    };

    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private AudioFocusRequest audioFocusRequest;
    private String mixTitle = "로컬 음악";
    private String mixSubtitle = "기기 저장 음악";
    private int queueIndex;
    private int playbackVersion;
    private int queueLoadVersion;
    private int countedPlaybackVersion = -1;
    private int failedTrackSkips;
    private boolean preparing;
    private boolean playing;
    private boolean startWhenPrepared;
    private boolean resumeOnAudioFocusGain;
    private boolean noisyAudioReceiverRegistered;
    private boolean shuffleEnabled;
    private int repeatMode = REPEAT_OFF;
    private long sleepTimerEndAtMs;
    private long sleepTimerRemainingMs;
    private boolean sleepTimerPaused;
    private String errorStatus;
    private String cachedArtworkUri = "";
    private String loadingArtworkUri = "";
    private Bitmap cachedArtwork;

    public static Intent playQueueIntent(Context context, MusicStation station, List<DeviceAudioTrack> tracks) {
        return playQueueIntent(context, station, tracks, 0);
    }

    public static Intent playQueueIntent(Context context, MusicStation station, List<DeviceAudioTrack> tracks, int startIndex) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(ACTION_PLAY_QUEUE);
        intent.putExtra(EXTRA_MIX_TITLE, station == null ? "로컬 음악" : station.title());
        intent.putExtra(EXTRA_MIX_SUBTITLE, station == null ? "기기 저장 음악" : station.subtitle());
        intent.putExtra(EXTRA_SHUFFLE_ENABLED, station != null
                && station.mixType() != MusicStation.MixType.TRACK
                && tracks != null
                && tracks.size() > 1);

        int count = tracks == null ? 0 : tracks.size();
        long[] ids = new long[count];

        for (int index = 0; index < count; index++) {
            ids[index] = tracks.get(index).id();
        }

        intent.putExtra(EXTRA_TRACK_IDS, ids);
        intent.putExtra(EXTRA_START_INDEX, Math.max(0, startIndex));
        return intent;
    }

    public static Intent playOnlineQueueIntent(Context context, MusicStation station, List<DeviceAudioTrack> tracks, int startIndex) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(ACTION_PLAY_ONLINE_QUEUE);
        intent.putExtra(EXTRA_MIX_TITLE, station == null ? "스트림" : station.title());
        intent.putExtra(EXTRA_MIX_SUBTITLE, station == null ? "온라인 재생" : station.subtitle());
        intent.putExtra(EXTRA_SHUFFLE_ENABLED, false);
        int count = tracks == null ? 0 : tracks.size();
        long[] ids = new long[count];
        String[] titles = new String[count];
        String[] artists = new String[count];
        String[] albums = new String[count];
        String[] urls = new String[count];
        String[] thumbnails = new String[count];
        long[] durations = new long[count];
        for (int index = 0; index < count; index++) {
            DeviceAudioTrack track = tracks.get(index);
            ids[index] = track.id();
            titles[index] = track.title();
            artists[index] = track.artist();
            albums[index] = track.album();
            urls[index] = track.contentUri();
            thumbnails[index] = track.albumArtUri();
            durations[index] = track.durationMs();
        }
        intent.putExtra(EXTRA_TRACK_IDS, ids);
        intent.putExtra(EXTRA_ONLINE_TITLES, titles);
        intent.putExtra(EXTRA_ONLINE_ARTISTS, artists);
        intent.putExtra(EXTRA_ONLINE_ALBUMS, albums);
        intent.putExtra(EXTRA_ONLINE_URLS, urls);
        intent.putExtra(EXTRA_ONLINE_THUMBNAILS, thumbnails);
        intent.putExtra(EXTRA_ONLINE_DURATIONS, durations);
        intent.putExtra(EXTRA_START_INDEX, Math.max(0, startIndex));
        return intent;
    }

    public static Intent commandIntent(Context context, String action) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        return intent;
    }

    public static Intent seekIntent(Context context, long positionMs) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(ACTION_SEEK_TO);
        intent.putExtra(EXTRA_SEEK_POSITION_MS, positionMs);
        return intent;
    }

    public static Intent seekQueueIndexIntent(Context context, int index) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(ACTION_SEEK_TO_QUEUE_INDEX);
        intent.putExtra(EXTRA_START_INDEX, Math.max(0, index));
        return intent;
    }

    public static Intent queueEditIntent(Context context, String action, List<DeviceAudioTrack> tracks) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(action);
        int count = tracks == null ? 0 : tracks.size();
        long[] ids = new long[count];
        for (int index = 0; index < count; index++) {
            ids[index] = tracks.get(index).id();
        }
        intent.putExtra(EXTRA_TRACK_IDS, ids);
        return intent;
    }

    public static Intent reorderQueueIntent(Context context, List<DeviceAudioTrack> tracks) {
        return queueEditIntent(context, ACTION_REORDER_QUEUE, tracks);
    }

    public static Intent reorderQueueIntent(
            Context context,
            List<DeviceAudioTrack> tracks,
            int[] sourceIndices,
            int currentSourceIndex
    ) {
        Intent intent = reorderQueueIntent(context, tracks);
        intent.putExtra(EXTRA_QUEUE_SOURCE_INDICES, sourceIndices);
        intent.putExtra(EXTRA_CURRENT_QUEUE_SOURCE_INDEX, currentSourceIndex);
        return intent;
    }

    public static Intent sleepTimerIntent(Context context, int minutes) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(minutes <= 0 ? ACTION_CANCEL_SLEEP_TIMER : ACTION_SET_SLEEP_TIMER);
        intent.putExtra(EXTRA_SLEEP_TIMER_MINUTES, minutes);
        return intent;
    }

    public static Intent toggleSleepTimerPauseIntent(Context context) {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(ACTION_TOGGLE_SLEEP_TIMER_PAUSE);
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
                .build();
        mediaSession = new MediaSession(this, "RabbYTPlayback");
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
            public void onSeekTo(long pos) {
                seekTo(pos);
            }

            @Override
            public void onStop() {
                stopPlayback();
            }

            @Override
            public void onCustomAction(String action, Bundle extras) {
                if (ACTION_TOGGLE_SHUFFLE.equals(action)) {
                    toggleShuffle();
                } else if (ACTION_TOGGLE_REPEAT.equals(action)) {
                    toggleRepeat();
                } else if (ACTION_PREVIOUS_UNAVAILABLE.equals(action)
                        || ACTION_NEXT_UNAVAILABLE.equals(action)) {
                    broadcastState();
                }
            }
        });
        mediaSession.setActive(true);
        registerNoisyAudioReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_TOGGLE : intent.getAction();
        if (ACTION_PLAY_QUEUE.equals(action)) {
            loadQueueAsync(intent);
        } else if (ACTION_PLAY_ONLINE_QUEUE.equals(action)) {
            loadOnlineQueue(intent);
        } else if (ACTION_PLAY.equals(action)) {
            play();
        } else if (ACTION_PAUSE.equals(action)) {
            pause(false);
        } else if (ACTION_NEXT.equals(action)) {
            playNext();
        } else if (ACTION_PREVIOUS.equals(action)) {
            playPrevious();
        } else if (ACTION_SEEK_TO.equals(action)) {
            seekTo(intent.getLongExtra(EXTRA_SEEK_POSITION_MS, 0L));
        } else if (ACTION_SEEK_TO_QUEUE_INDEX.equals(action)) {
            seekToQueueIndex(intent.getIntExtra(EXTRA_START_INDEX, 0));
        } else if (ACTION_STOP.equals(action)) {
            stopPlayback();
        } else if (ACTION_TOGGLE_SHUFFLE.equals(action)) {
            toggleShuffle();
        } else if (ACTION_TOGGLE_REPEAT.equals(action)) {
            toggleRepeat();
        } else if (ACTION_PLAY_NEXT.equals(action)) {
            editQueueAsync(intent, true);
        } else if (ACTION_ADD_TO_QUEUE.equals(action)) {
            editQueueAsync(intent, false);
        } else if (ACTION_REORDER_QUEUE.equals(action)) {
            reorderQueue(
                    intent.getLongArrayExtra(EXTRA_TRACK_IDS),
                    intent.getIntArrayExtra(EXTRA_QUEUE_SOURCE_INDICES),
                    intent.getIntExtra(EXTRA_CURRENT_QUEUE_SOURCE_INDEX, -1)
            );
        } else if (ACTION_SET_SLEEP_TIMER.equals(action)) {
            setSleepTimer(intent.getIntExtra(EXTRA_SLEEP_TIMER_MINUTES, 0));
        } else if (ACTION_CANCEL_SLEEP_TIMER.equals(action)) {
            cancelSleepTimer(true);
        } else if (ACTION_TOGGLE_SLEEP_TIMER_PAUSE.equals(action)) {
            toggleSleepTimerPause();
        } else if (ACTION_REQUEST_STATE.equals(action)) {
            if (queue.isEmpty() && restoreQueueAsync(startId)) {
                return START_NOT_STICKY;
            }
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
        queueExecutor.shutdownNow();
        onlineResolveExecutor.shutdownNow();
        artworkExecutor.shutdownNow();
        mainHandler.removeCallbacks(stateTick);
        mainHandler.removeCallbacks(sleepTimerRunnable);
        unregisterNoisyAudioReceiver();
        releasePlayer();
        abandonAudioFocus();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        broadcastState();
        super.onDestroy();
    }

    private void loadQueueAsync(Intent intent) {
        int version = ++queueLoadVersion;
        long[] ids = intent.getLongArrayExtra(EXTRA_TRACK_IDS);
        int startIndex = Math.max(0, intent.getIntExtra(EXTRA_START_INDEX, 0));

        failedTrackSkips = 0;
        errorStatus = null;
        mixTitle = safeExtra(intent, EXTRA_MIX_TITLE, "로컬 음악");
        mixSubtitle = safeExtra(intent, EXTRA_MIX_SUBTITLE, "기기 저장 음악");
        shuffleEnabled = intent.getBooleanExtra(EXTRA_SHUFFLE_ENABLED, false);
        repeatMode = REPEAT_OFF;
        preparing = true;
        playing = false;
        startWhenPrepared = true;
        releasePlayer();
        abandonAudioFocus();
        updateTransportState();
        showNotification();
        broadcastState();

        queueExecutor.execute(() -> {
            List<DeviceAudioTrack> loadedTracks;
            try {
                loadedTracks = musicLibrary.loadTracksByIds(this, ids);
            } catch (Exception exception) {
                mainHandler.post(() -> handleQueueLoadFailure(version, exception));
                return;
            }
            mainHandler.post(() -> finishQueueLoad(version, loadedTracks, startIndex));
        });
    }

    private void loadOnlineQueue(Intent intent) {
        int version = ++queueLoadVersion;
        long[] ids = intent.getLongArrayExtra(EXTRA_TRACK_IDS);
        String[] titles = intent.getStringArrayExtra(EXTRA_ONLINE_TITLES);
        String[] artists = intent.getStringArrayExtra(EXTRA_ONLINE_ARTISTS);
        String[] albums = intent.getStringArrayExtra(EXTRA_ONLINE_ALBUMS);
        String[] urls = intent.getStringArrayExtra(EXTRA_ONLINE_URLS);
        String[] thumbnails = intent.getStringArrayExtra(EXTRA_ONLINE_THUMBNAILS);
        long[] durations = intent.getLongArrayExtra(EXTRA_ONLINE_DURATIONS);
        int count = urls == null ? 0 : urls.length;
        int startIndex = Math.max(0, intent.getIntExtra(EXTRA_START_INDEX, 0));

        failedTrackSkips = 0;
        errorStatus = null;
        mixTitle = safeExtra(intent, EXTRA_MIX_TITLE, "스트림");
        mixSubtitle = safeExtra(intent, EXTRA_MIX_SUBTITLE, "온라인 재생");
        shuffleEnabled = intent.getBooleanExtra(EXTRA_SHUFFLE_ENABLED, false);
        repeatMode = REPEAT_OFF;
        preparing = true;
        playing = false;
        startWhenPrepared = true;
        releasePlayer();
        abandonAudioFocus();
        updateTransportState();
        showNotification();
        broadcastState();

        ArrayList<DeviceAudioTrack> loadedTracks = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String url = safeArrayValue(urls, index);
            if (url.trim().isEmpty()) {
                continue;
            }
            long id = ids != null && index < ids.length ? ids[index] : remoteId(url);
            String title = safeArrayValue(titles, index);
            String artist = safeArrayValue(artists, index);
            String album = safeArrayValue(albums, index);
            String thumbnail = safeArrayValue(thumbnails, index);
            long duration = durations != null && index < durations.length ? Math.max(0L, durations[index]) : 0L;
            loadedTracks.add(new DeviceAudioTrack(
                    id,
                    title.isEmpty() ? "온라인 스트림" : title,
                    artist.isEmpty() ? mixTitle : artist,
                    album.isEmpty() ? mixTitle : album,
                    title.isEmpty() ? "온라인 스트림" : title,
                    "온라인 스트림",
                    url,
                    thumbnail,
                    0L,
                    0,
                    System.currentTimeMillis(),
                    duration,
                    0L,
                    artist
            ));
        }
        finishQueueLoad(version, loadedTracks, startIndex);
    }

    private void finishQueueLoad(int version, List<DeviceAudioTrack> loadedTracks, int startIndex) {
        if (version != queueLoadVersion) {
            return;
        }
        boolean shouldStartWhenPrepared = startWhenPrepared;
        originalQueue.clear();
        originalQueue.addAll(loadedTracks);
        queue.clear();
        queue.addAll(originalQueue);
        queueIndex = queue.isEmpty() ? 0 : Math.min(Math.max(0, startIndex), queue.size() - 1);
        shuffleEnabled = shuffleEnabled && queue.size() > 1;
        if (shuffleEnabled) {
            shuffleQueueFromCurrentTrack();
        }
        failedTrackSkips = 0;
        persistPlaybackSnapshot();
        prepareCurrentTrack(shouldStartWhenPrepared);
    }

    private void handleQueueLoadFailure(int version, Exception exception) {
        if (version != queueLoadVersion) {
            return;
        }
        queue.clear();
        originalQueue.clear();
        queueIndex = 0;
        preparing = false;
        playing = false;
        startWhenPrepared = false;
        clearPlaybackSnapshot();
        errorStatus = "재생 큐를 불러오지 못했습니다: " + safeMessage(exception);
        updateTransportState();
        showNotification();
        broadcastState();
    }

    private void editQueueAsync(Intent intent, boolean playNext) {
        long[] ids = intent.getLongArrayExtra(EXTRA_TRACK_IDS);
        if (ids == null || ids.length == 0) {
            return;
        }
        int version = queueLoadVersion;
        queueExecutor.execute(() -> {
            List<DeviceAudioTrack> loadedTracks;
            try {
                loadedTracks = musicLibrary.loadTracksByIds(this, ids);
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    errorStatus = "재생목록에 추가하지 못했습니다: " + safeMessage(exception);
                    broadcastState();
                });
                return;
            }
            mainHandler.post(() -> finishQueueEdit(version, loadedTracks, playNext));
        });
    }

    private void finishQueueEdit(int version, List<DeviceAudioTrack> loadedTracks, boolean playNext) {
        if (version != queueLoadVersion || loadedTracks == null || loadedTracks.isEmpty()) {
            return;
        }
        errorStatus = null;
        if (queue.isEmpty()) {
            mixTitle = playNext ? "다음 곡" : "사용자 재생목록";
            mixSubtitle = loadedTracks.size() + "곡";
            shuffleEnabled = false;
            repeatMode = REPEAT_OFF;
            queue.clear();
            queue.addAll(loadedTracks);
            originalQueue.clear();
            originalQueue.addAll(loadedTracks);
            queueIndex = 0;
            failedTrackSkips = 0;
            persistPlaybackSnapshot();
            prepareCurrentTrack(true);
            return;
        }
        int insertIndex = playNext ? Math.min(queueIndex + 1, queue.size()) : queue.size();
        int originalInsertIndex = originalQueueInsertIndex(playNext, insertIndex);
        queue.addAll(insertIndex, loadedTracks);
        originalQueue.addAll(originalInsertIndex, loadedTracks);
        shuffleEnabled = shuffleEnabled && queue.size() > 1;
        persistPlaybackSnapshot();
        updateTransportState();
        showNotification();
        broadcastState();
    }

    private void reorderQueue(long[] orderedIds, int[] sourceIndices, int currentSourceIndex) {
        if (orderedIds == null || orderedIds.length != queue.size() || queue.isEmpty()) {
            broadcastState();
            return;
        }
        DeviceAudioTrack current = currentTrack();
        long currentId = current == null ? -1L : current.id();
        ArrayList<DeviceAudioTrack> reordered = new ArrayList<>();
        boolean[] used = new boolean[queue.size()];
        int nextQueueIndex = -1;
        if (sourceIndices != null && sourceIndices.length == queue.size()) {
            for (int outputIndex = 0; outputIndex < sourceIndices.length; outputIndex++) {
                int sourceIndex = sourceIndices[outputIndex];
                if (sourceIndex < 0
                        || sourceIndex >= queue.size()
                        || used[sourceIndex]
                        || queue.get(sourceIndex).id() != orderedIds[outputIndex]) {
                    reordered.clear();
                    Arrays.fill(used, false);
                    nextQueueIndex = -1;
                    break;
                }
                used[sourceIndex] = true;
                if (sourceIndex == currentSourceIndex) {
                    nextQueueIndex = outputIndex;
                }
                reordered.add(queue.get(sourceIndex));
            }
        }
        for (long id : orderedIds) {
            if (reordered.size() == orderedIds.length) {
                break;
            }
            int found = -1;
            for (int index = 0; index < queue.size(); index++) {
                if (!used[index] && queue.get(index).id() == id) {
                    found = index;
                    break;
                }
            }
            if (found < 0) {
                broadcastState();
                return;
            }
            used[found] = true;
            if (found == queueIndex) {
                nextQueueIndex = reordered.size();
            }
            reordered.add(queue.get(found));
        }
        queue.clear();
        queue.addAll(reordered);
        originalQueue.clear();
        originalQueue.addAll(reordered);
        queueIndex = nextQueueIndex >= 0 ? nextQueueIndex : restoredQueueIndex(queueIndex, currentId);
        persistPlaybackSnapshot();
        showNotification();
        broadcastState();
    }

    private void prepareCurrentTrack() {
        prepareCurrentTrack(true);
    }

    private void prepareCurrentTrack(boolean shouldStartWhenPrepared) {
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
        startWhenPrepared = shouldStartWhenPrepared;
        errorStatus = null;
        updateTransportState();
        showNotification();
        broadcastState();
        releasePlayer();

        DeviceAudioTrack track = currentTrack();
        persistPlaybackSnapshot();
        if (isYouTubeWatchSource(track.contentUri())) {
            resolveOnlineAndPrepare(track, version);
            return;
        }
        prepareResolvedSource(track, track.contentUri(), version);
    }

    private void resolveOnlineAndPrepare(DeviceAudioTrack track, int version) {
        onlineResolveExecutor.execute(() -> {
            OnlineStreamResolver.ResolvedStream resolved;
            try {
                resolved = OnlineStreamResolver.resolve(this, track.contentUri());
                if (resolved.streamUrl().trim().isEmpty()) {
                    throw new IllegalStateException("온라인 스트림 URL이 비어 있습니다.");
                }
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    if (version == playbackVersion) {
                        handlePlaybackError("온라인 스트림 준비 실패: " + track.title());
                    }
                });
                return;
            }
            mainHandler.post(() -> {
                if (version == playbackVersion) {
                    prepareResolvedSource(track, resolved.streamUrl(), version);
                }
            });
        });
    }

    private void prepareResolvedSource(DeviceAudioTrack track, String sourceUri, int version) {
        try {
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(audioAttributes());
            if (isHttpUri(sourceUri)) {
                player.setDataSource(sourceUri);
            } else {
                player.setDataSource(this, Uri.parse(sourceUri));
            }
            player.setOnPreparedListener(prepared -> {
                if (version != playbackVersion) {
                    prepared.release();
                    return;
                }
                mediaPlayer = prepared;
                preparing = false;
                failedTrackSkips = 0;
                if (startWhenPrepared && requestAudioFocus()) {
                    prepared.start();
                    playing = true;
                    recordCurrentTrackStarted();
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
            player.setOnCompletionListener(completed -> handleTrackCompletion());
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
        if (preparing) {
            if (startWhenPrepared) {
                pause(false);
            } else {
                play();
            }
            return;
        }
        if (playing) {
            pause(false);
        } else {
            play();
        }
    }

    private void play() {
        if (queue.isEmpty()) {
            if (preparing) {
                startWhenPrepared = true;
                resumeOnAudioFocusGain = false;
                updateTransportState();
                showNotification();
                broadcastState();
            }
            return;
        }
        if (mediaPlayer == null || errorStatus != null) {
            prepareCurrentTrack();
            return;
        }
        if (preparing) {
            startWhenPrepared = true;
            updateTransportState();
            showNotification();
            broadcastState();
            return;
        }
        if (playing) {
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
            recordCurrentTrackStarted();
            errorStatus = null;
            updateTransportState();
            showNotification();
            broadcastState();
        } catch (IllegalStateException exception) {
            handlePlaybackError("재생을 다시 시작할 수 없습니다.");
        }
    }

    private void recordCurrentTrackStarted() {
        if (countedPlaybackVersion == playbackVersion) {
            return;
        }
        DeviceAudioTrack track = currentTrack();
        if (track == null) {
            return;
        }
        countedPlaybackVersion = playbackVersion;
        PlaybackStats.recordTrackStarted(this, track.id());
    }

    private void pause(boolean resumeAfterFocusGain) {
        if (preparing && mediaPlayer == null) {
            playing = false;
            startWhenPrepared = false;
            resumeOnAudioFocusGain = resumeAfterFocusGain;
            updateTransportState();
            showNotification();
            broadcastState();
            return;
        }
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
        moveToNextTrack(false);
    }

    private void seekToQueueIndex(int index) {
        if (queue.isEmpty()) {
            broadcastState();
            return;
        }
        int clamped = Math.max(0, Math.min(index, queue.size() - 1));
        if (queueIndex == clamped && !preparing && errorStatus == null) {
            play();
            return;
        }
        queueIndex = clamped;
        failedTrackSkips = 0;
        prepareCurrentTrack(true);
    }

    private void seekTo(long positionMs) {
        if (mediaPlayer == null) {
            broadcastState();
            return;
        }
        long clamped = Math.max(0L, Math.min(playbackDuration(), positionMs));
        try {
            mediaPlayer.seekTo(clamped, MediaPlayer.SEEK_CLOSEST);
        } catch (IllegalStateException exception) {
            return;
        }
        errorStatus = null;
        updateTransportState();
        showNotification();
        broadcastState();
    }

    private void moveToNextTrack(boolean fromCompletion) {
        if (queue.isEmpty()) {
            return;
        }
        if (queueIndex >= queue.size() - 1) {
            if (repeatMode == REPEAT_ALL) {
                queueIndex = 0;
            } else if (fromCompletion) {
                finishQueuePlayback();
                return;
            } else {
                broadcastState();
                return;
            }
        } else {
            queueIndex++;
        }
        failedTrackSkips = 0;
        prepareCurrentTrack();
    }

    private void playPrevious() {
        if (queue.isEmpty()) {
            return;
        }
        if (queueIndex <= 0) {
            if (repeatMode == REPEAT_ALL) {
                queueIndex = queue.size() - 1;
            } else {
                broadcastState();
                return;
            }
        } else {
            queueIndex--;
        }
        failedTrackSkips = 0;
        prepareCurrentTrack();
    }

    private boolean canMoveToNextTrack() {
        return queue.size() > 1
                && (queueIndex < queue.size() - 1 || repeatMode == REPEAT_ALL);
    }

    private boolean canMoveToPreviousTrack() {
        return queue.size() > 1
                && (queueIndex > 0 || repeatMode == REPEAT_ALL);
    }

    private boolean canShuffleQueue() {
        return queue.size() > 1;
    }

    private void handleTrackCompletion() {
        if (queue.isEmpty()) {
            finishQueuePlayback();
            return;
        }
        if (repeatMode == REPEAT_ONE) {
            prepareCurrentTrack();
            return;
        }
        moveToNextTrack(true);
    }

    private void finishQueuePlayback() {
        preparing = false;
        playing = false;
        startWhenPrepared = false;
        resumeOnAudioFocusGain = false;
        abandonAudioFocus();
        persistPlaybackSnapshot();
        updateTransportState();
        showNotification();
        broadcastState();
    }

    private void stopPlayback() {
        cancelSleepTimer(false);
        queueLoadVersion++;
        playbackVersion++;
        queue.clear();
        originalQueue.clear();
        queueIndex = 0;
        failedTrackSkips = 0;
        preparing = false;
        playing = false;
        startWhenPrepared = false;
        errorStatus = null;
        releasePlayer();
        abandonAudioFocus();
        clearPlaybackSnapshot();
        updateTransportState();
        broadcastState();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void setSleepTimer(int minutes) {
        if (minutes <= 0) {
            cancelSleepTimer(true);
            return;
        }
        long delayMs = minutes * 60_000L;
        sleepTimerEndAtMs = System.currentTimeMillis() + delayMs;
        sleepTimerRemainingMs = delayMs;
        sleepTimerPaused = false;
        mainHandler.removeCallbacks(sleepTimerRunnable);
        mainHandler.postDelayed(sleepTimerRunnable, delayMs);
        broadcastState();
    }

    private void cancelSleepTimer(boolean broadcast) {
        mainHandler.removeCallbacks(sleepTimerRunnable);
        sleepTimerEndAtMs = 0L;
        sleepTimerRemainingMs = 0L;
        sleepTimerPaused = false;
        if (broadcast) {
            broadcastState();
        }
    }

    private void toggleSleepTimerPause() {
        if (sleepTimerPaused) {
            long remainingMs = Math.max(1_000L, sleepTimerRemainingMs);
            sleepTimerEndAtMs = System.currentTimeMillis() + remainingMs;
            sleepTimerRemainingMs = remainingMs;
            sleepTimerPaused = false;
            mainHandler.removeCallbacks(sleepTimerRunnable);
            mainHandler.postDelayed(sleepTimerRunnable, remainingMs);
            broadcastState();
            return;
        }

        long remainingMs = sleepTimerRemainingMs();
        if (remainingMs <= 0L) {
            return;
        }
        sleepTimerRemainingMs = remainingMs;
        sleepTimerEndAtMs = 0L;
        sleepTimerPaused = true;
        mainHandler.removeCallbacks(sleepTimerRunnable);
        broadcastState();
    }

    private long sleepTimerRemainingMs() {
        if (sleepTimerPaused) {
            return Math.max(0L, sleepTimerRemainingMs);
        }
        if (sleepTimerEndAtMs <= 0L) {
            return 0L;
        }
        return Math.max(0L, sleepTimerEndAtMs - System.currentTimeMillis());
    }

    private boolean isSleepTimerRunning() {
        return !sleepTimerPaused && sleepTimerRemainingMs() > 0L;
    }

    private void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled && queue.size() > 1;
        if (shuffleEnabled) {
            ensureOriginalQueue();
            shuffleQueueFromCurrentTrack();
        } else {
            restoreOriginalQueueFromCurrentTrack();
        }
        persistPlaybackSnapshot();
        updateTransportState();
        showNotification();
        broadcastState();
    }

    private void shuffleQueueFromCurrentTrack() {
        DeviceAudioTrack track = currentTrack();
        if (track == null || queue.size() < 2) {
            return;
        }
        List<DeviceAudioTrack> remaining = new ArrayList<>(queue);
        remaining.remove(track);
        Collections.shuffle(remaining);
        queue.clear();
        queue.add(track);
        queue.addAll(remaining);
        queueIndex = 0;
    }

    private void restoreOriginalQueueFromCurrentTrack() {
        if (queue.isEmpty()) {
            originalQueue.clear();
            return;
        }
        if (originalQueue.size() != queue.size()) {
            originalQueue.clear();
            originalQueue.addAll(queue);
            return;
        }
        DeviceAudioTrack current = currentTrack();
        long currentId = current == null ? -1L : current.id();
        ArrayList<DeviceAudioTrack> restored = new ArrayList<>(originalQueue);
        int restoredIndex = indexOfTrack(restored, current);
        queue.clear();
        queue.addAll(restored);
        queueIndex = restoredIndex >= 0 ? restoredIndex : restoredQueueIndex(queueIndex, currentId);
    }

    private void ensureOriginalQueue() {
        if (originalQueue.size() == queue.size() && !originalQueue.isEmpty()) {
            return;
        }
        originalQueue.clear();
        originalQueue.addAll(queue);
    }

    private int originalQueueInsertIndex(boolean playNext, int queueInsertIndex) {
        ensureOriginalQueue();
        if (originalQueue.isEmpty()) {
            return 0;
        }
        if (!shuffleEnabled) {
            return Math.max(0, Math.min(queueInsertIndex, originalQueue.size()));
        }
        if (!playNext) {
            return originalQueue.size();
        }
        int currentIndex = indexOfTrack(originalQueue, currentTrack());
        if (currentIndex < 0) {
            return originalQueue.size();
        }
        return Math.min(currentIndex + 1, originalQueue.size());
    }

    private int indexOfTrack(List<DeviceAudioTrack> tracks, DeviceAudioTrack target) {
        if (tracks == null || target == null) {
            return -1;
        }
        for (int index = 0; index < tracks.size(); index++) {
            if (tracks.get(index) == target) {
                return index;
            }
        }
        String targetUri = target.contentUri();
        if (targetUri != null && !targetUri.trim().isEmpty()) {
            for (int index = 0; index < tracks.size(); index++) {
                String uri = tracks.get(index).contentUri();
                if (targetUri.equals(uri)) {
                    return index;
                }
            }
        }
        for (int index = 0; index < tracks.size(); index++) {
            if (tracks.get(index).id() == target.id()) {
                return index;
            }
        }
        return -1;
    }

    private void toggleRepeat() {
        if (repeatMode == REPEAT_OFF) {
            repeatMode = REPEAT_ALL;
        } else if (repeatMode == REPEAT_ALL) {
            repeatMode = REPEAT_ONE;
        } else {
            repeatMode = REPEAT_OFF;
        }
        persistPlaybackSnapshot();
        updateTransportState();
        showNotification();
        broadcastState();
    }

    private void handlePlaybackError(String message) {
        if (queue.size() > 1 && failedTrackSkips < queue.size() - 1) {
            preparing = false;
            playing = false;
            startWhenPrepared = false;
            errorStatus = message + " 다음 곡으로 넘어갑니다.";
            updateTransportState();
            showNotification();
            broadcastState();
            failedTrackSkips++;
            queueIndex = (queueIndex + 1) % queue.size();
            prepareCurrentTrack();
            return;
        }
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
            pause(playing || (preparing && startWhenPrepared));
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

    private void pauseForOutputDisconnect() {
        if (!playing && !(preparing && startWhenPrepared)) {
            return;
        }
        pause(false);
        abandonAudioFocus();
    }

    private void registerNoisyAudioReceiver() {
        if (noisyAudioReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(noisyAudioReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(noisyAudioReceiver, filter);
        }
        noisyAudioReceiverRegistered = true;
    }

    private void unregisterNoisyAudioReceiver() {
        if (!noisyAudioReceiverRegistered) {
            return;
        }
        try {
            unregisterReceiver(noisyAudioReceiver);
        } catch (IllegalArgumentException ignored) {
            // The service can be torn down during rapid playback transitions; treat it as unregistered.
        }
        noisyAudioReceiverRegistered = false;
    }

    private boolean restoreQueueAsync(int startId) {
        SharedPreferences prefs = playbackPreferences();
        long[] ids = persistedQueueIds(prefs);
        List<DeviceAudioTrack> snapshotTracks = persistedQueueSnapshot(prefs, PREF_PLAYBACK_QUEUE_SNAPSHOT);
        boolean restoreFromSnapshot = hasRemoteQueueItems(snapshotTracks);
        if (ids.length == 0 && !restoreFromSnapshot) {
            return false;
        }
        long[] originalIds = persistedQueueIds(prefs, PREF_PLAYBACK_ORIGINAL_QUEUE_IDS);
        List<DeviceAudioTrack> originalSnapshotTracks = persistedQueueSnapshot(
                prefs,
                PREF_PLAYBACK_ORIGINAL_QUEUE_SNAPSHOT
        );

        int version = ++queueLoadVersion;
        int restoredIndex = Math.max(0, prefs.getInt(PREF_PLAYBACK_QUEUE_INDEX, 0));
        long restoredTrackId = prefs.getLong(PREF_PLAYBACK_TRACK_ID, -1L);
        String restoredMixTitle = valueOrDefault(prefs.getString(PREF_PLAYBACK_MIX_TITLE, null), "로컬 음악");
        String restoredMixSubtitle = valueOrDefault(prefs.getString(PREF_PLAYBACK_MIX_SUBTITLE, null), "기기 저장 음악");
        boolean restoredShuffle = prefs.getBoolean(PREF_PLAYBACK_SHUFFLE, false);
        int restoredRepeatMode = prefs.getInt(PREF_PLAYBACK_REPEAT, REPEAT_OFF);

        failedTrackSkips = 0;
        errorStatus = null;
        preparing = false;
        playing = false;
        startWhenPrepared = false;
        resumeOnAudioFocusGain = false;
        releasePlayer();
        abandonAudioFocus();

        queueExecutor.execute(() -> {
            List<DeviceAudioTrack> loadedTracks;
            List<DeviceAudioTrack> originalTracks;
            try {
                loadedTracks = restoreFromSnapshot
                        ? new ArrayList<>(snapshotTracks)
                        : musicLibrary.loadTracksByIds(this, ids);
                if (restoreFromSnapshot && !originalSnapshotTracks.isEmpty()) {
                    originalTracks = new ArrayList<>(originalSnapshotTracks);
                } else {
                    originalTracks = originalIds.length == 0
                            ? new ArrayList<>(loadedTracks)
                            : musicLibrary.loadTracksByIds(this, originalIds);
                }
            } catch (Exception exception) {
                mainHandler.post(() -> finishRestoredQueueLoad(
                        version,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        restoredIndex,
                        restoredTrackId,
                        restoredMixTitle,
                        restoredMixSubtitle,
                        restoredShuffle,
                        restoredRepeatMode,
                        startId
                ));
                return;
            }
            mainHandler.post(() -> finishRestoredQueueLoad(
                    version,
                    loadedTracks,
                    originalTracks,
                    restoredIndex,
                    restoredTrackId,
                    restoredMixTitle,
                    restoredMixSubtitle,
                    restoredShuffle,
                    restoredRepeatMode,
                    startId
            ));
        });
        return true;
    }

    private void finishRestoredQueueLoad(
            int version,
            List<DeviceAudioTrack> loadedTracks,
            List<DeviceAudioTrack> restoredOriginalTracks,
            int restoredIndex,
            long restoredTrackId,
            String restoredMixTitle,
            String restoredMixSubtitle,
            boolean restoredShuffle,
            int restoredRepeatMode,
            int startId
    ) {
        if (version != queueLoadVersion) {
            return;
        }
        queue.clear();
        if (loadedTracks != null) {
            queue.addAll(loadedTracks);
        }
        originalQueue.clear();
        if (restoredOriginalTracks != null
                && restoredOriginalTracks.size() == queue.size()) {
            originalQueue.addAll(restoredOriginalTracks);
        } else {
            originalQueue.addAll(queue);
        }
        if (queue.isEmpty()) {
            queueIndex = 0;
            originalQueue.clear();
            clearPlaybackSnapshot();
            broadcastState();
            stopSelf(startId);
            return;
        }

        mixTitle = restoredMixTitle;
        mixSubtitle = restoredMixSubtitle;
        queueIndex = restoredQueueIndex(restoredIndex, restoredTrackId);
        shuffleEnabled = restoredShuffle && queue.size() > 1;
        repeatMode = validRepeatMode(restoredRepeatMode);
        preparing = false;
        playing = false;
        startWhenPrepared = false;
        resumeOnAudioFocusGain = false;
        errorStatus = null;
        failedTrackSkips = 0;
        persistPlaybackSnapshot();
        updateTransportState();
        broadcastState();
    }

    private int restoredQueueIndex(int fallbackIndex, long trackId) {
        if (trackId >= 0L) {
            for (int index = 0; index < queue.size(); index++) {
                if (queue.get(index).id() == trackId) {
                    return index;
                }
            }
        }
        return Math.min(Math.max(0, fallbackIndex), queue.size() - 1);
    }

    private int validRepeatMode(int mode) {
        return mode == REPEAT_ALL || mode == REPEAT_ONE ? mode : REPEAT_OFF;
    }

    private SharedPreferences playbackPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void persistPlaybackSnapshot() {
        if (queue.isEmpty()) {
            clearPlaybackSnapshot();
            return;
        }
        DeviceAudioTrack track = currentTrack();
        playbackPreferences().edit()
                .putString(PREF_PLAYBACK_QUEUE_IDS, serializedQueueIds())
                .putString(PREF_PLAYBACK_ORIGINAL_QUEUE_IDS, serializedOriginalQueueIds())
                .putString(PREF_PLAYBACK_QUEUE_SNAPSHOT, serializedTrackSnapshot(queue))
                .putString(PREF_PLAYBACK_ORIGINAL_QUEUE_SNAPSHOT, serializedTrackSnapshot(originalQueue))
                .putInt(PREF_PLAYBACK_QUEUE_INDEX, queueIndex)
                .putLong(PREF_PLAYBACK_TRACK_ID, track == null ? -1L : track.id())
                .putString(PREF_PLAYBACK_MIX_TITLE, mixTitle)
                .putString(PREF_PLAYBACK_MIX_SUBTITLE, mixSubtitle)
                .putBoolean(PREF_PLAYBACK_SHUFFLE, shuffleEnabled)
                .putInt(PREF_PLAYBACK_REPEAT, repeatMode)
                .apply();
    }

    private void clearPlaybackSnapshot() {
        playbackPreferences().edit()
                .remove(PREF_PLAYBACK_QUEUE_IDS)
                .remove(PREF_PLAYBACK_ORIGINAL_QUEUE_IDS)
                .remove(PREF_PLAYBACK_QUEUE_SNAPSHOT)
                .remove(PREF_PLAYBACK_ORIGINAL_QUEUE_SNAPSHOT)
                .remove(PREF_PLAYBACK_QUEUE_INDEX)
                .remove(PREF_PLAYBACK_TRACK_ID)
                .remove(PREF_PLAYBACK_MIX_TITLE)
                .remove(PREF_PLAYBACK_MIX_SUBTITLE)
                .remove(PREF_PLAYBACK_SHUFFLE)
                .remove(PREF_PLAYBACK_REPEAT)
                .apply();
    }

    private String serializedQueueIds() {
        return serializedTrackIds(queue);
    }

    private String serializedOriginalQueueIds() {
        ensureOriginalQueue();
        return serializedTrackIds(originalQueue);
    }

    private String serializedTrackIds(List<DeviceAudioTrack> tracks) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < tracks.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(tracks.get(index).id());
        }
        return builder.toString();
    }

    private String serializedTrackSnapshot(List<DeviceAudioTrack> tracks) {
        JSONArray items = new JSONArray();
        for (DeviceAudioTrack track : tracks == null ? new ArrayList<DeviceAudioTrack>() : tracks) {
            if (track == null) {
                continue;
            }
            JSONObject item = new JSONObject();
            try {
                item.put("id", track.id());
                item.put("title", track.title());
                item.put("artist", track.artist());
                item.put("representative_artist", track.representativeArtist());
                item.put("album", track.album());
                item.put("display_name", track.displayName());
                item.put("folder", track.folder());
                item.put("content_uri", track.contentUri());
                item.put("album_art_uri", track.albumArtUri());
                item.put("album_id", track.albumId());
                item.put("track_number", track.trackNumber());
                item.put("date_added_ms", track.dateAddedMs());
                item.put("duration_ms", track.durationMs());
                item.put("size_bytes", track.sizeBytes());
                items.put(item);
            } catch (Exception ignored) {
            }
        }
        return items.toString();
    }

    private List<DeviceAudioTrack> persistedQueueSnapshot(SharedPreferences prefs, String key) {
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        String serialized = prefs.getString(key, "");
        if (serialized == null || serialized.trim().isEmpty()) {
            return tracks;
        }
        try {
            JSONArray items = new JSONArray(serialized);
            for (int index = 0; index < items.length(); index++) {
                JSONObject item = items.optJSONObject(index);
                if (item == null) {
                    continue;
                }
                String contentUri = item.optString("content_uri", "");
                if (contentUri.trim().isEmpty()) {
                    continue;
                }
                tracks.add(new DeviceAudioTrack(
                        item.optLong("id", remoteId(contentUri)),
                        item.optString("title", ""),
                        item.optString("artist", ""),
                        item.optString("album", ""),
                        item.optString("display_name", ""),
                        item.optString("folder", ""),
                        contentUri,
                        item.optString("album_art_uri", ""),
                        Math.max(0L, item.optLong("album_id", 0L)),
                        Math.max(0, item.optInt("track_number", 0)),
                        Math.max(0L, item.optLong("date_added_ms", 0L)),
                        Math.max(0L, item.optLong("duration_ms", 0L)),
                        Math.max(0L, item.optLong("size_bytes", 0L)),
                        item.optString("representative_artist", "")
                ));
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
        return tracks;
    }

    private boolean hasRemoteQueueItems(List<DeviceAudioTrack> tracks) {
        for (DeviceAudioTrack track : tracks == null ? new ArrayList<DeviceAudioTrack>() : tracks) {
            if (track != null && (track.id() < 0L || isHttpUri(track.contentUri()))) {
                return true;
            }
        }
        return false;
    }

    private long[] persistedQueueIds(SharedPreferences prefs) {
        return persistedQueueIds(prefs, PREF_PLAYBACK_QUEUE_IDS);
    }

    private long[] persistedQueueIds(SharedPreferences prefs, String key) {
        String serialized = prefs.getString(key, "");
        if (serialized == null || serialized.trim().isEmpty()) {
            return new long[0];
        }
        String[] parts = serialized.split(",");
        long[] ids = new long[parts.length];
        int count = 0;
        for (String part : parts) {
            try {
                long id = Long.parseLong(part.trim());
                if (id >= 0L) {
                    ids[count++] = id;
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed entries and keep the rest of the saved queue usable.
            }
        }
        return count == ids.length ? ids : Arrays.copyOf(ids, count);
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
        Bitmap artwork = artworkFor(track);
        builder.setSmallIcon(R.drawable.ic_stat_playback)
                .setContentTitle(track == null ? "RabbYT 로컬 플레이어" : track.title())
                .setContentText(track == null ? "기기 저장 음악" : track.artist() + " · " + mixTitle)
                .setContentIntent(contentIntent())
                .setDeleteIntent(serviceAction(ACTION_STOP, 6))
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setOngoing(playing || (preparing && startWhenPrepared));
        if (artwork != null) {
            builder.setLargeIcon(artwork);
        }

        boolean waitingToPlay = preparing && startWhenPrepared;
        boolean shuffleAvailable = canShuffleQueue();
        boolean previousAvailable = canMoveToPreviousTrack();
        boolean nextAvailable = canMoveToNextTrack();
        builder.addAction(
                shuffleIcon(),
                shuffleNotificationLabel(),
                controlAction(ACTION_TOGGLE_SHUFFLE, shuffleAvailable, 1)
        );
        builder.addAction(
                previousIcon(),
                previousAvailable ? "이전" : "이전 없음",
                controlAction(ACTION_PREVIOUS, previousAvailable, 2)
        );
        builder.addAction(
                playing || waitingToPlay ? R.drawable.ic_pause : R.drawable.ic_play_arrow,
                playing || waitingToPlay ? "일시정지" : "재생",
                serviceAction(ACTION_TOGGLE, 3)
        );
        builder.addAction(
                nextIcon(),
                nextAvailable ? "다음" : "다음 없음",
                controlAction(ACTION_NEXT, nextAvailable, 4)
        );
        builder.addAction(
                repeatIcon(),
                repeatNotificationLabel(),
                serviceAction(ACTION_TOGGLE_REPEAT, 5)
        );

        if (mediaSession != null) {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(1, 2, 3));
        }
        return builder.build();
    }

    private String repeatNotificationLabel() {
        if (repeatMode == REPEAT_ONE) {
            return "한 곡 반복";
        }
        if (repeatMode == REPEAT_ALL) {
            return "전체 반복";
        }
        return "반복 꺼짐";
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

    private PendingIntent controlAction(String action, boolean enabled, int requestCode) {
        return serviceAction(enabled ? action : ACTION_REQUEST_STATE, requestCode);
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
            Bitmap artwork = artworkFor(track);
            MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, track.title())
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album())
                    .putString(MediaMetadata.METADATA_KEY_ART_URI, track.albumArtUri())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, track.albumArtUri())
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, track.durationMs());
            if (artwork != null) {
                metadata.putBitmap(MediaMetadata.METADATA_KEY_ART, artwork);
                metadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork);
            }
            mediaSession.setMetadata(metadata.build());
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
                | PlaybackState.ACTION_STOP;
        if (track != null) {
            actions |= PlaybackState.ACTION_SEEK_TO;
            boolean previousAvailable = canMoveToPreviousTrack();
            boolean nextAvailable = canMoveToNextTrack();
            if (previousAvailable) {
                actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
            }
            if (nextAvailable) {
                actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
            }
        }
        PlaybackState.Builder builder = new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, currentPosition(), playing ? 1f : 0f);
        if (track != null) {
            boolean previousAvailable = canMoveToPreviousTrack();
            boolean nextAvailable = canMoveToNextTrack();
            if (!previousAvailable) {
                builder.addCustomAction(disabledPreviousAction());
            }
            if (!nextAvailable) {
                builder.addCustomAction(disabledNextAction());
            }
            builder.addCustomAction(new PlaybackState.CustomAction.Builder(
                    ACTION_TOGGLE_SHUFFLE,
                    shuffleNotificationLabel(),
                    shuffleIcon()
            ).build());
            builder.addCustomAction(new PlaybackState.CustomAction.Builder(
                    ACTION_TOGGLE_REPEAT,
                    repeatNotificationLabel(),
                    repeatIcon()
            ).build());
        }
        if (errorStatus != null) {
            builder.setErrorMessage(errorStatus);
        }
        mediaSession.setPlaybackState(builder.build());
    }

    private PlaybackState.CustomAction disabledPreviousAction() {
        return new PlaybackState.CustomAction.Builder(
                ACTION_PREVIOUS_UNAVAILABLE,
                "이전 없음",
                R.drawable.ic_skip_previous_disabled
        ).build();
    }

    private PlaybackState.CustomAction disabledNextAction() {
        return new PlaybackState.CustomAction.Builder(
                ACTION_NEXT_UNAVAILABLE,
                "다음 없음",
                R.drawable.ic_skip_next_disabled
        ).build();
    }

    private int shuffleIcon() {
        if (!canShuffleQueue()) {
            return R.drawable.ic_shuffle_disabled;
        }
        return shuffleEnabled ? R.drawable.ic_shuffle : R.drawable.ic_shuffle_disabled;
    }

    private String shuffleNotificationLabel() {
        if (!canShuffleQueue()) {
            return "셔플 사용할 수 없음";
        }
        return shuffleEnabled ? "셔플 켜짐" : "셔플 꺼짐";
    }

    private int previousIcon() {
        return canMoveToPreviousTrack() ? R.drawable.ic_skip_previous : R.drawable.ic_skip_previous_disabled;
    }

    private int nextIcon() {
        return canMoveToNextTrack() ? R.drawable.ic_skip_next : R.drawable.ic_skip_next_disabled;
    }

    private int repeatIcon() {
        if (repeatMode == REPEAT_ONE) {
            return R.drawable.ic_repeat_one;
        }
        if (repeatMode == REPEAT_ALL) {
            return R.drawable.ic_repeat;
        }
        return R.drawable.ic_repeat_disabled;
    }

    private Bitmap artworkFor(DeviceAudioTrack track) {
        String artworkUri = track == null ? "" : track.albumArtUri();
        if (artworkUri.trim().isEmpty()) {
            return null;
        }
        if (isHttpUri(artworkUri)) {
            if (artworkUri.equals(cachedArtworkUri)) {
                return cachedArtwork;
            }
            requestRemoteArtwork(artworkUri);
            return null;
        }
        if (artworkUri.equals(cachedArtworkUri)) {
            return cachedArtwork;
        }
        cachedArtworkUri = artworkUri;
        cachedArtwork = loadArtworkBitmap(artworkUri);
        return cachedArtwork;
    }

    private Bitmap loadArtworkBitmap(String artworkUri) {
        try (InputStream stream = getContentResolver().openInputStream(Uri.parse(artworkUri))) {
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            if (bitmap == null) {
                return null;
            }
            int maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (maxDimension <= 768) {
                return bitmap;
            }
            float ratio = 768f / maxDimension;
            int width = Math.max(1, Math.round(bitmap.getWidth() * ratio));
            int height = Math.max(1, Math.round(bitmap.getHeight() * ratio));
            return Bitmap.createScaledBitmap(bitmap, width, height, true);
        } catch (Exception exception) {
            return null;
        }
    }

    private void requestRemoteArtwork(String artworkUri) {
        if (artworkUri == null || artworkUri.trim().isEmpty() || artworkUri.equals(loadingArtworkUri)) {
            return;
        }
        cachedArtworkUri = artworkUri;
        cachedArtwork = null;
        loadingArtworkUri = artworkUri;
        artworkExecutor.execute(() -> {
            Bitmap bitmap = loadRemoteArtworkBitmap(artworkUri);
            mainHandler.post(() -> {
                if (!artworkUri.equals(cachedArtworkUri)) {
                    return;
                }
                cachedArtwork = bitmap;
                loadingArtworkUri = "";
                updateTransportState();
                showNotification();
            });
        });
    }

    private Bitmap loadRemoteArtworkBitmap(String artworkUri) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(artworkUri).openConnection();
            connection.setConnectTimeout(4500);
            connection.setReadTimeout(6500);
            connection.setInstanceFollowRedirects(true);
            try (InputStream stream = connection.getInputStream()) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                if (bitmap == null) {
                    return null;
                }
                int maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
                if (maxDimension <= 768) {
                    return bitmap;
                }
                float ratio = 768f / maxDimension;
                int width = Math.max(1, Math.round(bitmap.getWidth() * ratio));
                int height = Math.max(1, Math.round(bitmap.getHeight() * ratio));
                return Bitmap.createScaledBitmap(bitmap, width, height, true);
            }
        } catch (Exception exception) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

    private long playbackDuration() {
        if (mediaPlayer != null) {
            try {
                return Math.max(0L, mediaPlayer.getDuration());
            } catch (IllegalStateException ignored) {
                // Fall back to metadata below.
            }
        }
        DeviceAudioTrack track = currentTrack();
        return track == null ? 0L : Math.max(0L, track.durationMs());
    }

    private void broadcastState() {
        Intent intent = new Intent(ACTION_STATE);
        intent.setPackage(getPackageName());
        DeviceAudioTrack track = currentTrack();
        intent.putExtra(EXTRA_HAS_QUEUE, !queue.isEmpty());
        intent.putExtra(EXTRA_PLAYING, playing);
        intent.putExtra(EXTRA_PREPARING, preparing);
        intent.putExtra(EXTRA_WILL_PLAY, preparing && startWhenPrepared);
        intent.putExtra(EXTRA_ERROR, errorStatus != null);
        intent.putExtra(EXTRA_TITLE, track == null ? "로컬 재생 대기" : track.title());
        intent.putExtra(EXTRA_META, track == null ? "기기 음악을 스캔하면 재생할 수 있습니다." : track.artist() + " · " + mixTitle);
        intent.putExtra(EXTRA_STATUS, statusText(track));
        intent.putExtra(EXTRA_TRACK_ID, track == null ? -1L : track.id());
        intent.putExtra(EXTRA_ARTIST, track == null ? "" : track.artist());
        intent.putExtra(EXTRA_ALBUM, track == null ? "" : track.album());
        intent.putExtra(EXTRA_FOLDER, track == null ? "" : track.folder());
        intent.putExtra(EXTRA_ALBUM_ART_URI, track == null ? "" : track.albumArtUri());
        intent.putExtra(EXTRA_DURATION_MS, track == null ? 0L : track.durationMs());
        intent.putExtra(EXTRA_POSITION_MS, currentPosition());
        intent.putExtra(EXTRA_QUEUE_INDEX, queue.isEmpty() ? -1 : queueIndex);
        intent.putExtra(EXTRA_QUEUE_SIZE, queue.size());
        intent.putExtra(EXTRA_MIX, mixTitle);
        intent.putExtra(EXTRA_SHUFFLE_ENABLED, shuffleEnabled);
        intent.putExtra(EXTRA_REPEAT_MODE, repeatMode);
        intent.putExtra(EXTRA_QUEUE_TRACK_IDS, queueTrackIds());
        intent.putExtra(EXTRA_QUEUE_TITLES, queueTitles());
        intent.putExtra(EXTRA_QUEUE_ARTISTS, queueArtists());
        intent.putExtra(EXTRA_QUEUE_ALBUMS, queueAlbums());
        intent.putExtra(EXTRA_QUEUE_URLS, queueUrls());
        intent.putExtra(EXTRA_QUEUE_THUMBNAILS, queueThumbnails());
        intent.putExtra(EXTRA_QUEUE_DURATIONS, queueDurations());
        intent.putExtra(EXTRA_SLEEP_TIMER_END_AT_MS, sleepTimerEndAtMs);
        intent.putExtra(EXTRA_SLEEP_TIMER_REMAINING_MS, sleepTimerRemainingMs());
        intent.putExtra(EXTRA_SLEEP_TIMER_PAUSED, sleepTimerPaused);
        sendBroadcast(intent);
        scheduleStateTick();
    }

    private long[] queueTrackIds() {
        long[] ids = new long[queue.size()];
        for (int index = 0; index < queue.size(); index++) {
            ids[index] = queue.get(index).id();
        }
        return ids;
    }

    private String[] queueTitles() {
        String[] values = new String[queue.size()];
        for (int index = 0; index < queue.size(); index++) {
            values[index] = queue.get(index).title();
        }
        return values;
    }

    private String[] queueArtists() {
        String[] values = new String[queue.size()];
        for (int index = 0; index < queue.size(); index++) {
            values[index] = queue.get(index).artist();
        }
        return values;
    }

    private String[] queueAlbums() {
        String[] values = new String[queue.size()];
        for (int index = 0; index < queue.size(); index++) {
            values[index] = queue.get(index).album();
        }
        return values;
    }

    private String[] queueUrls() {
        String[] values = new String[queue.size()];
        for (int index = 0; index < queue.size(); index++) {
            values[index] = queue.get(index).contentUri();
        }
        return values;
    }

    private String[] queueThumbnails() {
        String[] values = new String[queue.size()];
        for (int index = 0; index < queue.size(); index++) {
            values[index] = queue.get(index).albumArtUri();
        }
        return values;
    }

    private long[] queueDurations() {
        long[] values = new long[queue.size()];
        for (int index = 0; index < queue.size(); index++) {
            values[index] = queue.get(index).durationMs();
        }
        return values;
    }

    private void scheduleStateTick() {
        mainHandler.removeCallbacks(stateTick);
        if (playing || preparing || isSleepTimerRunning()) {
            mainHandler.postDelayed(stateTick, 1000);
        }
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

    private boolean isYouTubeWatchSource(String sourceUri) {
        String value = sourceUri == null ? "" : sourceUri.trim().toLowerCase();
        return value.startsWith("http")
                && (value.contains("youtube.com/")
                || value.contains("youtu.be/")
                || value.contains("music.youtube.com/")
                || value.contains("m.youtube.com/"));
    }

    private boolean isHttpUri(String sourceUri) {
        String value = sourceUri == null ? "" : sourceUri.trim().toLowerCase();
        return value.startsWith("http://") || value.startsWith("https://");
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

    private static String safeArrayValue(String[] values, int index) {
        if (values == null || index < 0 || index >= values.length || values[index] == null) {
            return "";
        }
        return values[index].trim();
    }

    private static long remoteId(String value) {
        String key = value == null ? "" : value.trim();
        long hash = 1125899906842597L;
        for (int index = 0; index < key.length(); index++) {
            hash = 31L * hash + key.charAt(index);
        }
        return hash == Long.MIN_VALUE ? -1L : -Math.abs(hash);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String safeMessage(Exception exception) {
        String message = exception == null ? null : exception.getMessage();
        return message == null || message.trim().isEmpty() ? "알 수 없는 오류" : message.trim();
    }

}
