package com.ytet.android.ui;

import android.animation.ObjectAnimator;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ytet.android.R;
import com.ytet.android.core.AudioFormat;
import com.ytet.android.core.DefaultMediaPaths;
import com.ytet.android.core.ExtractionRequest;
import com.ytet.android.core.MediaType;
import com.ytet.android.core.VideoQuality;
import com.ytet.android.extract.ExtractionService;
import com.ytet.android.extract.StorageWriter;
import com.ytet.android.library.DeviceAudioTrack;
import com.ytet.android.library.DeviceMusicLibrary;
import com.ytet.android.library.MusicLibrary;
import com.ytet.android.library.TrackMetadataOverrides;
import com.ytet.android.library.UserPlaylists;
import com.ytet.android.playback.PlaybackService;
import com.ytet.android.playback.PlaybackStats;
import com.ytet.android.stream.MusicStation;
import com.ytet.android.stream.OnlineStreamCatalog;
import com.ytet.android.stream.OnlineStreamClient;
import com.ytet.android.stream.OnlineStreamResolver;
import com.ytet.android.stream.OnlineStreamSection;
import com.ytet.android.stream.OnlineStreamVideo;
import com.ytet.android.stream.StationCatalog;
import com.ytet.android.update.UpdateChecker;
import com.ytet.android.update.UpdateApkProvider;
import com.ytet.android.update.UpdateDownloadService;
import com.ytet.android.update.UpdateInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ytet.android.BuildConfig;

public final class MainActivity extends Activity {
    private static final int REQUEST_OUTPUT_TREE = 1207;
    private static final int REQUEST_NOTIFICATIONS = 1208;
    private static final int REQUEST_DELETE_AUDIO = 1209;
    private static final int REQUEST_AUDIO_LIBRARY = 1210;
    private static final int REQUEST_WRITE_LIBRARY = 1211;
    private static final String UPDATE_APK_MIME = "application/vnd.android.package-archive";
    private static final String PREFS = "ytet_android";
    private static final String PREF_OUTPUT_TREE = "output_tree";
    private static final String PREF_UPDATE_DOWNLOAD_ID = "update_download_id";
    private static final String PREF_UPDATE_APK_PATH = "update_apk_path";
    private static final String PREF_UPDATE_TAG = "update_tag";
    private static final String PREF_LIBRARY_SOURCE = "library_source";
    private static final String PREF_LIBRARY_SORT = "library_sort";
    private static final String PLAYLIST_GROUP_PREFIX = "playlist:";
    private static final String LIBRARY_SOURCE_COLLECTION = "collection";
    private static final String LIBRARY_SOURCE_DEVICE = "device";
    private static final long LIBRARY_SEARCH_DEBOUNCE_MS = 120L;
    private static final long LIBRARY_PULL_MIN_REFRESH_VISIBLE_MS = 680L;
    private static final int MAX_SLEEP_TIMER_HOURS = 23;
    private static final int MAX_SLEEP_TIMER_MINUTES = MAX_SLEEP_TIMER_HOURS * 60 + 59;
    private static final int BOTTOM_CHROME_BASE = 0xFF0B0B0D;
    private static final int NOW_PLAYING_TRANSITION_NONE = 0;
    private static final int NOW_PLAYING_TRANSITION_NEXT = 1;
    private static final int NOW_PLAYING_TRANSITION_PREVIOUS = -1;
    private static final float NOW_PLAYING_SWIPE_RESISTANCE = 1.32f;
    private static final float NOW_PLAYING_BLOCKED_SWIPE_RESISTANCE = 0.18f;
    private static final float NOW_PLAYING_SWIPE_COMMIT_PROGRESS = 0.46f;
    private static final float NOW_PLAYING_CURRENT_FADE_START = 0.18f;
    private static final float NOW_PLAYING_CURRENT_FADE_RANGE = 0.42f;
    private static final float NOW_PLAYING_PREVIEW_FADE_START = 0.16f;
    private static final float NOW_PLAYING_PREVIEW_FADE_RANGE = 0.52f;
    private static final String[] SUPPORTED_VIDEO_URL_MARKERS = {
            "youtube.com/",
            "youtu.be/",
            "music.youtube.com/",
            "m.youtube.com/"
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService libraryExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService streamExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService streamResolveExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private final Set<String> streamResolvePrefetchUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final DeviceMusicLibrary deviceMusicLibrary = new DeviceMusicLibrary();
    private final UpdateChecker updateChecker = new UpdateChecker(BuildConfig.UPDATE_CHANNEL);

    private FrameLayout contentFrame;
    private ScrollView contentScrollView;
    private RecyclerView libraryRecyclerView;
    private LibraryRecyclerAdapter libraryRecyclerAdapter;
    private Parcelable libraryRecyclerState;
    private boolean libraryRecyclerGridMode;
    private int mainNavigationInset;
    private FrameLayout nowPlayingBar;
    private FrameLayout nowPlayingInfoFrame;
    private FrameLayout nowPlayingCurrentFrame;
    private FrameLayout nowPlayingPreviewFrame;
    private FrameLayout nowPlayingCover;
    private FrameLayout nowPlayingPreviewCover;
    private TextView nowPlayingTitle;
    private TextView nowPlayingMeta;
    private TextView nowPlayingPreviewTitle;
    private TextView nowPlayingPreviewMeta;
    private MiniPlaybackProgressView nowPlayingProgress;
    private ImageButton playPauseButton;
    private View bottomVignette;
    private LinearLayout bottomChrome;
    private View bottomNavigationGuard;
    private long renderedNowPlayingTrackId = Long.MIN_VALUE;
    private boolean renderedNowPlayingIdle = true;
    private String renderedNowPlayingTitle = "";
    private String renderedNowPlayingMeta = "";
    private long renderedNowPlayingPreviewTrackId = Long.MIN_VALUE;
    private int renderedNowPlayingPreviewDirection = NOW_PLAYING_TRANSITION_NONE;
    private boolean nowPlayingContentInitialized;
    private int pendingNowPlayingTransitionDirection = NOW_PLAYING_TRANSITION_NONE;
    private float nowPlayingSwipeStartX;
    private float nowPlayingSwipeStartY;
    private boolean nowPlayingSwipeTracking;
    private boolean nowPlayingSwipeConsumed;
    private TabItem homeTabButton;
    private TabItem libraryTabButton;
    private TabItem extractorTabButton;
    private TextView updateStatusText;
    private Button updateActionButton;
    private TextView updateDownloadStatusText;
    private ProgressBar updateDownloadProgressBar;
    private Dialog playerDialog;
    private Dialog queueDialog;
    private QueueSheetLayout queueSheetLayout;
    private QueueRecyclerAdapter queueRecyclerAdapter;
    private RecyclerView queueRecyclerView;
    private ItemTouchHelper queueItemTouchHelper;
    private FrameLayout queuePlayerCover;
    private TextView queuePlayerTitle;
    private TextView queuePlayerMeta;
    private ImageButton queuePlayerShuffleButton;
    private ImageButton queuePlayerPreviousButton;
    private ImageButton queuePlayerPlayButton;
    private ImageButton queuePlayerNextButton;
    private ImageButton queuePlayerRepeatButton;
    private long queuePlayerRenderedTrackId = Long.MIN_VALUE;
    private String queuePlayerRenderedAlbumArtUri = "";
    private int queueDialogRenderedIndex = Integer.MIN_VALUE;
    private int queueDialogRenderedQueueSize = Integer.MIN_VALUE;
    private long queueDialogRenderedTrackId = Long.MIN_VALUE;
    private long queueDialogRenderedFingerprint = Long.MIN_VALUE;
    private float playerQueueDragStartY;
    private boolean playerQueueDragTriggered;
    private AlertDialog updateDialog;
    private OnBackInvokedCallback backInvokedCallback;
    private PlaybackSeekBarView expandedPlaybackSeekBar;
    private TextView expandedPlaybackProgressText;
    private TextView expandedSleepTimerRemainingText;
    private String renderedExpandedPlayerSignature = "";

    private EditText urlInput;
    private RadioGroup mediaGroup;
    private RadioButton audioRadio;
    private RadioButton videoRadio;
    private Spinner optionSpinner;
    private CheckBox subtitlesCheck;
    private CheckBox playlistCheck;
    private CheckBox metadataEnhanceCheck;
    private Button chooseFolderButton;
    private Button resetOutputButton;
    private Button extractButton;
    private Button cancelExtractButton;
    private TextView folderText;
    private TextView statusText;
    private TextView resultText;
    private PullRefreshScrollView appContentScrollView;
    private ProgressBar progressBar;

    private Tab currentTab = Tab.HOME;
    private MusicStation activeStation;
    private boolean playbackHasQueue;
    private boolean playbackPlaying;
    private boolean playbackPreparing;
    private boolean playbackWillPlay;
    private boolean playbackError;
    private String playbackTitle = "로컬 재생 대기";
    private String playbackMeta = "기기 음악을 스캔하면 재생할 수 있습니다.";
    private String streamStatus = "기기 음악을 스캔하면 추천 믹스가 표시됩니다.";
    private long playbackTrackId = -1L;
    private String playbackArtist = "알 수 없는 아티스트";
    private String playbackAlbum = "앨범 정보 없음";
    private String playbackFolder = "알 수 없는 폴더";
    private String playbackAlbumArtUri = "";
    private String playbackThemeAlbumArtUri = "";
    private String playbackThemeLoadingUri = "";
    private int playbackThemeColor = 0xFF17181D;
    private String playbackMix = "로컬 음악";
    private long playbackDurationMs;
    private long playbackPositionMs;
    private int playbackQueueIndex = -1;
    private int playbackQueueSize;
    private boolean playbackShuffleEnabled;
    private int playbackRepeatMode = PlaybackService.REPEAT_OFF;
    private boolean playbackSeeking;
    private boolean resumePlaybackAfterSeek;
    private long playbackSeekPreviewMs;
    private List<DeviceAudioTrack> activeQueuePreview = new ArrayList<>();
    private long[] playbackQueueTrackIds = new long[0];
    private boolean queuePreviewLoading;
    private long sleepTimerEndAtMs;
    private long sleepTimerRemainingMs;
    private int sleepTimerMinutes;
    private int sleepTimerDraftInitialMinutes;
    private boolean sleepTimerPaused;
    private boolean sleepTimerControlsVisible;
    private boolean suppressPlayerDragDismiss;
    private boolean playerDragDismissActive;
    private String extractorUrl = "";
    private MediaType extractorMediaType = MediaType.AUDIO;
    private String extractorOption = AudioFormat.M4A.value();
    private boolean extractorIncludeSubtitles;
    private boolean extractorIncludePlaylist;
    private boolean extractorEnhanceMetadata;

    private List<DeviceAudioTrack> libraryTracks = new ArrayList<>();
    private List<DeviceAudioTrack> homeTracks = new ArrayList<>();
    private List<OnlineStreamSection> streamSections = new ArrayList<>();
    private OnlineStreamSection focusedStreamSection;
    private StreamVideoSort streamVideoSort = StreamVideoSort.POPULAR;
    private final Set<String> streamMetadataLoadingChannelIds = new HashSet<>();
    private boolean libraryLoaded;
    private boolean libraryLoading;
    private String libraryStatus = "기기 음악 권한을 허용하면 앨범과 아티스트를 정리합니다.";
    private boolean homeLoaded;
    private boolean homeLoading;
    private String homeStatus = "보관함 음악을 스캔하면 추천 믹스가 표시됩니다.";
    private boolean streamLoaded;
    private boolean streamLoading;
    private String streamLoadStatus = "온라인 추천 채널을 불러옵니다.";
    private LibraryFilter libraryFilter = LibraryFilter.ALL;
    private LibrarySort librarySort = LibrarySort.NEWEST;
    private String librarySearchQuery = "";
    private String pendingLibrarySearchQuery = "";
    private final Runnable librarySearchCommit = () -> commitLibrarySearch(pendingLibrarySearchQuery);
    private boolean libraryGridView;
    private boolean librarySearchVisible;
    private ArtistDetailMode artistDetailMode = ArtistDetailMode.ALL;
    private EditText librarySearchInput;
    private String cachedLibraryTabKey = "";
    private int libraryDataVersion;
    private int playlistDataVersion;
    private String cachedVisibleTracksKey = "";
    private List<DeviceAudioTrack> cachedVisibleTracks = new ArrayList<>();
    private String cachedVisibleGroupsKey = "";
    private List<LibraryGroup> cachedVisibleGroups = new ArrayList<>();
    private LibraryGroup focusedLibraryGroup;
    private LibraryFilter focusedLibraryGroupFilter;
    private LibraryGroup focusedParentArtistGroup;
    private View libraryFilterGestureArea;
    private int libraryFilterScrollX;
    private float libraryPullStartX;
    private float libraryPullStartY;
    private boolean libraryPullTracking;
    private boolean libraryPullConsumed;
    private float libraryPullDistance;
    private boolean libraryPullReady;
    private View libraryPullIndicator;
    private PullRefreshIndicatorView libraryPullIndicatorView;
    private ObjectAnimator libraryPullSpinAnimator;
    private long libraryPullRefreshStartedAtMs;
    private boolean libraryPullRefreshSettling;
    private final Runnable libraryPullFinishRunnable = () -> {
        libraryPullRefreshSettling = false;
        libraryPullRefreshStartedAtMs = 0L;
        updateLibraryPullIndicator(0f, false);
    };
    private String librarySource = LIBRARY_SOURCE_COLLECTION;
    private DeviceAudioTrack selectedTrack;
    private DeviceAudioTrack pendingDeleteTrack;
    private List<DeviceAudioTrack> pendingDeleteTracks = new ArrayList<>();
    private final Map<Long, View> libraryTrackItemViews = new HashMap<>();
    private final Map<Long, String> librarySearchIndex = new HashMap<>();

    private String outputTreeUri;
    private int extractionPercent;
    private String extractionStatus = "대기 중";
    private String extractionResult = "-";
    private boolean extractionBusy;
    private boolean extractionCancelRequested;
    private boolean receiverRegistered;
    private boolean playbackReceiverRegistered;
    private boolean updateReceiverRegistered;
    private boolean updateChecking;
    private boolean updateChecked;
    private boolean updateDownloading;
    private String updateStatus = "정식 릴리즈 업데이트만 확인합니다.";
    private UpdateInfo availableUpdate;
    private String updateApkPath = "";
    private boolean extractionPendingNotificationPermission;
    private boolean updatePendingNotificationPermission;

    private final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ExtractionService.ACTION_PROGRESS.equals(intent.getAction())) {
                return;
            }

            int percent = intent.getIntExtra(ExtractionService.EXTRA_PERCENT, 0);
            String stage = intent.getStringExtra(ExtractionService.EXTRA_STAGE);
            String message = intent.getStringExtra(ExtractionService.EXTRA_MESSAGE);
            String error = intent.getStringExtra(ExtractionService.EXTRA_ERROR);
            String result = intent.getStringExtra(ExtractionService.EXTRA_RESULT);
            boolean done = intent.getBooleanExtra(ExtractionService.EXTRA_DONE, false);
            boolean canceled = intent.getBooleanExtra(ExtractionService.EXTRA_CANCELED, false);

            extractionPercent = percent;
            if (canceled) {
                extractionStatus = "취소됨";
                extractionResult = result == null ? "추출을 취소했습니다." : result;
                extractionCancelRequested = false;
            } else if (error != null) {
                extractionStatus = "오류";
                extractionResult = error;
                extractionCancelRequested = false;
            } else {
                extractionStatus = progressStatus(stage, message);
                if (result != null) {
                    extractionResult = result;
                }
            }

            if (done) {
                extractionBusy = false;
                extractionCancelRequested = false;
            } else {
                extractionBusy = true;
            }
            applyExtractionStateToViews();
        }
    };

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!UpdateDownloadService.ACTION_PROGRESS.equals(intent.getAction())) {
                return;
            }
            handleUpdateDownloadProgress(intent);
        }
    };

    private final BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PlaybackService.ACTION_SLEEP_TIMER_FINISHED.equals(intent.getAction())) {
                closeAfterSleepTimer();
                return;
            }
            if (!PlaybackService.ACTION_STATE.equals(intent.getAction())) {
                return;
            }
            boolean incomingHasQueue = intent.getBooleanExtra(PlaybackService.EXTRA_HAS_QUEUE, false);
            boolean incomingPreparing = intent.getBooleanExtra(PlaybackService.EXTRA_PREPARING, false);
            boolean keepPreviewDuringQueueLoad = !incomingHasQueue
                    && incomingPreparing
                    && activeStation != null
                    && playbackTrackId >= 0L;
            playbackHasQueue = incomingHasQueue || keepPreviewDuringQueueLoad;
            playbackPlaying = intent.getBooleanExtra(PlaybackService.EXTRA_PLAYING, false);
            playbackPreparing = incomingPreparing;
            playbackWillPlay = intent.getBooleanExtra(PlaybackService.EXTRA_WILL_PLAY, false);
            playbackError = intent.getBooleanExtra(PlaybackService.EXTRA_ERROR, false);
            if (!keepPreviewDuringQueueLoad) {
                playbackTitle = valueOrDefault(
                        intent.getStringExtra(PlaybackService.EXTRA_TITLE),
                        "로컬 재생 대기"
                );
                playbackMeta = valueOrDefault(
                        intent.getStringExtra(PlaybackService.EXTRA_META),
                        "기기 음악을 스캔하면 재생할 수 있습니다."
                );
                playbackTrackId = intent.getLongExtra(PlaybackService.EXTRA_TRACK_ID, -1L);
                playbackArtist = valueOrDefault(intent.getStringExtra(PlaybackService.EXTRA_ARTIST), "알 수 없는 아티스트");
                playbackAlbum = valueOrDefault(intent.getStringExtra(PlaybackService.EXTRA_ALBUM), "앨범 정보 없음");
                playbackFolder = valueOrDefault(intent.getStringExtra(PlaybackService.EXTRA_FOLDER), "알 수 없는 폴더");
                playbackAlbumArtUri = valueOrDefault(intent.getStringExtra(PlaybackService.EXTRA_ALBUM_ART_URI), "");
                playbackDurationMs = intent.getLongExtra(PlaybackService.EXTRA_DURATION_MS, 0L);
                playbackPositionMs = intent.getLongExtra(PlaybackService.EXTRA_POSITION_MS, 0L);
                playbackQueueIndex = intent.getIntExtra(PlaybackService.EXTRA_QUEUE_INDEX, -1);
                playbackQueueSize = intent.getIntExtra(PlaybackService.EXTRA_QUEUE_SIZE, 0);
            }
            streamStatus = keepPreviewDuringQueueLoad
                    ? "준비 중: " + playbackTitle
                    : valueOrDefault(intent.getStringExtra(PlaybackService.EXTRA_STATUS), playbackMeta);
            playbackMix = valueOrDefault(intent.getStringExtra(PlaybackService.EXTRA_MIX), "로컬 음악");
            playbackShuffleEnabled = intent.getBooleanExtra(PlaybackService.EXTRA_SHUFFLE_ENABLED, false);
            playbackRepeatMode = intent.getIntExtra(PlaybackService.EXTRA_REPEAT_MODE, PlaybackService.REPEAT_OFF);
            sleepTimerEndAtMs = intent.getLongExtra(PlaybackService.EXTRA_SLEEP_TIMER_END_AT_MS, 0L);
            sleepTimerRemainingMs = intent.getLongExtra(PlaybackService.EXTRA_SLEEP_TIMER_REMAINING_MS, 0L);
            sleepTimerPaused = intent.getBooleanExtra(PlaybackService.EXTRA_SLEEP_TIMER_PAUSED, false);
            if (!keepPreviewDuringQueueLoad) {
                updateQueuePreviewFromIds(intent.getLongArrayExtra(PlaybackService.EXTRA_QUEUE_TRACK_IDS));
            }
            if (!playbackHasQueue && !keepPreviewDuringQueueLoad) {
                activeStation = null;
                activeQueuePreview = new ArrayList<>();
                playbackQueueTrackIds = new long[0];
            }
            updateNowPlayingBar();
            updateExpandedPlaybackProgress();
            updateExpandedPlayer();
            updateQueueDialog();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyMainWindowBars();
        outputTreeUri = getPreferences().getString(PREF_OUTPUT_TREE, null);
        normalizeSavedOutputTree();
        librarySource = getPreferences().getString(PREF_LIBRARY_SOURCE, LIBRARY_SOURCE_COLLECTION);
        librarySort = LibrarySort.fromKey(getPreferences().getString(PREF_LIBRARY_SORT, LibrarySort.NEWEST.key));
        updateApkPath = getPreferences().getString(PREF_UPDATE_APK_PATH, "");
        ensureDefaultMediaFolders();
        clearInstalledPendingUpdateIfNeeded();
        applySharedUrlIntent(getIntent(), false);
        setContentView(buildContent());
        registerBackNavigationCallback();
        startUpdateCheck(false);
        handleUpdateInstallIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applySharedUrlIntent(intent, true);
        handleUpdateInstallIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(ExtractionService.ACTION_PROGRESS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(progressReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(progressReceiver, filter);
            }
            receiverRegistered = true;
        }
        if (!playbackReceiverRegistered) {
            IntentFilter filter = new IntentFilter(PlaybackService.ACTION_STATE);
            filter.addAction(PlaybackService.ACTION_SLEEP_TIMER_FINISHED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(playbackReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(playbackReceiver, filter);
            }
            playbackReceiverRegistered = true;
            startService(PlaybackService.commandIntent(this, PlaybackService.ACTION_REQUEST_STATE));
        }
        if (!updateReceiverRegistered) {
            IntentFilter filter = new IntentFilter(UpdateDownloadService.ACTION_PROGRESS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(updateReceiver, filter);
            }
            updateReceiverRegistered = true;
        }
        refreshPendingUpdateDownloadState();
    }

    @Override
    protected void onStop() {
        if (updateReceiverRegistered) {
            unregisterReceiver(updateReceiver);
            updateReceiverRegistered = false;
        }
        if (playbackReceiverRegistered) {
            unregisterReceiver(playbackReceiver);
            playbackReceiverRegistered = false;
        }
        if (receiverRegistered) {
            unregisterReceiver(progressReceiver);
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (playerDialog != null) {
            playerDialog.dismiss();
            playerDialog = null;
        }
        if (queueDialog != null) {
            queueDialog.dismiss();
            queueDialog = null;
        }
        if (updateDialog != null) {
            updateDialog.dismiss();
            updateDialog = null;
        }
        if (updateReceiverRegistered) {
            unregisterReceiver(updateReceiver);
            updateReceiverRegistered = false;
        }
        unregisterBackNavigationCallback();
        mainHandler.removeCallbacks(librarySearchCommit);
        mainHandler.removeCallbacks(libraryPullFinishRunnable);
        libraryExecutor.shutdownNow();
        streamExecutor.shutdownNow();
        streamResolveExecutor.shutdownNow();
        imageExecutor.shutdownNow();
        updateExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (closeOpenDialogFromBack()) {
            return;
        }
        if (handleStreamBackNavigation()) {
            return;
        }
        if (handleLibraryBackNavigation()) {
            return;
        }
        super.onBackPressed();
    }

    private void registerBackNavigationCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || backInvokedCallback != null) {
            return;
        }
        backInvokedCallback = () -> {
            if (closeOpenDialogFromBack()) {
                return;
            }
            if (handleStreamBackNavigation()) {
                return;
            }
            if (handleLibraryBackNavigation()) {
                return;
            }
            finish();
        };
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback
        );
    }

    private void unregisterBackNavigationCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || backInvokedCallback == null) {
            return;
        }
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backInvokedCallback);
        backInvokedCallback = null;
    }

    private boolean closeOpenDialogFromBack() {
        if (queueDialog != null && queueDialog.isShowing()) {
            collapseOrDismissQueueDialog();
            return true;
        }
        if (playerDialog != null && playerDialog.isShowing()) {
            playerDialog.dismiss();
            return true;
        }
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
            return true;
        }
        return false;
    }

    private boolean handleStreamBackNavigation() {
        if (currentTab != Tab.HOME || focusedStreamSection == null) {
            return false;
        }
        focusedStreamSection = null;
        renderCurrentTab();
        scrollHomeToTop();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OUTPUT_TREE) {
            handleOutputTreeResult(resultCode, data);
            return;
        }
        if (requestCode == REQUEST_DELETE_AUDIO && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && pendingDeleteTrack != null) {
                deleteTrackDirectly(pendingDeleteTrack);
                return;
            }
            int count = pendingDeleteTracks.isEmpty() ? 1 : pendingDeleteTracks.size();
            toast(count > 1 ? count + "개 파일을 삭제했습니다." : "선택한 파일을 삭제했습니다.");
            selectedTrack = null;
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
            focusedParentArtistGroup = null;
            pendingDeleteTrack = null;
            pendingDeleteTracks = new ArrayList<>();
            startLibraryRefresh(true);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (extractionPendingNotificationPermission) {
                extractionPendingNotificationPermission = false;
                if (granted) {
                    startExtraction();
                } else {
                    extractionStatus = "알림 권한이 필요합니다.";
                    extractionResult = "앱을 나가도 진행률과 성공/실패를 알림바에서 확인하려면 알림 권한을 허용해야 합니다.";
                    applyExtractionStateToViews();
                    toast("알림 권한이 없어 추출을 시작하지 않았습니다.");
                }
            }
            if (updatePendingNotificationPermission) {
                updatePendingNotificationPermission = false;
                if (granted) {
                    downloadUpdate(availableUpdate);
                } else {
                    updateDownloading = false;
                    updateStatus = "업데이트 다운로드 진행률과 완료 알림을 표시하려면 알림 권한이 필요합니다.";
                    renderUpdateState();
                    toast("알림 권한이 없어 업데이트 다운로드를 시작하지 않았습니다.");
                }
            }
            return;
        }
        if (requestCode != REQUEST_AUDIO_LIBRARY) {
            if (requestCode == REQUEST_WRITE_LIBRARY) {
                handleWritePermissionResult(grantResults);
            }
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLibraryRefresh(true);
        } else {
            libraryStatus = "기기 음악을 관리하려면 오디오 읽기 권한이 필요합니다.";
            renderLibraryDependentTabs();
        }
    }

    private View buildContent() {
        FrameLayout app = new FrameLayout(this);
        app.setBackgroundColor(color(R.color.ytet_background));

        contentFrame = new FrameLayout(this);
        appContentScrollView = new PullRefreshScrollView(this);
        contentScrollView = appContentScrollView;
        contentScrollView.setFillViewport(true);
        contentScrollView.setClipToPadding(false);
        contentScrollView.setBackgroundColor(color(R.color.ytet_background));
        contentFrame.addView(contentScrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        libraryRecyclerView = new PullRefreshRecyclerView(this);
        libraryRecyclerView.setVisibility(View.GONE);
        libraryRecyclerView.setClipToPadding(false);
        libraryRecyclerView.setBackgroundColor(color(R.color.ytet_background));
        libraryRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        libraryRecyclerView.setItemAnimator(null);
        contentFrame.addView(libraryRecyclerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        FrameLayout.LayoutParams refreshParams = new FrameLayout.LayoutParams(
                dp(56),
                dp(56),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        contentFrame.addView(libraryPullRefreshIndicator(), refreshParams);
        app.addView(contentFrame, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        bottomVignette = new View(this);
        bottomVignette.setBackground(bottomVignetteBackground());
        bottomVignette.setClickable(false);
        app.addView(bottomVignette, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                bottomVignetteHeight(0),
                Gravity.BOTTOM
        ));

        bottomChrome = new LinearLayout(this);
        bottomChrome.setOrientation(LinearLayout.VERTICAL);
        bottomChrome.setClickable(true);
        bottomChrome.setBackground(bottomChromeBackground());
        nowPlayingBar = buildNowPlayingBar();
        nowPlayingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams nowPlayingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(64)
        );
        nowPlayingParams.setMargins(dp(10), 0, dp(10), 0);
        bottomChrome.addView(nowPlayingBar, nowPlayingParams);
        bottomChrome.addView(buildBottomTabs(), matchWrap());
        bottomNavigationGuard = new View(this);
        bottomNavigationGuard.setBackgroundColor(Color.TRANSPARENT);
        bottomNavigationGuard.setOnTouchListener((view, event) -> true);
        bottomChrome.addView(bottomNavigationGuard, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
        ));
        app.addView(bottomChrome, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        ));
        updateMainContentBottomPadding(0);
        applyMainContentInsets(app);

        renderCurrentTab();
        return app;
    }

    private FrameLayout buildNowPlayingBar() {
        FrameLayout bar = new FrameLayout(this);
        bar.setBackground(nowPlayingBarBackground(true));
        bar.setOnClickListener(view -> showExpandedPlayer());

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(16), dp(10));
        bar.addView(row, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        nowPlayingInfoFrame = new FrameLayout(this);
        nowPlayingInfoFrame.setClickable(true);
        nowPlayingInfoFrame.setClipChildren(true);
        nowPlayingInfoFrame.setClipToPadding(true);
        nowPlayingInfoFrame.setOnTouchListener(this::handleNowPlayingInfoTouch);

        nowPlayingCurrentFrame = new FrameLayout(this);
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        nowPlayingCurrentFrame.addView(infoRow, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        nowPlayingInfoFrame.addView(nowPlayingCurrentFrame, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        nowPlayingCover = new FrameLayout(this);
        setNowPlayingCover(true);
        infoRow.addView(nowPlayingCover, marginRight(10, dp(44), dp(44)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);
        nowPlayingTitle = marqueeText("로컬 재생 대기", 14, R.color.ytet_text, true);
        nowPlayingMeta = marqueeText("기기 음악을 스캔하면 재생할 수 있습니다.", 12, R.color.ytet_muted, false);
        copy.addView(nowPlayingTitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(22)
        ));
        copy.addView(nowPlayingMeta, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(20)
        ));
        infoRow.addView(copy, new LinearLayout.LayoutParams(0, dp(44), 1f));

        nowPlayingPreviewFrame = new FrameLayout(this);
        nowPlayingPreviewFrame.setAlpha(0f);
        nowPlayingPreviewFrame.setVisibility(View.INVISIBLE);
        LinearLayout previewRow = new LinearLayout(this);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);
        nowPlayingPreviewFrame.addView(previewRow, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        nowPlayingPreviewCover = new FrameLayout(this);
        previewRow.addView(nowPlayingPreviewCover, marginRight(10, dp(44), dp(44)));
        LinearLayout previewCopy = new LinearLayout(this);
        previewCopy.setOrientation(LinearLayout.VERTICAL);
        previewCopy.setGravity(Gravity.CENTER_VERTICAL);
        nowPlayingPreviewTitle = marqueeText("", 14, R.color.ytet_text, true);
        nowPlayingPreviewMeta = marqueeText("", 12, R.color.ytet_muted, false);
        previewCopy.addView(nowPlayingPreviewTitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(22)
        ));
        previewCopy.addView(nowPlayingPreviewMeta, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(20)
        ));
        previewRow.addView(previewCopy, new LinearLayout.LayoutParams(0, dp(44), 1f));
        nowPlayingInfoFrame.addView(nowPlayingPreviewFrame, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        row.addView(nowPlayingInfoFrame, new LinearLayout.LayoutParams(0, dp(44), 1f));

        playPauseButton = iconButton(R.drawable.ic_play_arrow, "재생", true);
        playPauseButton.setOnClickListener(view -> toggleStreamPlayback());
        row.addView(playPauseButton, new LinearLayout.LayoutParams(dp(48), dp(44)));

        nowPlayingProgress = new MiniPlaybackProgressView(this);
        bar.addView(nowPlayingProgress, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(3),
                Gravity.BOTTOM
        ));
        return bar;
    }

    private LinearLayout buildBottomTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setClickable(true);
        tabs.setPadding(dp(10), dp(2), dp(10), dp(6));
        tabs.setBackgroundColor(Color.TRANSPARENT);

        homeTabButton = tabButton("스트림", Tab.HOME, R.drawable.ic_tab_stream_outline, R.drawable.ic_tab_stream_filled);
        libraryTabButton = tabButton("내 음악", Tab.LIBRARY, R.drawable.ic_tab_library_outline, R.drawable.ic_tab_library_filled);
        extractorTabButton = tabButton("추출기", Tab.EXTRACTOR, R.drawable.ic_tab_extract_outline, R.drawable.ic_tab_extract_filled);
        tabs.addView(homeTabButton.root, tabParams());
        tabs.addView(libraryTabButton.root, tabParams());
        tabs.addView(extractorTabButton.root, tabParams());
        return tabs;
    }

    private TabItem tabButton(String label, Tab tab, int outlineIcon, int filledIcon) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(0, dp(1), 0, dp(1));
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setOnClickListener(view -> showTab(tab));

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        root.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));

        TextView text = text(label, 11, R.color.ytet_muted, true);
        text.setGravity(Gravity.CENTER);
        text.setIncludeFontPadding(false);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, dp(1), 0, 0);
        root.addView(text, textParams);
        return new TabItem(root, icon, text, outlineIcon, filledIcon);
    }

    private void showTab(Tab tab) {
        if (currentTab == tab) {
            if (tab == Tab.HOME && focusedStreamSection != null) {
                focusedStreamSection = null;
                renderCurrentTab();
                scrollHomeToTop();
            }
            if (tab == Tab.LIBRARY && focusedLibraryGroup != null) {
                focusedLibraryGroup = null;
                focusedLibraryGroupFilter = null;
                focusedParentArtistGroup = null;
                artistDetailMode = ArtistDetailMode.ALL;
                selectedTrack = null;
                renderCurrentTab();
            }
            return;
        }
        saveCurrentTabInputs();
        saveLibraryTabScroll();
        currentTab = tab;
        renderCurrentTab();
    }

    private void showExtractorWithUrl(String sharedUrl, boolean renderImmediately) {
        String normalizedUrl = sharedUrl == null ? "" : sharedUrl.trim();
        if (normalizedUrl.isEmpty()) {
            return;
        }
        extractorUrl = normalizedUrl;
        currentTab = Tab.EXTRACTOR;
        if (urlInput != null) {
            urlInput.setText(normalizedUrl);
            urlInput.setSelection(urlInput.getText().length());
        }
        if (renderImmediately && contentScrollView != null) {
            renderCurrentTab();
            toast("공유한 링크를 추출기에 입력했습니다.");
        }
    }

    private void renderCurrentTab() {
        if (contentScrollView == null || libraryRecyclerView == null) {
            return;
        }
        saveLibraryTabScroll();
        if (currentTab == Tab.LIBRARY) {
            flushLibrarySearchInput();
            contentScrollView.setVisibility(View.GONE);
            libraryRecyclerView.setVisibility(View.VISIBLE);
            renderLibraryRecyclerTab();
            updateTabStyles();
            updateNowPlayingBar();
            updateLibraryPullIndicator(libraryLoading ? libraryPullRefreshTriggerDistance() : libraryPullDistance, libraryLoading || libraryPullReady);
            updateExtractorScrollMode();
            return;
        }

        libraryRecyclerView.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.VISIBLE);
        contentScrollView.removeAllViews();
        View view;
        if (currentTab == Tab.EXTRACTOR) {
            view = buildExtractorTab();
        } else {
            view = buildHomeTab();
        }
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        contentScrollView.addView(view, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        updateTabStyles();
        updateNowPlayingBar();
        resetLibraryPullIndicator();
        updateExtractorScrollMode();
    }

    private void ensureDefaultMediaFolders() {
        try {
            StorageWriter storageWriter = new StorageWriter();
            storageWriter.migrateLegacyDefaultFolders(this);
            storageWriter.ensureDefaultFolders();
        } catch (Exception ignored) {
            // MediaStore creates the public folders when the first file is saved on scoped-storage devices.
        }
    }

    private boolean handleLibraryPullToRefresh(MotionEvent event) {
        int action = event.getActionMasked();
        if (currentTab != Tab.LIBRARY || (contentScrollView == null && libraryRecyclerView == null)) {
            resetLibraryPullIndicator();
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            boolean blockedByFilterScroll = isTouchInsideView(event, libraryFilterGestureArea);
            libraryPullTracking = !blockedByFilterScroll && isLibraryContentAtTop() && !libraryLoading;
            libraryPullConsumed = false;
            libraryPullStartX = event.getRawX();
            libraryPullStartY = event.getRawY();
            libraryPullDistance = 0f;
            libraryPullReady = false;
            updateLibraryPullIndicator(0f, false);
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE && libraryPullTracking) {
            float horizontalDistance = event.getRawX() - libraryPullStartX;
            float dragDistance = event.getRawY() - libraryPullStartY;
            if (Math.abs(horizontalDistance) > dp(6) && Math.abs(horizontalDistance) > Math.abs(dragDistance)) {
                libraryPullTracking = false;
                libraryPullConsumed = false;
                libraryPullDistance = 0f;
                libraryPullReady = false;
                updateLibraryPullIndicator(0f, false);
                return false;
            }
            if (dragDistance <= 0f || !isLibraryContentAtTop()) {
                libraryPullDistance = 0f;
                libraryPullReady = false;
                updateLibraryPullIndicator(0f, false);
                return libraryPullConsumed;
            }
            libraryPullDistance = dragDistance;
            if (dragDistance > dp(2)) {
                libraryPullConsumed = true;
            }
            libraryPullReady = dragDistance >= libraryPullRefreshTriggerDistance();
            updateLibraryPullIndicator(dragDistance, libraryPullReady);
            return libraryPullConsumed;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            boolean shouldRefresh = action == MotionEvent.ACTION_UP && libraryPullTracking && libraryPullReady && !libraryLoading;
            boolean consumed = libraryPullConsumed || libraryPullReady;
            libraryPullTracking = false;
            libraryPullConsumed = false;
            libraryPullDistance = 0f;
            libraryPullReady = false;
            updateLibraryPullIndicator(shouldRefresh ? libraryPullRefreshTriggerDistance() : 0f, shouldRefresh);
            if (shouldRefresh) {
                startLibraryRefresh(false);
                return true;
            }
            if (consumed) {
                return true;
            }
        }
        return false;
    }

    private boolean isLibraryContentAtTop() {
        if (libraryRecyclerView != null && libraryRecyclerView.getVisibility() == View.VISIBLE) {
            return !libraryRecyclerView.canScrollVertically(-1);
        }
        return contentScrollView == null || contentScrollView.getScrollY() <= 0;
    }

    private boolean isTouchInsideView(MotionEvent event, View view) {
        if (event == null || view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        Rect rect = new Rect();
        if (!view.getGlobalVisibleRect(rect)) {
            return false;
        }
        return rect.contains((int) event.getRawX(), (int) event.getRawY());
    }

    private void resetLibraryPullIndicator() {
        libraryPullTracking = false;
        libraryPullConsumed = false;
        libraryPullDistance = 0f;
        libraryPullReady = false;
        libraryPullRefreshSettling = false;
        libraryPullRefreshStartedAtMs = 0L;
        mainHandler.removeCallbacks(libraryPullFinishRunnable);
        updateLibraryPullIndicator(0f, false);
    }

    private void updateLibraryPullIndicator(float dragDistance, boolean ready) {
        if (libraryPullIndicator == null) {
            return;
        }
        boolean refreshVisualActive = libraryLoading || libraryPullRefreshSettling;
        boolean visible = refreshVisualActive || ready || dragDistance > dp(4);
        float progress = refreshVisualActive
                ? 1f
                : Math.min(1f, Math.max(0f, dragDistance / libraryPullMaxDragDistance()));
        float offset = refreshVisualActive
                ? libraryPullRefreshOffset()
                : libraryPullHiddenOffset()
                + Math.min(libraryPullMaxOffset() - libraryPullHiddenOffset(), dragDistance * 0.78f);
        if (!visible) {
            hideLibraryPullIndicator();
            return;
        }
        libraryPullIndicator.animate().cancel();
        libraryPullIndicator.setVisibility(View.VISIBLE);
        libraryPullIndicator.setTranslationY(offset);
        libraryPullIndicator.setAlpha(refreshVisualActive || ready ? 1f : Math.max(0.35f, progress));
        float scale = 0.86f + progress * 0.14f;
        libraryPullIndicator.setScaleX(scale);
        libraryPullIndicator.setScaleY(scale);
        if (refreshVisualActive) {
            startLibraryPullSpin();
        } else {
            stopLibraryPullSpin();
            if (libraryPullIndicatorView != null) {
                libraryPullIndicatorView.setPullProgress(progress);
            }
        }
    }

    private void hideLibraryPullIndicator() {
        if (libraryPullIndicator == null || libraryPullIndicator.getVisibility() != View.VISIBLE) {
            stopLibraryPullSpin();
            if (libraryPullIndicatorView != null) {
                libraryPullIndicatorView.setPullProgress(0f);
            }
            return;
        }
        stopLibraryPullSpin();
        libraryPullIndicator.animate()
                .alpha(0f)
                .scaleX(0.82f)
                .scaleY(0.82f)
                .translationY(libraryPullHiddenOffset())
                .setDuration(180L)
                .withEndAction(() -> {
                    if (!libraryLoading && !libraryPullReady && libraryPullDistance <= 0f && libraryPullIndicator != null) {
                        libraryPullIndicator.setVisibility(View.INVISIBLE);
                        if (libraryPullIndicatorView != null) {
                            libraryPullIndicatorView.setPullProgress(0f);
                        }
                    }
                })
                .start();
    }

    private float libraryPullHiddenOffset() {
        return dp(92);
    }

    private float libraryPullRefreshOffset() {
        return dp(154);
    }

    private float libraryPullMaxOffset() {
        return dp(210);
    }

    private float libraryPullRefreshTriggerDistance() {
        return dp(86);
    }

    private float libraryPullMaxDragDistance() {
        return dp(150);
    }

    private void startLibraryPullSpin() {
        if (libraryPullIndicatorView == null) {
            return;
        }
        if (libraryPullSpinAnimator != null && libraryPullSpinAnimator.isStarted()) {
            return;
        }
        libraryPullIndicatorView.setRefreshing(true);
        libraryPullSpinAnimator = ObjectAnimator.ofFloat(
                libraryPullIndicatorView,
                "spinDegrees",
                libraryPullIndicatorView.getSpinDegrees(),
                libraryPullIndicatorView.getSpinDegrees() + 360f
        );
        libraryPullSpinAnimator.setDuration(720L);
        libraryPullSpinAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        libraryPullSpinAnimator.setInterpolator(new LinearInterpolator());
        libraryPullSpinAnimator.start();
    }

    private void stopLibraryPullSpin() {
        if (libraryPullSpinAnimator != null) {
            libraryPullSpinAnimator.cancel();
            libraryPullSpinAnimator = null;
        }
        if (libraryPullIndicatorView != null) {
            libraryPullIndicatorView.setRefreshing(false);
        }
    }

    private void beginLibraryPullRefreshVisual() {
        if (currentTab != Tab.LIBRARY || libraryPullIndicator == null) {
            return;
        }
        libraryPullRefreshStartedAtMs = System.currentTimeMillis();
        libraryPullRefreshSettling = false;
        mainHandler.removeCallbacks(libraryPullFinishRunnable);
        updateLibraryPullIndicator(libraryPullRefreshTriggerDistance(), true);
    }

    private void finishLibraryPullRefreshVisual() {
        if (libraryPullRefreshStartedAtMs <= 0L || libraryPullIndicator == null) {
            return;
        }
        if (currentTab != Tab.LIBRARY) {
            libraryPullRefreshSettling = false;
            libraryPullRefreshStartedAtMs = 0L;
            mainHandler.removeCallbacks(libraryPullFinishRunnable);
            return;
        }
        long elapsedMs = Math.max(0L, System.currentTimeMillis() - libraryPullRefreshStartedAtMs);
        long delayMs = Math.max(220L, LIBRARY_PULL_MIN_REFRESH_VISIBLE_MS - elapsedMs);
        libraryPullRefreshSettling = true;
        mainHandler.removeCallbacks(libraryPullFinishRunnable);
        updateLibraryPullIndicator(libraryPullRefreshTriggerDistance(), true);
        mainHandler.postDelayed(libraryPullFinishRunnable, delayMs);
    }

    private View buildHomeTab() {
        LinearLayout root = screenRoot();
        if (!streamLoaded && !streamLoading) {
            startStreamRefresh(false);
        }
        if (focusedStreamSection != null) {
            OnlineStreamSection section = currentFocusedStreamSection();
            if (section != null) {
                buildStreamSectionDetail(root, section);
                return root;
            }
            focusedStreamSection = null;
        }

        if (streamLoading && streamSections.isEmpty()) {
            root.addView(streamLoadingView(), matchWrap());
            return root;
        }
        if (!streamLoadStatus.trim().isEmpty()) {
            root.addView(muted(streamLoadStatus, 13), marginBottom(14));
        }
        if (streamSections.isEmpty()) {
            LinearLayout empty = panel();
            empty.addView(label("표시할 스트림이 없습니다."), marginBottom(8));
            empty.addView(muted("온라인 상태에서 다시 열면 추천 채널을 불러옵니다.", 13), matchWrap());
            root.addView(empty, marginBottom(18));
            return root;
        }
        for (OnlineStreamSection section : streamSections) {
            root.addView(streamSectionView(section), marginBottom(18));
        }
        return root;
    }

    private View streamLoadingView() {
        LinearLayout loading = new LinearLayout(this);
        loading.setOrientation(LinearLayout.VERTICAL);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(28), 0, dp(28));

        int height = Math.max(dp(280), getResources().getDisplayMetrics().heightPixels - dp(330));
        StreamLoadingSpinnerView spinner = new StreamLoadingSpinnerView(this);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        spinnerParams.setMargins(0, 0, 0, dp(18));
        loading.addView(spinner, spinnerParams);

        TextView title = label("추천을 불러오는 중");
        title.setGravity(Gravity.CENTER);
        loading.addView(title, marginBottom(8));

        TextView hint = muted("네트워크 상태에 따라 잠시 걸릴 수 있습니다.", 13);
        hint.setGravity(Gravity.CENTER);
        loading.addView(hint, matchWrap());

        loading.setMinimumHeight(height);
        return loading;
    }

    private View streamSectionView(OnlineStreamSection section) {
        LinearLayout panel = panel();
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(4), dp(4), dp(4));
        header.setBackground(rounded(Color.TRANSPARENT, 8));
        header.setClickable(true);
        header.setOnClickListener(view -> showStreamSectionDetail(section));
        header.addView(onlineImageView(section.avatarUrl(), section.channelTitle(), 44, 22), marginRight(12, dp(44), dp(44)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(section.channelTitle(), 17, R.color.ytet_text, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title, marginBottom(2));
        copy.addView(muted("YouTube 추천 플레이리스트", 12), matchWrap());
        header.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        panel.addView(header, marginBottom(12));

        List<OnlineStreamVideo> videos = section.videos();
        int previewCount = Math.min(3, videos.size());
        for (int index = 0; index < previewCount; index++) {
            panel.addView(streamVideoRow(section, videos.get(index), index), marginBottom(index == previewCount - 1 ? 0 : 8));
        }
        return panel;
    }

    private void showStreamSectionDetail(OnlineStreamSection section) {
        focusedStreamSection = section;
        streamVideoSort = StreamVideoSort.POPULAR;
        renderCurrentTab();
        scrollHomeToTop();
    }

    private void scrollHomeToTop() {
        if (contentScrollView != null) {
            contentScrollView.post(() -> contentScrollView.scrollTo(0, 0));
        }
    }

    private OnlineStreamSection currentFocusedStreamSection() {
        if (focusedStreamSection == null) {
            return null;
        }
        for (OnlineStreamSection section : streamSections) {
            if (TextUtils.equals(section.channelId(), focusedStreamSection.channelId())) {
                return section;
            }
        }
        return focusedStreamSection;
    }

    private void buildStreamSectionDetail(LinearLayout root, OnlineStreamSection section) {
        ensureStreamSectionMetadata(section);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back = toolbarIconButton(R.drawable.ic_arrow_back, "스트림 목록", false);
        back.setOnClickListener(view -> handleStreamBackNavigation());
        top.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, dp(44), 1f));
        root.addView(top, marginBottom(16));

        LinearLayout hero = panel();
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(12), dp(12), dp(12), dp(12));
        hero.addView(onlineImageView(section.avatarUrl(), section.channelTitle(), 58, 29), marginRight(12, dp(58), dp(58)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(section.channelTitle(), 22, R.color.ytet_text, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title, marginBottom(4));
        copy.addView(muted(section.videos().size() + "개 비디오", 12), matchWrap());
        hero.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams sortParams = new LinearLayout.LayoutParams(dp(104), dp(44));
        sortParams.setMargins(dp(12), 0, 0, 0);
        hero.addView(streamSortDropdownButton(), sortParams);
        root.addView(hero, marginBottom(12));

        if (streamMetadataLoadingChannelIds.contains(section.channelId())) {
            root.addView(muted("조회수 기준 정렬 정보를 불러오는 중", 12), marginBottom(10));
        }

        List<OnlineStreamVideo> videos = sortedStreamVideos(section);
        if (videos.isEmpty()) {
            LinearLayout empty = panel();
            empty.addView(label("표시할 비디오가 없습니다."), matchWrap());
            root.addView(empty, marginBottom(18));
            return;
        }
        prefetchOnlineStreamVideos(videos, 8);
        for (int index = 0; index < videos.size(); index++) {
            OnlineStreamVideo video = videos.get(index);
            int sourceIndex = streamVideoIndex(section, video);
            root.addView(streamVideoRow(section, video, sourceIndex), marginBottom(index == videos.size() - 1 ? 0 : 8));
        }
    }

    private void ensureStreamSectionMetadata(OnlineStreamSection section) {
        if (section == null
                || streamSectionHasMetadata(section)
                || streamMetadataLoadingChannelIds.contains(section.channelId())) {
            return;
        }
        streamMetadataLoadingChannelIds.add(section.channelId());
        List<OnlineStreamVideo> videos = section.videos();
        streamExecutor.execute(() -> {
            try {
                List<OnlineStreamVideo> enriched = OnlineStreamClient.enrichVideos(this, videos);
                runOnUiThread(() -> finishStreamSectionMetadataLoad(section, enriched));
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    streamMetadataLoadingChannelIds.remove(section.channelId());
                    if (focusedStreamSection != null
                            && TextUtils.equals(focusedStreamSection.channelId(), section.channelId())) {
                        renderCurrentTab();
                    }
                });
            }
        });
    }

    private void finishStreamSectionMetadataLoad(OnlineStreamSection section, List<OnlineStreamVideo> enriched) {
        streamMetadataLoadingChannelIds.remove(section.channelId());
        if (enriched == null || enriched.isEmpty()) {
            return;
        }
        OnlineStreamSection updated = new OnlineStreamSection(
                section.channelId(),
                section.channelTitle(),
                section.channelUrl(),
                section.avatarUrl(),
                enriched
        );
        List<OnlineStreamSection> updatedSections = new ArrayList<>();
        for (OnlineStreamSection existing : streamSections) {
            updatedSections.add(TextUtils.equals(existing.channelId(), section.channelId()) ? updated : existing);
        }
        streamSections = updatedSections;
        if (focusedStreamSection != null
                && TextUtils.equals(focusedStreamSection.channelId(), section.channelId())) {
            focusedStreamSection = updated;
            renderCurrentTab();
        }
    }

    private boolean streamSectionHasMetadata(OnlineStreamSection section) {
        for (OnlineStreamVideo video : section.videos()) {
            if (video.viewCount() > 0L || video.publishedRank() > 0L) {
                return true;
            }
        }
        return false;
    }

    private View streamSortDropdownButton() {
        Button button = secondaryButton(streamVideoSort.label + " ▾");
        button.setTextSize(13);
        button.setOnClickListener(this::showStreamSortMenu);
        return button;
    }

    private void showStreamSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        for (StreamVideoSort sort : StreamVideoSort.values()) {
            menu.getMenu().add(0, sort.ordinal(), sort.ordinal(), sort.label);
        }
        menu.setOnMenuItemClickListener(item -> {
            StreamVideoSort[] sorts = StreamVideoSort.values();
            int index = item.getItemId();
            if (index < 0 || index >= sorts.length || streamVideoSort == sorts[index]) {
                return true;
            }
            streamVideoSort = sorts[index];
            renderCurrentTab();
            return true;
        });
        menu.show();
    }

    private List<OnlineStreamVideo> sortedStreamVideos(OnlineStreamSection section) {
        List<OnlineStreamVideo> videos = section == null ? new ArrayList<>() : section.videos();
        videos.sort((first, second) -> compareStreamVideos(first, second));
        return videos;
    }

    private int compareStreamVideos(OnlineStreamVideo first, OnlineStreamVideo second) {
        if (streamVideoSort == StreamVideoSort.POPULAR) {
            int viewCompare = Long.compare(second.viewCount(), first.viewCount());
            if (viewCompare != 0) {
                return viewCompare;
            }
            int popularCompare = compareStreamVideosByPopularRank(first, second);
            if (popularCompare != 0) {
                return popularCompare;
            }
            return compareStreamVideosByLatest(first, second);
        }
        if (streamVideoSort == StreamVideoSort.OLDEST) {
            return compareStreamVideosByOldest(first, second);
        }
        return compareStreamVideosByLatest(first, second);
    }

    private int compareStreamVideosByPopularRank(OnlineStreamVideo first, OnlineStreamVideo second) {
        int firstRank = first.popularRank() <= 0 ? Integer.MAX_VALUE : first.popularRank();
        int secondRank = second.popularRank() <= 0 ? Integer.MAX_VALUE : second.popularRank();
        return Integer.compare(firstRank, secondRank);
    }

    private int compareStreamVideosByLatest(OnlineStreamVideo first, OnlineStreamVideo second) {
        if (first.publishedRank() > 0L || second.publishedRank() > 0L) {
            int publishedCompare = Long.compare(second.publishedRank(), first.publishedRank());
            if (publishedCompare != 0) {
                return publishedCompare;
            }
        }
        return Integer.compare(first.sourceIndex(), second.sourceIndex());
    }

    private int compareStreamVideosByOldest(OnlineStreamVideo first, OnlineStreamVideo second) {
        if (first.publishedRank() > 0L && second.publishedRank() > 0L) {
            int publishedCompare = Long.compare(first.publishedRank(), second.publishedRank());
            if (publishedCompare != 0) {
                return publishedCompare;
            }
        } else if (first.publishedRank() > 0L) {
            return -1;
        } else if (second.publishedRank() > 0L) {
            return 1;
        }
        return Integer.compare(second.sourceIndex(), first.sourceIndex());
    }

    private int streamVideoIndex(OnlineStreamSection section, OnlineStreamVideo target) {
        if (section == null || target == null) {
            return 0;
        }
        List<OnlineStreamVideo> videos = section.videos();
        for (int index = 0; index < videos.size(); index++) {
            OnlineStreamVideo video = videos.get(index);
            if (TextUtils.equals(video.id(), target.id())
                    || TextUtils.equals(video.watchUrl(), target.watchUrl())) {
                return index;
            }
        }
        return Math.max(0, Math.min(videos.size() - 1, target.sourceIndex()));
    }

    private View streamVideoRow(OnlineStreamSection section, OnlineStreamVideo video, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));
        row.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
        row.setOnClickListener(view -> playOnlineStream(section, index));

        LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(dp(102), dp(58));
        thumbParams.setMargins(0, 0, dp(12), 0);
        row.addView(onlineImageView(video.thumbnailUrl(), video.title(), 102, 8), thumbParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(video.title(), 14, R.color.ytet_text, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title, marginBottom(4));
        TextView meta = muted(video.channelTitle(), 12);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(meta, matchWrap());
        row.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View stationCard(MusicStation station, List<DeviceAudioTrack> sourceTracks) {
        LinearLayout card = panel();
        card.setBackground(rounded(station.accentColor(), 8));
        card.setOnClickListener(view -> playStation(station, sourceTracks));

        TextView category = text(station.category(), 12, android.R.color.white, true);
        category.setAlpha(0.86f);
        card.addView(category, marginBottom(10));
        card.addView(text(station.title(), 18, android.R.color.white, true), marginBottom(8));
        card.addView(text(station.subtitle(), 13, android.R.color.white, false), marginBottom(12));
        TextView description = text(station.description(), 12, android.R.color.white, false);
        description.setAlpha(0.82f);
        description.setMinLines(3);
        card.addView(description, marginBottom(12));
        Button play = compactButton("재생");
        play.setOnClickListener(view -> playStation(station, sourceTracks));
        card.addView(play, matchWrap());
        return card;
    }

    private View updatePanel() {
        LinearLayout panel = panel();
        panel.addView(label("업데이트"), marginBottom(8));
        panel.addView(muted("새로운 업데이트가 있습니다.", 13), marginBottom(10));
        updateStatusText = text(updateStatus, 14, R.color.ytet_text, false);
        panel.addView(updateStatusText, marginBottom(12));
        updateActionButton = secondaryButton(updateActionLabel());
        updateActionButton.setEnabled(!updateChecking && !updateDownloading);
        updateActionButton.setOnClickListener(view -> handleUpdateAction());
        panel.addView(updateActionButton, matchWrap());
        return panel;
    }

    private String updateActionLabel() {
        if (updateChecking) {
            return "확인 중";
        }
        if (updateDownloading) {
            return "다운로드 중";
        }
        if (isDownloadedUpdateReady()) {
            return "설치";
        }
        if (availableUpdate != null) {
            return "다운로드";
        }
        return updateChecked ? "다시 확인" : "업데이트 확인";
    }

    private void handleUpdateAction() {
        if (updateChecking || updateDownloading) {
            return;
        }
        if (isDownloadedUpdateReady()) {
            installDownloadedUpdate();
            return;
        }
        if (availableUpdate != null) {
            downloadUpdate(availableUpdate);
            return;
        }
        startUpdateCheck(true);
    }

    private void showUpdateAvailableDialog(UpdateInfo update) {
        if (!canShowUpdateDialog() || update == null || update.apkUrl().isEmpty()) {
            return;
        }
        dismissUpdateDialog();

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout body = dialogBody("업데이트 사용 가능");
        body.addView(muted("새로운 업데이트가 있습니다.", 13), marginBottom(12));
        body.addView(trackDetailItem("현재 버전", displayVersionTag(currentAppVersionName())), marginBottom(10));
        body.addView(trackDetailItem("새 버전", displayVersionTag(update.tagName())), marginBottom(10));
        body.addView(trackDetailItem("파일", update.apkName()), marginBottom(14));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button later = detailActionButton("나중에");
        later.setOnClickListener(view -> dialog.dismiss());
        Button download = detailActionButton("다운로드");
        download.setOnClickListener(view -> {
            dialog.dismiss();
            downloadUpdate(update);
        });
        actions.addView(later, fixedButtonParams(82, 38, 8));
        actions.addView(download, fixedButtonParams(94, 38, 0));
        body.addView(actions, matchWrap());

        dialog.setView(body);
        updateDialog = dialog;
        dialog.setOnDismissListener(view -> {
            if (updateDialog == dialog) {
                updateDialog = null;
            }
        });
        dialog.show();
        styleDetailDialog(dialog);
    }

    private void showDownloadedUpdateDialog(String tag) {
        if (!canShowUpdateDialog() || !isDownloadedUpdateReady()) {
            return;
        }
        dismissUpdateDialog();

        String updateTag = tag == null || tag.trim().isEmpty() ? "다운로드한 업데이트" : tag.trim();
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout body = dialogBody("업데이트 설치 준비 완료");
        body.addView(muted(displayUpdateTag(updateTag) + " APK 다운로드가 완료되었습니다. Android 설치 화면을 열어 업데이트를 승인할 수 있습니다.", 13), marginBottom(14));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button close = detailActionButton("닫기");
        close.setOnClickListener(view -> dialog.dismiss());
        Button install = detailActionButton("설치");
        install.setOnClickListener(view -> {
            dialog.dismiss();
            installDownloadedUpdate();
        });
        actions.addView(close, fixedButtonParams(76, 38, 8));
        actions.addView(install, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());

        dialog.setView(body);
        updateDialog = dialog;
        dialog.setOnDismissListener(view -> {
            if (updateDialog == dialog) {
                updateDialog = null;
            }
        });
        dialog.show();
        styleDetailDialog(dialog);
    }

    private void showUpdateDownloadDialog(UpdateInfo update) {
        if (!canShowUpdateDialog()) {
            return;
        }
        dismissUpdateDialog();

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setCancelable(false);
        LinearLayout body = dialogBody("업데이트 다운로드");
        String tag = update == null ? "새 버전" : displayVersionTag(update.tagName());
        body.addView(muted(tag + " APK를 다운로드하고 있습니다.", 13), marginBottom(12));

        updateDownloadStatusText = text("다운로드 준비 중입니다.", 14, R.color.ytet_text, false);
        body.addView(updateDownloadStatusText, marginBottom(12));

        updateDownloadProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        updateDownloadProgressBar.setIndeterminate(true);
        updateDownloadProgressBar.setMax(100);
        updateDownloadProgressBar.setProgress(0);
        body.addView(updateDownloadProgressBar, matchWrap());

        dialog.setView(body);
        updateDialog = dialog;
        dialog.setOnDismissListener(view -> {
            if (updateDialog == dialog) {
                updateDialog = null;
            }
            clearUpdateDownloadProgressViews();
        });
        dialog.show();
        styleDetailDialog(dialog);
    }

    private void showUpdateMessageDialog(String title, String message) {
        if (!canShowUpdateDialog()) {
            toast(message);
            return;
        }
        dismissUpdateDialog();

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout body = dialogBody(title);
        body.addView(muted(message, 13), marginBottom(14));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button close = detailActionButton("닫기");
        close.setOnClickListener(view -> dialog.dismiss());
        actions.addView(close, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());

        dialog.setView(body);
        updateDialog = dialog;
        dialog.setOnDismissListener(view -> {
            if (updateDialog == dialog) {
                updateDialog = null;
            }
        });
        dialog.show();
        styleDetailDialog(dialog);
    }

    private boolean canShowUpdateDialog() {
        return !isFinishing() && !isDestroyed();
    }

    private void dismissUpdateDialog() {
        if (updateDialog != null) {
            updateDialog.dismiss();
            updateDialog = null;
        }
        clearUpdateDownloadProgressViews();
    }

    private void clearUpdateDownloadProgressViews() {
        updateDownloadStatusText = null;
        updateDownloadProgressBar = null;
    }

    private void startUpdateCheck(boolean manual) {
        if (updateChecking) {
            return;
        }
        updateChecking = true;
        updateStatus = updateChannelLabel() + " 업데이트를 확인하는 중입니다.";
        renderUpdateState();
        String currentVersionName = currentAppVersionName();
        updateExecutor.execute(() -> {
            try {
                UpdateInfo update = updateChecker.checkForUpdate(currentVersionName);
                runOnUiThread(() -> {
                    updateChecking = false;
                    updateChecked = true;
                    availableUpdate = update;
                    if (update == null) {
                        updateStatus = "현재 설치된 " + displayVersionTag(currentVersionName) + " 버전이 최신 " + updateChannelLabel() + "입니다.";
                    } else {
                        updateStatus = displayVersionTag(update.tagName()) + " 업데이트를 사용할 수 있습니다.";
                        showUpdateAvailableDialog(update);
                    }
                    renderUpdateState();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    updateChecking = false;
                    updateChecked = true;
                    if (manual) {
                        updateStatus = "업데이트 확인 실패: " + safeMessage(exception);
                    } else {
                        updateStatus = "업데이트 확인에 실패했습니다. 필요할 때 다시 확인하세요.";
                    }
                    renderUpdateState();
                });
            }
        });
    }

    private void downloadUpdate(UpdateInfo update) {
        if (update == null || update.apkUrl().isEmpty()) {
            toast("다운로드할 업데이트 파일이 없습니다.");
            return;
        }
        if (!hasNotificationPermission()) {
            updatePendingNotificationPermission = true;
            showUpdateNotificationPermissionRationale();
            return;
        }
        dismissUpdateDialog();
        updateDownloading = true;
        updateStatus = displayUpdateTag(update.tagName()) + " 업데이트 APK를 다운로드하는 중입니다.";
        renderUpdateState();
        showUpdateDownloadDialog(update);

        Intent intent = UpdateDownloadService.downloadIntent(this, update);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception exception) {
            updateDownloading = false;
            updateStatus = "업데이트 다운로드 서비스를 시작할 수 없습니다: " + safeMessage(exception);
            renderUpdateState();
            showUpdateMessageDialog("업데이트 다운로드 실패", updateStatus);
        }
    }

    private void installDownloadedUpdate() {
        updateApkPath = getPreferences().getString(PREF_UPDATE_APK_PATH, updateApkPath);
        File apkFile = updateApkFile();
        if (apkFile == null || !apkFile.isFile()) {
            toast("설치할 업데이트 APK가 아직 준비되지 않았습니다.");
            clearPendingUpdateDownload();
            return;
        }
        dismissUpdateDialog();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            updateStatus = "설치를 계속하려면 RabbYT의 알 수 없는 앱 설치를 허용한 뒤 설치를 다시 누르세요.";
            renderUpdateState();
            openInstallPermissionSettings();
            return;
        }
        Uri apkUri = UpdateApkProvider.uriFor(this, apkFile);

        Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        install.setData(apkUri);
        install.setClipData(ClipData.newRawUri("RabbYT update", apkUri));
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        install.putExtra(Intent.EXTRA_RETURN_RESULT, false);
        try {
            startActivity(install);
            updateStatus = "Android 설치 화면에서 업데이트를 승인하세요.";
            renderUpdateState();
        } catch (ActivityNotFoundException exception) {
            Intent fallback = new Intent(Intent.ACTION_VIEW);
            fallback.setDataAndType(apkUri, UPDATE_APK_MIME);
            fallback.setClipData(ClipData.newRawUri("RabbYT update", apkUri));
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(fallback);
                updateStatus = "Android 설치 화면에서 업데이트를 승인하세요.";
                renderUpdateState();
            } catch (ActivityNotFoundException fallbackException) {
                updateStatus = "APK 설치 화면을 열 수 없습니다.";
                renderUpdateState();
            }
        }
    }

    private void openInstallPermissionSettings() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getPackageName())
        );
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        }
    }

    private void refreshPendingUpdateDownloadState() {
        clearInstalledPendingUpdateIfNeeded();
        updateApkPath = getPreferences().getString(PREF_UPDATE_APK_PATH, "");
        File apkFile = updateApkFile();
        if (apkFile == null) {
            return;
        }
        if (apkFile.isFile()) {
            updateDownloading = false;
            String tag = getPreferences().getString(PREF_UPDATE_TAG, "다운로드한 업데이트");
            updateStatus = displayUpdateTag(tag) + " APK 다운로드가 완료되었습니다. 설치할 수 있습니다.";
            renderUpdateState();
            showDownloadedUpdateDialog(tag);
            return;
        }
        clearPendingUpdateDownload();
        updateDownloading = false;
        updateStatus = "이전 업데이트 파일을 찾을 수 없습니다.";
        renderUpdateState();
    }

    private boolean isDownloadedUpdateReady() {
        updateApkPath = getPreferences().getString(PREF_UPDATE_APK_PATH, updateApkPath);
        File apkFile = updateApkFile();
        return apkFile != null && apkFile.isFile();
    }

    private void clearInstalledPendingUpdateIfNeeded() {
        String tag = getPreferences().getString(PREF_UPDATE_TAG, "");
        if (!tag.isEmpty() && UpdateChecker.isDownloadedUpdateInstalled(tag, currentAppVersionName())) {
            clearPendingUpdateDownload();
            updateDownloading = false;
        }
    }

    private void clearPendingUpdateDownload() {
        File apkFile = updateApkFile();
        if (apkFile != null && apkFile.isFile()) {
            apkFile.delete();
        }
        updateApkPath = "";
        getPreferences().edit()
                .remove(PREF_UPDATE_APK_PATH)
                .remove(PREF_UPDATE_DOWNLOAD_ID)
                .remove(PREF_UPDATE_TAG)
                .apply();
    }

    private void renderUpdateState() {
        if (updateStatusText != null) {
            updateStatusText.setText(updateStatus);
        }
        if (updateActionButton != null) {
            updateActionButton.setText(updateActionLabel());
            updateActionButton.setEnabled(!updateChecking && !updateDownloading);
        }
    }

    private File updateApkFile() {
        if (updateApkPath == null || updateApkPath.trim().isEmpty()) {
            return null;
        }
        return new File(updateApkPath);
    }

    private void handleUpdateInstallIntent(Intent intent) {
        if (intent == null || !UpdateDownloadService.ACTION_INSTALL_UPDATE.equals(intent.getAction())) {
            return;
        }
        updateApkPath = getPreferences().getString(PREF_UPDATE_APK_PATH, updateApkPath);
        installDownloadedUpdate();
    }

    private void handleUpdateDownloadProgress(Intent intent) {
        boolean done = intent.getBooleanExtra(UpdateDownloadService.EXTRA_DONE, false);
        boolean canceled = intent.getBooleanExtra(UpdateDownloadService.EXTRA_CANCELED, false);
        String error = intent.getStringExtra(UpdateDownloadService.EXTRA_ERROR);
        String message = valueOrDefault(intent.getStringExtra(UpdateDownloadService.EXTRA_MESSAGE), "다운로드 중입니다.");
        String tag = valueOrDefault(intent.getStringExtra(UpdateDownloadService.EXTRA_TAG), "다운로드한 업데이트");
        int percent = intent.getIntExtra(UpdateDownloadService.EXTRA_PERCENT, -1);

        if (error != null) {
            updateDownloading = false;
            clearPendingUpdateDownload();
            updateStatus = error;
            renderUpdateState();
            showUpdateMessageDialog("업데이트 다운로드 실패", error);
            return;
        }
        if (canceled) {
            updateDownloading = false;
            updateStatus = message;
            renderUpdateState();
            dismissUpdateDialog();
            toast(message);
            return;
        }
        if (done) {
            updateDownloading = false;
            updateApkPath = valueOrDefault(
                    intent.getStringExtra(UpdateDownloadService.EXTRA_APK_PATH),
                    getPreferences().getString(PREF_UPDATE_APK_PATH, "")
            );
            updateStatus = displayUpdateTag(tag) + " APK 다운로드가 완료되었습니다. 설치할 수 있습니다.";
            updateDownloadProgress(100, "다운로드 완료. 설치할 수 있습니다.");
            renderUpdateState();
            showDownloadedUpdateDialog(tag);
            return;
        }

        updateDownloading = true;
        updateStatus = displayUpdateTag(tag) + " 업데이트 APK를 다운로드하는 중입니다.";
        if (updateDownloadProgressBar == null && updateDialog == null && canShowUpdateDialog()) {
            showUpdateDownloadDialog(availableUpdate);
        }
        updateDownloadProgress(percent, message);
        renderUpdateState();
    }

    private void updateDownloadProgress(int percent, String status) {
        if (updateDownloadProgressBar == null || updateDownloadStatusText == null) {
            return;
        }
        if (percent >= 0) {
            updateDownloadProgressBar.setIndeterminate(false);
            updateDownloadProgressBar.setProgress(Math.max(0, Math.min(100, percent)));
        } else {
            updateDownloadProgressBar.setIndeterminate(true);
        }
        updateDownloadStatusText.setText(status == null || status.trim().isEmpty()
                ? "다운로드 중입니다."
                : status);
    }

    private String currentAppVersionName() {
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
            return versionName == null || versionName.trim().isEmpty() ? "0.0.0" : versionName.trim();
        } catch (PackageManager.NameNotFoundException exception) {
            return "0.0.0";
        }
    }

    private String displayVersionTag(String value) {
        String version = value == null ? "" : value.trim();
        if (version.isEmpty()) {
            return "v0.0.0";
        }
        if (version.matches("(?i).*nightly[-.]\\d+$")) {
            return "Nightly " + version.replaceFirst("(?i)^.*nightly[-.](\\d+)$", "$1");
        }
        version = version.replaceFirst("^v", "");
        version = version.replaceFirst("-android$", "");
        return "v" + version;
    }

    private String displayUpdateTag(String value) {
        String tag = value == null ? "" : value.trim();
        if (tag.matches("(?i)^v?\\d+\\.\\d+\\.\\d+.*") || tag.matches("(?i).*nightly[-.]\\d+$")) {
            return displayVersionTag(tag);
        }
        return tag.isEmpty() ? "다운로드한 업데이트" : tag;
    }

    private String updateChannelLabel() {
        return UpdateChecker.CHANNEL_NIGHTLY.equals(BuildConfig.UPDATE_CHANNEL) ? "Beta" : "정식 릴리즈";
    }

    private void saveLibraryTabScroll() {
        if (libraryRecyclerView != null
                && libraryRecyclerView.getVisibility() == View.VISIBLE
                && libraryRecyclerView.getLayoutManager() != null) {
            libraryRecyclerState = libraryRecyclerView.getLayoutManager().onSaveInstanceState();
        }
    }

    private String libraryTabCacheKey() {
        String focusedKey = focusedLibraryGroup == null ? "" : focusedLibraryGroup.key;
        String focusedFilter = focusedLibraryGroupFilter == null ? "" : focusedLibraryGroupFilter.name();
        String parentKey = focusedParentArtistGroup == null ? "" : focusedParentArtistGroup.key;
        return librarySource
                + "|data=" + libraryDataVersion
                + "|playlists=" + playlistDataVersion
                + "|loaded=" + libraryLoaded
                + "|loading=" + libraryLoading
                + "|status=" + libraryStatus
                + "|filter=" + libraryFilter.name()
                + "|sort=" + librarySort.key
                + "|grid=" + libraryGridView
                + "|searchVisible=" + librarySearchVisible
                + "|search=" + librarySearchQuery
                + "|focused=" + focusedKey
                + "|focusedFilter=" + focusedFilter
                + "|parent=" + parentKey
                + "|artistMode=" + artistDetailMode.name();
    }

    private void invalidateLibraryContentCache() {
        cachedLibraryTabKey = "";
        libraryRecyclerState = null;
        invalidateLibraryResultCache();
    }

    private void invalidateLibraryResultCache() {
        cachedVisibleTracksKey = "";
        cachedVisibleTracks = new ArrayList<>();
        cachedVisibleGroupsKey = "";
        cachedVisibleGroups = new ArrayList<>();
    }

    private void markLibraryDataChanged() {
        libraryDataVersion++;
        invalidateLibraryContentCache();
    }

    private void markPlaylistDataChanged() {
        playlistDataVersion++;
        invalidateLibraryContentCache();
    }

    private void renderLibraryRecyclerTab() {
        libraryTrackItemViews.clear();
        String key = libraryTabCacheKey();
        boolean sameLibrarySurface = key.equals(cachedLibraryTabKey);
        if (!sameLibrarySurface) {
            libraryRecyclerState = null;
            cachedLibraryTabKey = key;
        }
        List<LibraryListItem> items = buildLibraryRecyclerItems();
        boolean gridMode = libraryGridView && libraryRecyclerItemsCanUseGrid(items);
        if (libraryRecyclerView.getLayoutManager() == null || libraryRecyclerGridMode != gridMode) {
            libraryRecyclerGridMode = gridMode;
            if (gridMode) {
                GridLayoutManager manager = new GridLayoutManager(this, 2);
                manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        if (libraryRecyclerAdapter == null) {
                            return 2;
                        }
                        int type = libraryRecyclerAdapter.getItemViewType(position);
                        return type == LibraryListItem.TYPE_TRACK_CARD || type == LibraryListItem.TYPE_GROUP_CARD ? 1 : 2;
                    }
                });
                libraryRecyclerView.setLayoutManager(manager);
            } else {
                libraryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
        }
        if (libraryRecyclerAdapter == null) {
            libraryRecyclerAdapter = new LibraryRecyclerAdapter();
            libraryRecyclerView.setAdapter(libraryRecyclerAdapter);
        }
        libraryRecyclerAdapter.submitItems(items);
        updateMainContentBottomPadding(mainNavigationInset);
        if (sameLibrarySurface && libraryRecyclerState != null) {
            Parcelable state = libraryRecyclerState;
            libraryRecyclerView.post(() -> {
                if (libraryRecyclerView != null && libraryRecyclerView.getLayoutManager() != null) {
                    libraryRecyclerView.getLayoutManager().onRestoreInstanceState(state);
                }
            });
        }
    }

    private boolean libraryRecyclerItemsCanUseGrid(List<LibraryListItem> items) {
        if (items == null) {
            return false;
        }
        for (LibraryListItem item : items) {
            if (item != null && (item.type == LibraryListItem.TYPE_TRACK_CARD || item.type == LibraryListItem.TYPE_GROUP_CARD)) {
                return true;
            }
        }
        return false;
    }

    private List<LibraryListItem> buildLibraryRecyclerItems() {
        List<LibraryListItem> items = new ArrayList<>();
        if (!hasAudioPermission()) {
            LinearLayout root = screenRoot();
            LinearLayout permission = panel();
            permission.addView(label("오디오 권한 필요"), marginBottom(8));
            permission.addView(muted("Android 미디어 저장소에서 음악 파일을 읽어 앨범과 아티스트로 정리합니다.", 14), marginBottom(14));
            Button request = primaryButton("권한 허용");
            request.setOnClickListener(view -> requestAudioPermission());
            permission.addView(request, matchWrap());
            root.addView(permission, marginBottom(16));
            items.add(LibraryListItem.staticView(root));
            return items;
        }

        if (!libraryLoaded && !libraryLoading) {
            startLibraryRefresh(false);
        }

        if (focusedLibraryGroup != null && isGroupDetailFilter(focusedLibraryGroupFilter)) {
            LibraryGroup group = currentFocusedLibraryGroup();
            if (group != null && (!group.tracks.isEmpty() || focusedLibraryGroupFilter == LibraryFilter.PLAYLIST)) {
                LinearLayout root = screenRoot();
                buildLibraryGroupDetail(root, group);
                items.add(LibraryListItem.staticView(root));
                return items;
            }
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
            focusedParentArtistGroup = null;
        }

        items.add(LibraryListItem.staticView(libraryRecyclerHeader()));
        appendLibraryRecyclerResults(items);
        return items;
    }

    private void appendLibraryRecyclerResults(List<LibraryListItem> items) {
        if (libraryFilter == LibraryFilter.ALL) {
            List<DeviceAudioTrack> visibleTracks = visibleLibraryTracks();
            if (visibleTracks.isEmpty()) {
                items.add(LibraryListItem.staticView(wrapLibraryContent(emptyLibraryView())));
                return;
            }
            for (DeviceAudioTrack track : visibleTracks) {
                items.add(libraryGridView ? LibraryListItem.trackCard(track) : LibraryListItem.trackRow(track));
            }
            return;
        }

        if (libraryFilter == LibraryFilter.PLAYLIST) {
            items.add(LibraryListItem.staticView(wrapLibraryContent(createPlaylistPanel())));
        }

        List<LibraryGroup> visibleGroups = visibleLibraryGroups();
        if (visibleGroups.isEmpty()) {
            items.add(LibraryListItem.staticView(wrapLibraryContent(emptyLibraryView())));
            return;
        }
        for (LibraryGroup group : visibleGroups) {
            items.add(libraryGridView ? LibraryListItem.groupCard(group) : LibraryListItem.groupRow(group));
        }
    }

    private View libraryRecyclerHeader() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(14));
        root.addView(librarySearchToolbar(), marginBottom(8));
        root.addView(libraryFilterBar(), marginBottom(shouldShowLibrarySearchInput() ? 8 : 12));
        if (shouldShowLibrarySearchInput()) {
            root.addView(librarySearchInputRow(), marginBottom(10));
        }
        if (!libraryStatus.trim().isEmpty()) {
            root.addView(libraryViewToolbar(), marginBottom(0));
        }
        return root;
    }

    private View wrapLibraryContent(View content) {
        FrameLayout frame = new FrameLayout(this);
        frame.setPadding(dp(20), 0, dp(20), 0);
        frame.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        return frame;
    }

    private View emptyLibraryView() {
        LinearLayout empty = panel();
        boolean searching = !librarySearchQuery.trim().isEmpty();
        empty.addView(label(searching ? "검색 결과가 없습니다." : "표시할 음악이 없습니다."), marginBottom(8));
        String hint = searching
                ? "다른 검색어를 입력하거나 전체/앨범/아티스트/재생목록 필터를 바꿔보세요."
                : libraryFilter == LibraryFilter.PLAYLIST
                ? "새 재생목록을 만들거나 곡을 길게 눌러 재생목록에 저장하세요."
                : emptyLibraryHint();
        empty.addView(muted(hint, 13), matchWrap());
        return empty;
    }

    private View libraryFilterBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        HorizontalScrollView shelf = new HorizontalScrollView(this);
        shelf.setHorizontalScrollBarEnabled(false);
        shelf.setHorizontalFadingEdgeEnabled(true);
        shelf.setFadingEdgeLength(dp(28));
        shelf.setOverScrollMode(View.OVER_SCROLL_NEVER);
        shelf.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> libraryFilterScrollX = scrollX);
        libraryFilterGestureArea = shelf;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button all = libraryFilterChip("전체", LibraryFilter.ALL);
        Button album = libraryFilterChip("앨범", LibraryFilter.ALBUM);
        Button artist = libraryFilterChip("아티스트", LibraryFilter.ARTIST);
        Button playlist = libraryFilterChip("재생목록", LibraryFilter.PLAYLIST);
        row.addView(all, marginRight(8, LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        row.addView(album, marginRight(8, LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        row.addView(artist, marginRight(8, LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        row.addView(playlist, marginRight(0, LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        shelf.addView(row, matchWrap());
        bar.addView(shelf, new LinearLayout.LayoutParams(0, dp(38), 1f));
        View selectedChip = libraryFilter == LibraryFilter.ALBUM
                ? album
                : libraryFilter == LibraryFilter.ARTIST
                ? artist
                : libraryFilter == LibraryFilter.PLAYLIST
                ? playlist
                : all;
        shelf.post(() -> {
            restoreLibraryFilterScroll(shelf);
            centerLibraryFilterChip(shelf, selectedChip);
        });

        LinearLayout sort = librarySortButton();
        LinearLayout.LayoutParams sortParams = new LinearLayout.LayoutParams(dp(librarySort.buttonWidthDp()), dp(44));
        sortParams.setMargins(dp(4), 0, 0, 0);
        bar.addView(sort, sortParams);
        return bar;
    }

    private void centerLibraryFilterChip(HorizontalScrollView shelf, View chip) {
        if (shelf == null || chip == null || shelf.getChildCount() == 0) {
            return;
        }
        View content = shelf.getChildAt(0);
        int visibleWidth = Math.max(0, shelf.getWidth() - shelf.getPaddingLeft() - shelf.getPaddingRight());
        if (visibleWidth <= 0 || content.getWidth() <= 0 || chip.getWidth() <= 0) {
            return;
        }
        int chipCenter = chip.getLeft() + chip.getWidth() / 2;
        int target = chipCenter - visibleWidth / 2;
        int maxScroll = Math.max(0, content.getWidth() - visibleWidth);
        int scrollX = Math.max(0, Math.min(target, maxScroll));
        if (Math.abs(shelf.getScrollX() - scrollX) <= dp(2)) {
            return;
        }
        shelf.smoothScrollTo(scrollX, 0);
    }

    private void restoreLibraryFilterScroll(HorizontalScrollView shelf) {
        if (shelf == null || shelf.getChildCount() == 0) {
            return;
        }
        View content = shelf.getChildAt(0);
        int visibleWidth = Math.max(0, shelf.getWidth() - shelf.getPaddingLeft() - shelf.getPaddingRight());
        int maxScroll = Math.max(0, content.getWidth() - visibleWidth);
        int restored = Math.max(0, Math.min(libraryFilterScrollX, maxScroll));
        if (restored > 0) {
            shelf.scrollTo(restored, 0);
        }
    }

    private LinearLayout librarySortButton() {
        LinearLayout sort = new LinearLayout(this);
        sort.setOrientation(LinearLayout.HORIZONTAL);
        sort.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        sort.setClickable(true);
        sort.setFocusable(true);
        sort.setBackgroundColor(Color.TRANSPARENT);
        sort.setContentDescription("정렬: " + librarySort.label);
        sort.setOnClickListener(view -> showLibrarySortDialog());

        TextView label = text(librarySort.buttonLabel(), 12, R.color.ytet_text, true);
        label.setSingleLine(false);
        label.setMaxLines(2);
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        label.setIncludeFontPadding(false);
        label.setLineSpacing(0f, 0.92f);
        sort.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("▾", 12, R.color.ytet_text, true);
        arrow.setGravity(Gravity.CENTER);
        arrow.setIncludeFontPadding(false);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(10), LinearLayout.LayoutParams.WRAP_CONTENT);
        arrowParams.setMargins(dp(1), 0, 0, 0);
        sort.addView(arrow, arrowParams);
        return sort;
    }

    private Button libraryFilterChip(String label, LibraryFilter filter) {
        Button chip = compactButton(label);
        boolean selected = libraryFilter == filter;
        chip.setTextColor(selected ? 0xFFFFFFFF : color(R.color.ytet_text));
        chip.setBackground(rounded(selected ? color(R.color.ytet_accent) : color(R.color.ytet_panel_alt), 18));
        chip.setOnClickListener(view -> {
            if (libraryFilter == filter) {
                return;
            }
            if (libraryFilterGestureArea instanceof HorizontalScrollView) {
                libraryFilterScrollX = ((HorizontalScrollView) libraryFilterGestureArea).getScrollX();
            }
            libraryFilter = filter;
            selectedTrack = null;
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
            focusedParentArtistGroup = null;
            artistDetailMode = ArtistDetailMode.ALL;
            renderCurrentTab();
        });
        return chip;
    }

    private void showLibrarySortDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout body = dialogBody("정렬");
        for (LibrarySort sort : LibrarySort.values()) {
            body.addView(librarySourceOption(
                    sort.label,
                    sort.description,
                    librarySort == sort,
                    () -> {
                        setLibrarySort(sort);
                        dialog.dismiss();
                    }
            ), marginBottom(8));
        }
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button close = detailActionButton("닫기");
        close.setOnClickListener(view -> dialog.dismiss());
        actions.addView(close, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.setView(body);
        dialog.show();
        styleDetailDialog(dialog);
    }

    private void setLibrarySort(LibrarySort sort) {
        if (sort == null || sort == librarySort) {
            return;
        }
        librarySort = sort;
        getPreferences().edit().putString(PREF_LIBRARY_SORT, sort.key).apply();
        selectedTrack = null;
        renderCurrentTab();
    }

    private View libraryViewToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        TextView status = muted(libraryStatus, 12);
        status.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(status, new LinearLayout.LayoutParams(0, dp(34), 1f));
        return toolbar;
    }

    private View libraryPullRefreshIndicator() {
        PullRefreshIndicatorView indicator = new PullRefreshIndicatorView(this);
        indicator.setVisibility(View.INVISIBLE);
        indicator.setAlpha(0f);
        libraryPullIndicator = indicator;
        libraryPullIndicatorView = indicator;
        updateLibraryPullIndicator(0f, false);
        return indicator;
    }

    private View librarySearchToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        Button source = toolbarTextButton(librarySourceLabel() + " ▾");
        source.setTextSize(18);
        source.setOnClickListener(view -> showLibrarySourceDialog());
        toolbar.addView(source, marginRight(8, dp(126), dp(48)));

        View spacer = new View(this);
        toolbar.addView(spacer, new LinearLayout.LayoutParams(0, dp(48), 1f));

        ImageButton search = toolbarIconButton(
                R.drawable.ic_search,
                shouldShowLibrarySearchInput() ? "검색 닫기" : "검색",
                shouldShowLibrarySearchInput()
        );
        search.setOnClickListener(view -> {
            if (!shouldShowLibrarySearchInput()) {
                librarySearchVisible = true;
                renderCurrentTab();
                return;
            }
            closeLibrarySearch();
        });
        toolbar.addView(search, marginRight(8, dp(44), dp(44)));

        ImageButton viewToggle = toolbarIconButton(
                libraryGridView ? R.drawable.ic_view_list : R.drawable.ic_grid_view,
                libraryGridView ? "리스트 보기" : "카드 보기",
                false
        );
        viewToggle.setOnClickListener(view -> {
            libraryGridView = !libraryGridView;
            renderCurrentTab();
        });
        toolbar.addView(viewToggle, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return toolbar;
    }

    private View librarySearchInputRow() {
        librarySearchInput = new LibrarySearchEditText(this);
        librarySearchInput.setSingleLine(true);
        librarySearchInput.setText(librarySearchQuery);
        librarySearchInput.setHint("검색");
        librarySearchInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        librarySearchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        styleInput(librarySearchInput);
        librarySearchInput.setMinHeight(dp(56));
        librarySearchInput.setPadding(dp(2), 0, dp(2), 0);
        librarySearchInput.setBackgroundColor(Color.TRANSPARENT);
        librarySearchInput.setSelection(librarySearchInput.getText().length());
        librarySearchInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (view instanceof EditText) {
                ((EditText) view).setCursorVisible(hasFocus);
            }
        });
        librarySearchInput.setOnClickListener(view -> librarySearchInput.setCursorVisible(true));
        librarySearchInput.setOnEditorActionListener((view, actionId, event) -> {
            boolean enterKey = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == EditorInfo.IME_ACTION_SEARCH || enterKey) {
                finishLibrarySearchInput(view);
                return true;
            }
            return false;
        });
        librarySearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                scheduleLibrarySearch(text == null ? "" : text.toString());
            }

            @Override
            public void afterTextChanged(Editable text) {
            }
        });
        if (librarySearchVisible) {
            librarySearchInput.post(this::focusLibrarySearchInput);
        }
        return librarySearchInput;
    }

    private void showLibrarySourceDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout body = dialogBody("내 콘텐츠 보기");
        body.addView(librarySourceOption(
                "보관함",
                "RabbYT/Music에 저장된 음악만 보기",
                !isDeviceFileSource(),
                () -> {
                    setLibrarySource(LIBRARY_SOURCE_COLLECTION);
                    dialog.dismiss();
                }
        ), marginBottom(8));
        body.addView(librarySourceOption(
                "기기 파일",
                "기기에서 음악으로 분류된 파일 전체 보기",
                isDeviceFileSource(),
                () -> {
                    setLibrarySource(LIBRARY_SOURCE_DEVICE);
                    dialog.dismiss();
                }
        ), marginBottom(16));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button close = detailActionButton("닫기");
        close.setOnClickListener(view -> dialog.dismiss());
        actions.addView(close, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.setView(body);
        dialog.show();
        styleDetailDialog(dialog);
    }

    private View librarySourceOption(String title, String subtitle, boolean selected, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(rounded(selected ? selectedTrackBackgroundColor() : color(R.color.ytet_panel), 12));
        row.setOnClickListener(view -> action.run());

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(title, 15, R.color.ytet_text, true), marginBottom(3));
        copy.addView(muted(subtitle, 12), matchWrap());
        row.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView state = text(selected ? "선택됨" : "", 12, R.color.ytet_muted, true);
        state.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        row.addView(state, new LinearLayout.LayoutParams(dp(58), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void setLibrarySource(String source) {
        String nextSource = LIBRARY_SOURCE_DEVICE.equals(source)
                ? LIBRARY_SOURCE_DEVICE
                : LIBRARY_SOURCE_COLLECTION;
        if (nextSource.equals(librarySource)) {
            return;
        }
        librarySource = nextSource;
        getPreferences().edit().putString(PREF_LIBRARY_SOURCE, librarySource).apply();
        selectedTrack = null;
        focusedLibraryGroup = null;
        focusedLibraryGroupFilter = null;
        focusedParentArtistGroup = null;
        artistDetailMode = ArtistDetailMode.ALL;
        libraryLoaded = false;
        startLibraryRefresh(true);
    }

    private String librarySourceLabel() {
        return isDeviceFileSource() ? "기기 파일" : "보관함";
    }

    private boolean isDeviceFileSource() {
        return LIBRARY_SOURCE_DEVICE.equals(librarySource);
    }

    private void scheduleLibrarySearch(String query) {
        pendingLibrarySearchQuery = query == null ? "" : query;
        mainHandler.removeCallbacks(librarySearchCommit);
        mainHandler.postDelayed(librarySearchCommit, LIBRARY_SEARCH_DEBOUNCE_MS);
    }

    private void commitLibrarySearch(String query) {
        String nextQuery = query == null ? "" : query;
        if (nextQuery.equals(librarySearchQuery)) {
            return;
        }
        librarySearchQuery = nextQuery;
        librarySearchVisible = true;
        selectedTrack = null;
        if (currentTab != Tab.LIBRARY) {
            return;
        }
        refreshLibraryResultsOnly();
    }

    private void flushLibrarySearchInput() {
        if (librarySearchInput != null) {
            pendingLibrarySearchQuery = librarySearchInput.getText().toString();
        }
        mainHandler.removeCallbacks(librarySearchCommit);
        String nextQuery = pendingLibrarySearchQuery == null ? "" : pendingLibrarySearchQuery;
        if (!nextQuery.equals(librarySearchQuery)) {
            librarySearchQuery = nextQuery;
            librarySearchVisible = true;
            selectedTrack = null;
        }
    }

    private void finishLibrarySearchInput(View view) {
        String query = view instanceof TextView ? ((TextView) view).getText().toString() : pendingLibrarySearchQuery;
        pendingLibrarySearchQuery = query == null ? "" : query;
        mainHandler.removeCallbacks(librarySearchCommit);
        commitLibrarySearch(pendingLibrarySearchQuery);
        hideKeyboard(view);
        view.clearFocus();
        if (view instanceof EditText) {
            ((EditText) view).setCursorVisible(false);
        }
    }

    private void closeLibrarySearch() {
        mainHandler.removeCallbacks(librarySearchCommit);
        hideKeyboard(librarySearchInput);
        librarySearchVisible = false;
        librarySearchQuery = "";
        pendingLibrarySearchQuery = "";
        librarySearchInput = null;
        selectedTrack = null;
        renderCurrentTab();
    }

    private void refreshLibraryResultsOnly() {
        if (currentTab != Tab.LIBRARY || libraryRecyclerView == null || libraryRecyclerAdapter == null) {
            renderCurrentTab();
            return;
        }
        libraryTrackItemViews.clear();
        List<LibraryListItem> items = new ArrayList<>();
        LibraryListItem header = libraryRecyclerAdapter.firstItem();
        if (header != null && header.type == LibraryListItem.TYPE_STATIC) {
            items.add(header);
        } else {
            items.add(LibraryListItem.staticView(libraryRecyclerHeader()));
        }
        appendLibraryRecyclerResults(items);
        libraryRecyclerAdapter.submitItems(items);
        cachedLibraryTabKey = libraryTabCacheKey();
    }

    private void focusLibrarySearchInput() {
        if (librarySearchInput == null) {
            return;
        }
        librarySearchInput.requestFocus();
        librarySearchInput.setCursorVisible(true);
        librarySearchInput.setSelection(librarySearchInput.getText().length());
        showKeyboard(librarySearchInput);
    }

    private void hideKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private boolean shouldShowLibrarySearchInput() {
        return librarySearchVisible || !librarySearchQuery.trim().isEmpty();
    }

    private List<DeviceAudioTrack> visibleLibraryTracks() {
        String cacheKey = visibleTracksCacheKey();
        if (cacheKey.equals(cachedVisibleTracksKey)) {
            return cachedVisibleTracks;
        }
        String query = librarySearchQuery == null ? "" : librarySearchQuery.trim().toLowerCase(Locale.ROOT);
        List<DeviceAudioTrack> result;
        if (query.isEmpty()) {
            result = sortedLibraryTracks(libraryTracks, libraryFilter);
        } else {
            List<DeviceAudioTrack> matches = new ArrayList<>();
            for (DeviceAudioTrack track : libraryTracks) {
                if (trackMatchesQuery(track, query)) {
                    matches.add(track);
                }
            }
            result = sortedLibraryTracks(matches, libraryFilter);
        }
        cachedVisibleTracksKey = cacheKey;
        cachedVisibleTracks = result;
        return result;
    }

    private List<LibraryGroup> visibleLibraryGroups() {
        String cacheKey = visibleGroupsCacheKey();
        if (cacheKey.equals(cachedVisibleGroupsKey)) {
            return cachedVisibleGroups;
        }
        List<LibraryGroup> result;
        if (libraryFilter == LibraryFilter.PLAYLIST) {
            result = visiblePlaylistGroups();
        } else {
            result = libraryGroupsForTracks(visibleLibraryTracks(), libraryFilter);
        }
        cachedVisibleGroupsKey = cacheKey;
        cachedVisibleGroups = result;
        return result;
    }

    private String visibleTracksCacheKey() {
        return "tracks|source=" + librarySource
                + "|data=" + libraryDataVersion
                + "|filter=" + libraryFilter.name()
                + "|sort=" + librarySort.key
                + "|query=" + librarySearchQuery;
    }

    private String visibleGroupsCacheKey() {
        return "groups|source=" + librarySource
                + "|data=" + libraryDataVersion
                + "|playlists=" + playlistDataVersion
                + "|filter=" + libraryFilter.name()
                + "|sort=" + librarySort.key
                + "|query=" + librarySearchQuery;
    }

    private List<LibraryGroup> visiblePlaylistGroups() {
        String query = librarySearchQuery == null ? "" : librarySearchQuery.trim().toLowerCase(Locale.ROOT);
        List<LibraryGroup> groups = new ArrayList<>();
        for (UserPlaylists.Playlist playlist : UserPlaylists.list(this)) {
            LibraryGroup group = playlistGroup(playlist);
            if (!query.isEmpty() && !playlistMatchesQuery(group, query)) {
                continue;
            }
            groups.add(group);
        }
        groups.sort(this::comparePlaylistGroups);
        return groups;
    }

    private boolean playlistMatchesQuery(LibraryGroup group, String query) {
        if (group == null) {
            return false;
        }
        String cleanQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (cleanQuery.isEmpty()) {
            return true;
        }
        if (group.title.toLowerCase(Locale.ROOT).contains(cleanQuery)
                || group.subtitle.toLowerCase(Locale.ROOT).contains(cleanQuery)) {
            return true;
        }
        for (DeviceAudioTrack track : group.tracks) {
            if (trackMatchesQuery(track, cleanQuery)) {
                return true;
            }
        }
        return false;
    }

    private LibraryGroup playlistGroup(UserPlaylists.Playlist playlist) {
        List<DeviceAudioTrack> tracks = tracksForIds(playlist == null ? new ArrayList<>() : playlist.trackIds());
        String title = playlist == null ? "재생목록" : playlist.title();
        String subtitle = tracks.size() + "곡 · " + totalDurationLabel(tracks);
        return new LibraryGroup(
                PLAYLIST_GROUP_PREFIX + (playlist == null ? "" : playlist.id()),
                title,
                subtitle,
                bestCoverTrack(tracks),
                tracks
        );
    }

    private List<DeviceAudioTrack> tracksForIds(List<Long> ids) {
        Map<Long, DeviceAudioTrack> byId = tracksById(libraryTracks);
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        if (ids == null) {
            return tracks;
        }
        for (Long id : ids) {
            DeviceAudioTrack track = id == null ? null : byId.get(id);
            if (track != null) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    private Map<Long, DeviceAudioTrack> tracksById(List<DeviceAudioTrack> tracks) {
        Map<Long, DeviceAudioTrack> byId = new LinkedHashMap<>();
        if (tracks == null) {
            return byId;
        }
        for (DeviceAudioTrack track : tracks) {
            if (track != null) {
                byId.put(track.id(), track);
            }
        }
        return byId;
    }

    private List<Long> trackIdsForTracks(List<DeviceAudioTrack> tracks) {
        List<Long> ids = new ArrayList<>();
        if (tracks == null) {
            return ids;
        }
        for (DeviceAudioTrack track : tracks) {
            if (track != null) {
                ids.add(track.id());
            }
        }
        return ids;
    }

    private List<LibraryGroup> libraryGroupsForTracks(List<DeviceAudioTrack> tracks, LibraryFilter filter) {
        Map<String, List<DeviceAudioTrack>> grouped = new LinkedHashMap<>();
        Map<String, String> titles = new LinkedHashMap<>();
        for (DeviceAudioTrack track : tracks) {
            String key = libraryGroupKey(track, filter);
            titles.putIfAbsent(key, libraryGroupTitle(track, filter));
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(track);
        }

        List<LibraryGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<DeviceAudioTrack>> entry : grouped.entrySet()) {
            groups.add(libraryGroup(
                    entry.getKey(),
                    libraryGroupTitleForTracks(entry.getValue(), filter, titles.get(entry.getKey())),
                    entry.getValue(),
                    filter
            ));
        }
        groups.sort((first, second) -> compareLibraryGroups(first, second, filter));
        return groups;
    }

    private String libraryGroupKey(DeviceAudioTrack track, LibraryFilter filter) {
        if (filter == LibraryFilter.ARTIST) {
            return "artist:" + groupKeyPart(MusicLibrary.representativeArtist(track));
        }
        if (isCollectionPlaylistFolder(track.folder())) {
            return "album-folder:" + groupKeyPart(track.folder());
        }
        String key = "album:" + groupKeyPart(track.album()) + "|folder:" + groupKeyPart(track.folder());
        if (isUnknownAlbum(track.album())) {
            key += "|artist:" + groupKeyPart(MusicLibrary.representativeArtist(track));
        }
        return key;
    }

    private String libraryGroupTitle(DeviceAudioTrack track, LibraryFilter filter) {
        return filter == LibraryFilter.ALBUM ? track.album() : MusicLibrary.representativeArtist(track);
    }

    private String libraryGroupTitleForTracks(List<DeviceAudioTrack> tracks, LibraryFilter filter, String fallback) {
        if (filter != LibraryFilter.ALBUM) {
            return fallback;
        }
        String commonAlbum = commonAlbumTitle(tracks);
        if (commonAlbum != null) {
            return commonAlbum;
        }
        String folder = sharedFolderLabel(tracks);
        if (isCollectionPlaylistFolder(folder)) {
            return folder;
        }
        return fallback;
    }

    private String groupKeyPart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isUnknownAlbum(String album) {
        return album == null || album.trim().isEmpty() || "앨범 정보 없음".equals(album.trim());
    }

    private boolean isCollectionPlaylistFolder(String folder) {
        return !isDeviceFileSource()
                && folder != null
                && !folder.trim().isEmpty()
                && !DefaultMediaPaths.MUSIC_FOLDER.equalsIgnoreCase(folder.trim());
    }

    private String commonAlbumTitle(List<DeviceAudioTrack> tracks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        for (DeviceAudioTrack track : tracks) {
            if (isUnknownAlbum(track.album())) {
                continue;
            }
            String key = groupKeyPart(track.album());
            if (key.isEmpty()) {
                continue;
            }
            counts.put(key, counts.getOrDefault(key, 0) + 1);
            labels.putIfAbsent(key, track.album());
        }
        String bestKey = "";
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestKey = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        int minimum = tracks.size() <= 1 ? 1 : Math.max(2, (tracks.size() + 1) / 2);
        return bestCount >= minimum ? labels.get(bestKey) : null;
    }

    private String sharedFolderLabel(List<DeviceAudioTrack> tracks) {
        String folder = "";
        for (DeviceAudioTrack track : tracks) {
            if (folder.isEmpty()) {
                folder = track.folder();
                continue;
            }
            if (!folder.equals(track.folder())) {
                return "";
            }
        }
        return folder;
    }

    private LibraryGroup libraryGroup(String key, String title, List<DeviceAudioTrack> tracks, LibraryFilter filter) {
        List<DeviceAudioTrack> orderedTracks = orderedGroupTracks(tracks, filter);
        String subtitle;
        if (filter == LibraryFilter.PLAYLIST) {
            subtitle = orderedTracks.size() + "곡 · " + totalDurationLabel(orderedTracks);
        } else if (filter == LibraryFilter.ALBUM) {
            subtitle = albumArtistSummary(orderedTracks) + " · " + orderedTracks.size() + "곡 · " + totalDurationLabel(orderedTracks);
        } else {
            subtitle = distinctAlbumCount(orderedTracks) + "개 앨범 · " + orderedTracks.size() + "곡 · " + totalDurationLabel(orderedTracks);
        }
        return new LibraryGroup(key, title, subtitle, bestCoverTrack(orderedTracks), orderedTracks);
    }

    private List<DeviceAudioTrack> orderedGroupTracks(List<DeviceAudioTrack> tracks, LibraryFilter filter) {
        List<DeviceAudioTrack> ordered = tracks == null ? new ArrayList<>() : new ArrayList<>(tracks);
        if (filter == LibraryFilter.ALBUM) {
            ordered.sort(this::compareAlbumTrackOrder);
        } else {
            ordered.sort((first, second) -> compareLibraryTracks(first, second, filter));
        }
        return ordered;
    }

    private List<DeviceAudioTrack> sortedLibraryTracks(List<DeviceAudioTrack> tracks, LibraryFilter filter) {
        List<DeviceAudioTrack> sorted = tracks == null ? new ArrayList<>() : new ArrayList<>(tracks);
        sorted.sort((first, second) -> compareLibraryTracks(first, second, filter));
        return sorted;
    }

    private int compareLibraryTracks(DeviceAudioTrack first, DeviceAudioTrack second, LibraryFilter filter) {
        switch (librarySort) {
            case OLDEST:
                return compareByDate(first, second, true);
            case NAME:
                return compareTrackName(first, second);
            case MOST_PLAYED:
                return compareByPlayCount(first, second, false);
            case LEAST_PLAYED:
                return compareByPlayCount(first, second, true);
            case NEWEST:
            default:
                return compareByDate(first, second, false);
        }
    }

    private int compareLibraryGroups(LibraryGroup first, LibraryGroup second, LibraryFilter filter) {
        switch (librarySort) {
            case OLDEST:
                int oldest = Long.compare(groupOldestTimestamp(first), groupOldestTimestamp(second));
                return oldest != 0 ? oldest : compareGroupName(first, second);
            case NAME:
                return compareGroupName(first, second);
            case MOST_PLAYED:
                int mostPlayed = Long.compare(groupPlayCount(second), groupPlayCount(first));
                return mostPlayed != 0 ? mostPlayed : compareGroupName(first, second);
            case LEAST_PLAYED:
                int leastPlayed = Long.compare(groupPlayCount(first), groupPlayCount(second));
                return leastPlayed != 0 ? leastPlayed : compareGroupName(first, second);
            case NEWEST:
            default:
                int newest = Long.compare(groupNewestTimestamp(second), groupNewestTimestamp(first));
                return newest != 0 ? newest : compareGroupName(first, second);
        }
    }

    private int comparePlaylistGroups(LibraryGroup first, LibraryGroup second) {
        switch (librarySort) {
            case OLDEST:
                int oldest = Long.compare(playlistUpdatedAt(first), playlistUpdatedAt(second));
                return oldest != 0 ? oldest : compareGroupName(first, second);
            case NAME:
                return compareGroupName(first, second);
            case MOST_PLAYED:
                int mostPlayed = Long.compare(groupPlayCount(second), groupPlayCount(first));
                return mostPlayed != 0 ? mostPlayed : compareGroupName(first, second);
            case LEAST_PLAYED:
                int leastPlayed = Long.compare(groupPlayCount(first), groupPlayCount(second));
                return leastPlayed != 0 ? leastPlayed : compareGroupName(first, second);
            case NEWEST:
            default:
                int newest = Long.compare(playlistUpdatedAt(second), playlistUpdatedAt(first));
                return newest != 0 ? newest : compareGroupName(first, second);
        }
    }

    private long playlistUpdatedAt(LibraryGroup group) {
        UserPlaylists.Playlist playlist = UserPlaylists.find(this, playlistIdFromGroupKey(group == null ? "" : group.key));
        return playlist == null ? 0L : playlist.updatedAtMs();
    }

    private int compareByDate(DeviceAudioTrack first, DeviceAudioTrack second, boolean oldestFirst) {
        int result = oldestFirst
                ? Long.compare(trackSortTimestamp(first), trackSortTimestamp(second))
                : Long.compare(trackSortTimestamp(second), trackSortTimestamp(first));
        return result != 0 ? result : compareTrackName(first, second);
    }

    private int compareByPlayCount(DeviceAudioTrack first, DeviceAudioTrack second, boolean leastFirst) {
        int result = leastFirst
                ? Integer.compare(trackPlayCount(first), trackPlayCount(second))
                : Integer.compare(trackPlayCount(second), trackPlayCount(first));
        return result != 0 ? result : compareTrackName(first, second);
    }

    private int compareAlbumTrackOrder(DeviceAudioTrack first, DeviceAudioTrack second) {
        int firstNumber = albumTrackSortNumber(first);
        int secondNumber = albumTrackSortNumber(second);
        if (firstNumber != secondNumber) {
            return Integer.compare(firstNumber, secondNumber);
        }
        return compareAlbumStoredOrder(first, second);
    }

    private int albumTrackSortNumber(DeviceAudioTrack track) {
        if (track == null) {
            return Integer.MAX_VALUE;
        }
        if (track.trackNumber() > 0) {
            return track.trackNumber();
        }
        int filePrefix = leadingTrackNumber(track.displayName());
        if (filePrefix > 0) {
            return filePrefix;
        }
        int titlePrefix = leadingTrackNumber(track.title());
        return titlePrefix > 0 ? titlePrefix : Integer.MAX_VALUE;
    }

    private int leadingTrackNumber(String value) {
        if (value == null) {
            return 0;
        }
        String text = value.trim();
        int index = 0;
        int number = 0;
        while (index < text.length() && Character.isDigit(text.charAt(index)) && index < 3) {
            number = number * 10 + (text.charAt(index) - '0');
            index++;
        }
        if (index == 0 || number <= 0 || number > 999) {
            return 0;
        }
        if (index < text.length() && Character.isLetterOrDigit(text.charAt(index))) {
            return 0;
        }
        return number;
    }

    private int compareAlbumStoredOrder(DeviceAudioTrack first, DeviceAudioTrack second) {
        int stored = Long.compare(trackSortTimestamp(first), trackSortTimestamp(second));
        if (stored != 0) {
            return stored;
        }
        return Long.compare(first == null ? 0L : first.id(), second == null ? 0L : second.id());
    }

    private long trackSortTimestamp(DeviceAudioTrack track) {
        if (track == null) {
            return 0L;
        }
        return track.dateAddedMs() > 0L ? track.dateAddedMs() : track.id();
    }

    private int trackPlayCount(DeviceAudioTrack track) {
        return track == null ? 0 : PlaybackStats.playCount(this, track.id());
    }

    private int compareTrackName(DeviceAudioTrack first, DeviceAudioTrack second) {
        int title = compareTextValues(first == null ? "" : first.title(), second == null ? "" : second.title());
        if (title != 0) {
            return title;
        }
        int artist = compareTextValues(first == null ? "" : first.artist(), second == null ? "" : second.artist());
        if (artist != 0) {
            return artist;
        }
        return compareTextValues(first == null ? "" : first.album(), second == null ? "" : second.album());
    }

    private int compareGroupName(LibraryGroup first, LibraryGroup second) {
        return compareTextValues(first == null ? "" : first.title, second == null ? "" : second.title);
    }

    private int compareTextValues(String first, String second) {
        String left = first == null ? "" : first.trim();
        String right = second == null ? "" : second.trim();
        return left.compareToIgnoreCase(right);
    }

    private long groupNewestTimestamp(LibraryGroup group) {
        if (group == null) {
            return 0L;
        }
        long newest = 0L;
        for (DeviceAudioTrack track : group.tracks) {
            newest = Math.max(newest, trackSortTimestamp(track));
        }
        return newest;
    }

    private long groupOldestTimestamp(LibraryGroup group) {
        if (group == null) {
            return 0L;
        }
        long oldest = Long.MAX_VALUE;
        for (DeviceAudioTrack track : group.tracks) {
            long timestamp = trackSortTimestamp(track);
            if (timestamp > 0L) {
                oldest = Math.min(oldest, timestamp);
            }
        }
        return oldest == Long.MAX_VALUE ? 0L : oldest;
    }

    private long groupPlayCount(LibraryGroup group) {
        if (group == null) {
            return 0L;
        }
        long count = 0L;
        for (DeviceAudioTrack track : group.tracks) {
            count += trackPlayCount(track);
        }
        return count;
    }

    private LibraryGroup currentFocusedLibraryGroup() {
        if (focusedLibraryGroup == null || focusedLibraryGroupFilter == null) {
            return null;
        }
        if (focusedLibraryGroupFilter == LibraryFilter.PLAYLIST) {
            UserPlaylists.Playlist playlist = UserPlaylists.find(this, playlistIdFromGroupKey(focusedLibraryGroup.key));
            return playlist == null ? null : playlistGroup(playlist);
        }
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        for (DeviceAudioTrack track : libraryTracks) {
            if (focusedLibraryGroup.key.equals(libraryGroupKey(track, focusedLibraryGroupFilter))) {
                tracks.add(track);
            }
        }
        if (tracks.isEmpty()) {
            return libraryLoading ? focusedLibraryGroup : null;
        }
        return libraryGroup(focusedLibraryGroup.key, focusedLibraryGroup.title, tracks, focusedLibraryGroupFilter);
    }

    private String albumArtistSummary(List<DeviceAudioTrack> tracks) {
        String artist = "";
        for (DeviceAudioTrack track : tracks) {
            String representative = MusicLibrary.representativeArtist(track);
            if (!representative.isEmpty() && !"알 수 없는 아티스트".equals(representative)) {
                return representative;
            }
            if (artist.isEmpty()) {
                artist = representative;
            }
        }
        return artist.isEmpty() ? "알 수 없는 아티스트" : artist;
    }

    private int distinctAlbumCount(List<DeviceAudioTrack> tracks) {
        Map<String, Boolean> albums = new LinkedHashMap<>();
        for (DeviceAudioTrack track : tracks) {
            albums.put(track.album(), true);
        }
        return albums.size();
    }

    private String totalDurationLabel(List<DeviceAudioTrack> tracks) {
        long total = 0L;
        for (DeviceAudioTrack track : tracks) {
            total += track.durationMs();
        }
        return MusicLibrary.formatDuration(total);
    }

    private DeviceAudioTrack bestCoverTrack(List<DeviceAudioTrack> tracks) {
        DeviceAudioTrack fallback = tracks.isEmpty() ? null : tracks.get(0);
        for (DeviceAudioTrack track : tracks) {
            if (track.albumArtUri() != null && !track.albumArtUri().trim().isEmpty()) {
                return track;
            }
        }
        return fallback;
    }

    private View libraryGroupCard(LibraryGroup group) {
        return libraryGroupCard(group, () -> handleLibraryGroupClick(group));
    }

    private View libraryGroupCard(LibraryGroup group, Runnable action) {
        LinearLayout card = panel();
        card.setPadding(dp(10), dp(10), dp(10), dp(12));
        card.setOnClickListener(view -> action.run());
        card.setOnLongClickListener(view -> {
            showLibraryGroupQuickActions(group);
            return true;
        });

        SquareFrameLayout coverFrame = new SquareFrameLayout(this);
        coverFrame.addView(groupCoverView(group), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        card.addView(coverFrame, marginBottom(10));

        TextView title = text(group.title, 14, R.color.ytet_text, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(title, marginBottom(4));

        TextView subtitle = muted(group.subtitle, 12);
        subtitle.setMaxLines(2);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(subtitle, matchWrap());
        return card;
    }

    private View libraryGroupRow(LibraryGroup group) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackground(rounded(color(R.color.ytet_panel), 8));
        row.setOnClickListener(view -> handleLibraryGroupClick(group));
        row.setOnLongClickListener(view -> {
            showLibraryGroupQuickActions(group);
            return true;
        });

        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(58), dp(58));
        coverParams.setMargins(0, 0, dp(12), 0);
        row.addView(groupCoverView(group), coverParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(group.title, 15, R.color.ytet_text, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(title, marginBottom(4));
        TextView subtitle = muted(group.subtitle, 12);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(subtitle, matchWrap());
        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void handleLibraryGroupClick(LibraryGroup group) {
        if (isGroupDetailFilter(libraryFilter)) {
            focusedLibraryGroup = group;
            focusedLibraryGroupFilter = libraryFilter;
            focusedParentArtistGroup = null;
            if (libraryFilter == LibraryFilter.ARTIST) {
                artistDetailMode = distinctAlbumCount(group.tracks) > 1
                        ? ArtistDetailMode.ALBUMS
                        : ArtistDetailMode.ALL;
            }
            selectedTrack = null;
            renderCurrentTab();
            return;
        }
        playLibraryGroup(group);
    }

    private boolean handleLibraryBackNavigation() {
        if (currentTab != Tab.LIBRARY) {
            return false;
        }
        if (focusedLibraryGroup != null) {
            if (focusedLibraryGroupFilter == LibraryFilter.ALBUM && focusedParentArtistGroup != null) {
                focusedLibraryGroup = focusedParentArtistGroup;
                focusedLibraryGroupFilter = LibraryFilter.ARTIST;
                focusedParentArtistGroup = null;
            } else {
                focusedLibraryGroup = null;
                focusedLibraryGroupFilter = null;
                focusedParentArtistGroup = null;
            }
            selectedTrack = null;
            renderCurrentTab();
            return true;
        }
        if (librarySearchVisible || !librarySearchQuery.trim().isEmpty()) {
            closeLibrarySearch();
            return true;
        }
        return false;
    }

    private void buildLibraryGroupDetail(LinearLayout root, LibraryGroup group) {
        LibraryFilter detailFilter = focusedLibraryGroupFilter == null ? libraryFilter : focusedLibraryGroupFilter;
        boolean artistDetail = detailFilter == LibraryFilter.ARTIST;

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        String backDescription = detailFilter == LibraryFilter.PLAYLIST
                ? "재생목록 목록"
                : artistDetail ? "아티스트 목록" : "앨범 목록";
        ImageButton back = toolbarIconButton(R.drawable.ic_arrow_back, backDescription, false);
        back.setOnClickListener(view -> handleLibraryBackNavigation());
        top.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, dp(44), 1f));
        if (artistDetail) {
            Button viewMode = toolbarTextButton(artistDetailModeLabel() + " ▾");
            viewMode.setOnClickListener(view -> showArtistDetailModeDialog());
            top.addView(viewMode, marginRight(2, dp(82), dp(44)));
        }
        ImageButton search = toolbarIconButton(R.drawable.ic_search, "검색", false);
        search.setOnClickListener(view -> {
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
            focusedParentArtistGroup = null;
            librarySearchVisible = true;
            renderCurrentTab();
        });
        top.addView(search, new LinearLayout.LayoutParams(dp(44), dp(44)));
        root.addView(top, marginBottom(24));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(142), dp(142));
        coverParams.setMargins(0, 0, dp(18), 0);
        hero.addView(groupCoverView(group), coverParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(group.title, 27, R.color.ytet_text, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title, marginBottom(8));
        TextView subtitle = muted(group.subtitle, 15);
        subtitle.setMaxLines(2);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(subtitle, marginBottom(12));
        ImageButton more = playerIconButton(R.drawable.ic_more_vert, "상세정보", false, true);
        more.setPadding(dp(10), dp(10), dp(10), dp(10));
        more.setOnClickListener(view -> showLibraryGroupDetails(group, detailFilter));
        copy.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));
        hero.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(hero, marginBottom(24));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        View play = actionButtonWithIcon("재생", R.drawable.ic_play_arrow, true);
        play.setOnClickListener(view -> playLibraryGroup(group, 0, false));
        actions.addView(play, weightedControlParams(1, 10));
        View shuffle = actionButtonWithIcon("셔플", R.drawable.ic_shuffle, false);
        shuffle.setOnClickListener(view -> playLibraryGroup(group, 0, true));
        actions.addView(shuffle, weightedControlParams(1, 0));
        root.addView(actions, marginBottom(22));

        if (artistDetail && artistDetailMode == ArtistDetailMode.ALBUMS) {
            addArtistAlbumGroupGrid(root, group);
            return;
        }

        if (group.tracks.isEmpty()) {
            root.addView(muted(detailFilter == LibraryFilter.PLAYLIST
                    ? "수록곡이 없습니다. 곡을 길게 눌러 이 재생목록에 저장할 수 있습니다."
                    : "수록곡이 없습니다.", 13), matchWrap());
            return;
        }

        for (int index = 0; index < group.tracks.size(); index++) {
            root.addView(libraryGroupTrackRow(group, group.tracks.get(index), index), marginBottom(6));
        }
    }

    private boolean isGroupDetailFilter(LibraryFilter filter) {
        return filter == LibraryFilter.ALBUM || filter == LibraryFilter.ARTIST || filter == LibraryFilter.PLAYLIST;
    }

    private String artistDetailModeLabel() {
        return artistDetailMode == ArtistDetailMode.ALBUMS ? "앨범" : "전체";
    }

    private void showArtistDetailModeDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout body = dialogBody("아티스트 보기");
        body.addView(librarySourceOption(
                "전체",
                "곡 단위로 보고 선택한 곡부터 재생",
                artistDetailMode == ArtistDetailMode.ALL,
                () -> {
                    artistDetailMode = ArtistDetailMode.ALL;
                    dialog.dismiss();
                    renderCurrentTab();
                }
        ), marginBottom(8));
        body.addView(librarySourceOption(
                "앨범",
                "앨범 단위 카드로 보고 앨범 상세로 이동",
                artistDetailMode == ArtistDetailMode.ALBUMS,
                () -> {
                    artistDetailMode = ArtistDetailMode.ALBUMS;
                    dialog.dismiss();
                    renderCurrentTab();
                }
        ), marginBottom(16));
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button close = detailActionButton("닫기");
        close.setOnClickListener(view -> dialog.dismiss());
        actions.addView(close, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.setView(body);
        dialog.show();
        styleDetailDialog(dialog);
    }

    private void addArtistAlbumGroupGrid(LinearLayout root, LibraryGroup artistGroup) {
        List<LibraryGroup> albums = libraryGroupsForTracks(artistGroup.tracks, LibraryFilter.ALBUM);
        if (albums.isEmpty()) {
            root.addView(muted("표시할 앨범이 없습니다.", 13), matchWrap());
            return;
        }
        for (int index = 0; index < albums.size(); index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LibraryGroup first = albums.get(index);
            row.addView(libraryGroupCard(first, () -> openArtistAlbumDetail(artistGroup, first)), cardColumnParams(8));
            if (index + 1 < albums.size()) {
                LibraryGroup second = albums.get(index + 1);
                row.addView(libraryGroupCard(second, () -> openArtistAlbumDetail(artistGroup, second)), cardColumnParams(0));
            } else {
                row.addView(new View(this), cardColumnParams(0));
            }
            root.addView(row, marginBottom(10));
        }
    }

    private void openArtistAlbumDetail(LibraryGroup artistGroup, LibraryGroup albumGroup) {
        focusedParentArtistGroup = artistGroup;
        focusedLibraryGroup = albumGroup;
        focusedLibraryGroupFilter = LibraryFilter.ALBUM;
        selectedTrack = null;
        renderCurrentTab();
    }

    private View libraryGroupTrackRow(LibraryGroup group, DeviceAudioTrack track, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(9), dp(6), dp(9));
        row.setBackground(rounded(track.id() == playbackTrackId ? selectedTrackBackgroundColor() : Color.TRANSPARENT, 8));
        row.setOnClickListener(view -> {
            selectLibraryTrack(track);
            playLibraryGroup(group, index, false);
        });
        row.setOnLongClickListener(view -> {
            selectLibraryTrack(track);
            showTrackQuickActions(track);
            return true;
        });

        TextView number = muted(Integer.toString(index + 1), 13);
        number.setGravity(Gravity.CENTER);
        row.addView(number, new LinearLayout.LayoutParams(dp(34), LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(track.title(), 15, R.color.ytet_text, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(title, marginBottom(4));
        TextView meta = muted(track.artist() + " · " + MusicLibrary.formatDuration(track.durationMs()), 12);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(meta, matchWrap());
        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton more = trackMoreButton(track);
        row.addView(more, new LinearLayout.LayoutParams(dp(42), dp(42)));
        return row;
    }

    private void showLibraryGroupDetails(LibraryGroup group, LibraryFilter filter) {
        LinearLayout body = dialogBody("상세정보");
        if (filter == LibraryFilter.PLAYLIST) {
            body.addView(trackDetailItem("재생목록", group.title), marginBottom(10));
        } else if (filter == LibraryFilter.ARTIST) {
            body.addView(trackDetailItem("아티스트", group.title), marginBottom(10));
            body.addView(trackDetailItem("앨범", distinctAlbumCount(group.tracks) + "개"), marginBottom(10));
        } else {
            body.addView(trackDetailItem("앨범", group.title), marginBottom(10));
            body.addView(trackDetailItem("아티스트", albumArtistSummary(group.tracks)), marginBottom(10));
        }
        body.addView(trackDetailItem("수록곡", group.tracks.size() + "곡"), marginBottom(10));
        body.addView(trackDetailItem("전체 재생시간", totalDurationLabel(group.tracks)), marginBottom(14));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        if (filter == LibraryFilter.ALBUM || filter == LibraryFilter.PLAYLIST) {
            Button edit = detailActionButton("수정");
            edit.setOnClickListener(view -> {
                dialog.dismiss();
                if (filter == LibraryFilter.PLAYLIST) {
                    showEditPlaylistDialog(group);
                } else {
                    showEditLibraryGroupDialog(group);
                }
            });
            actions.addView(edit, fixedButtonParams(76, 38, 8));
        }
        Button delete = detailActionButton("삭제");
        delete.setOnClickListener(view -> {
            dialog.dismiss();
            confirmDeleteLibraryGroup(group, filter);
        });
        Button close = detailActionButton("닫기");
        close.setOnClickListener(view -> dialog.dismiss());
        actions.addView(delete, fixedButtonParams(76, 38, 8));
        actions.addView(close, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
    }

    private void showEditLibraryGroupDialog(LibraryGroup group) {
        if (group == null || group.tracks.isEmpty()) {
            return;
        }
        LinearLayout body = dialogBody("앨범명 수정");
        String label = "앨범명";
        EditText input = metadataEditField(label, group.title);
        body.addView(metadataEditLabel(label), marginBottom(4));
        body.addView(input, marginBottom(10));
        body.addView(muted("이 앨범 그룹 안의 곡 표시 앨범명을 함께 변경합니다. 폴더명은 변경하지 않습니다.", 11), marginBottom(14));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button cancel = detailActionButton("취소");
        cancel.setOnClickListener(view -> dialog.dismiss());
        Button save = detailActionButton("저장");
        save.setOnClickListener(view -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) {
                toast(label + "을 입력해 주세요.");
                return;
            }
            applyAlbumGroupName(group, value);
            dialog.dismiss();
        });
        actions.addView(cancel, fixedButtonParams(76, 38, 8));
        actions.addView(save, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
    }

    private void applyAlbumGroupName(LibraryGroup group, String album) {
        List<DeviceAudioTrack> edited = new ArrayList<>();
        for (DeviceAudioTrack track : group.tracks) {
            edited.add(TrackMetadataOverrides.saveAlbum(this, track, album));
        }
        applyEditedTracks(edited);
        toast(group.tracks.size() + "곡의 앨범명을 수정했습니다.");
    }

    private void confirmDeleteLibraryGroup(LibraryGroup group, LibraryFilter filter) {
        if (group == null) {
            return;
        }
        if (filter == LibraryFilter.PLAYLIST) {
            confirmDeletePlaylist(group);
            return;
        }
        if (group.tracks.isEmpty()) {
            return;
        }
        String target = filter == LibraryFilter.ARTIST ? "아티스트" : "앨범";
        LinearLayout body = dialogBody(target + " 삭제");
        body.addView(muted(
                group.title + "의 " + group.tracks.size() + "개 파일을 기기에서 삭제합니다. 이 작업은 되돌릴 수 없습니다.",
                13
        ), marginBottom(14));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button cancel = detailActionButton("취소");
        cancel.setOnClickListener(view -> dialog.dismiss());
        Button delete = detailActionButton("삭제");
        delete.setOnClickListener(view -> {
            dialog.dismiss();
            deleteTracks(new ArrayList<>(group.tracks));
        });
        actions.addView(cancel, fixedButtonParams(76, 38, 8));
        actions.addView(delete, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
    }

    private View createPlaylistPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(12), dp(10), dp(12), dp(10));
        panel.setBackground(rounded(color(R.color.ytet_panel), 10));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text("새 재생목록", 15, R.color.ytet_text, true), marginBottom(2));
        copy.addView(muted("곡이나 앨범을 직접 묶어 관리", 12), matchWrap());
        panel.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button create = detailActionButton("생성");
        create.setOnClickListener(view -> showCreatePlaylistDialog(new ArrayList<>()));
        panel.addView(create, new LinearLayout.LayoutParams(dp(76), dp(38)));
        return panel;
    }

    private void showCreatePlaylistDialog(List<DeviceAudioTrack> tracksToAdd) {
        LinearLayout body = dialogBody("새 재생목록");
        EditText input = metadataEditField("재생목록 이름", defaultPlaylistTitle(tracksToAdd));
        body.addView(metadataEditLabel("재생목록 이름"), marginBottom(4));
        body.addView(input, marginBottom(14));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button cancel = detailActionButton("취소");
        cancel.setOnClickListener(view -> dialog.dismiss());
        Button save = detailActionButton("생성");
        save.setOnClickListener(view -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                toast("재생목록 이름을 입력해 주세요.");
                return;
            }
            UserPlaylists.Playlist playlist = UserPlaylists.create(this, title, trackIdsForTracks(tracksToAdd));
            dialog.dismiss();
            toast(playlist.title() + " 재생목록을 만들었습니다.");
            markPlaylistDataChanged();
            renderLibraryDependentTabs();
        });
        actions.addView(cancel, fixedButtonParams(76, 38, 8));
        actions.addView(save, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
        input.requestFocus();
        input.setSelection(input.getText().length());
        input.post(() -> showKeyboard(input));
    }

    private String defaultPlaylistTitle(List<DeviceAudioTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return "새 재생목록";
        }
        if (tracks.size() == 1) {
            return tracks.get(0).title();
        }
        return "새 재생목록";
    }

    private void showEditPlaylistDialog(LibraryGroup group) {
        String playlistId = playlistIdFromGroupKey(group == null ? "" : group.key);
        UserPlaylists.Playlist playlist = UserPlaylists.find(this, playlistId);
        if (playlist == null) {
            toast("재생목록을 찾을 수 없습니다.");
            return;
        }
        List<DeviceAudioTrack> editableTracks = new ArrayList<>(tracksForIds(playlist.trackIds()));
        LinearLayout body = dialogBody("재생목록 수정");
        EditText nameInput = metadataEditField("재생목록 이름", playlist.title());
        body.addView(metadataEditLabel("재생목록 이름"), marginBottom(4));
        body.addView(nameInput, marginBottom(12));

        TextView trackLabel = metadataEditLabel("수록곡");
        body.addView(trackLabel, marginBottom(6));
        ScrollView scroll = new ScrollView(this);
        LinearLayout trackList = new LinearLayout(this);
        trackList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(trackList, matchWrap());
        body.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(280)
        ));

        Runnable[] renderTracks = new Runnable[1];
        renderTracks[0] = () -> renderPlaylistEditTracks(trackList, editableTracks, renderTracks[0]);
        renderTracks[0].run();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button cancel = detailActionButton("취소");
        cancel.setOnClickListener(view -> dialog.dismiss());
        Button save = detailActionButton("저장");
        save.setOnClickListener(view -> {
            String title = nameInput.getText().toString().trim();
            if (title.isEmpty()) {
                toast("재생목록 이름을 입력해 주세요.");
                return;
            }
            UserPlaylists.rename(this, playlist.id(), title);
            UserPlaylists.updateTracks(this, playlist.id(), trackIdsForTracks(editableTracks));
            UserPlaylists.Playlist updated = UserPlaylists.find(this, playlist.id());
            if (updated != null) {
                focusedLibraryGroup = playlistGroup(updated);
                focusedLibraryGroupFilter = LibraryFilter.PLAYLIST;
            }
            dialog.dismiss();
            toast("재생목록을 수정했습니다.");
            markPlaylistDataChanged();
            renderLibraryDependentTabs();
        });
        actions.addView(cancel, fixedButtonParams(76, 38, 8));
        actions.addView(save, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
    }

    private void renderPlaylistEditTracks(LinearLayout root, List<DeviceAudioTrack> tracks, Runnable rerender) {
        root.removeAllViews();
        if (tracks.isEmpty()) {
            root.addView(muted("수록곡이 없습니다.", 13), matchWrap());
            return;
        }
        for (DeviceAudioTrack track : new ArrayList<>(tracks)) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(8), dp(8), dp(8));
            row.setBackground(rounded(color(R.color.ytet_panel), 8));
            row.addView(trackCoverView(track), marginRight(10, dp(42), dp(42)));

            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            TextView title = text(track.title(), 14, R.color.ytet_text, true);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            copy.addView(title, marginBottom(3));
            TextView meta = muted(track.artist() + " · " + MusicLibrary.formatDuration(track.durationMs()), 12);
            meta.setSingleLine(true);
            meta.setEllipsize(TextUtils.TruncateAt.END);
            copy.addView(meta, matchWrap());
            row.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button remove = detailActionButton("제거");
            remove.setOnClickListener(view -> {
                tracks.remove(track);
                rerender.run();
            });
            row.addView(remove, new LinearLayout.LayoutParams(dp(68), dp(36)));
            root.addView(row, marginBottom(8));
        }
    }

    private void confirmDeletePlaylist(LibraryGroup group) {
        String playlistId = playlistIdFromGroupKey(group == null ? "" : group.key);
        if (playlistId.isEmpty()) {
            return;
        }
        LinearLayout body = dialogBody("재생목록 삭제");
        body.addView(muted(group.title + " 재생목록만 삭제합니다. 기기에 저장된 음악 파일은 삭제하지 않습니다.", 13), marginBottom(14));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button cancel = detailActionButton("취소");
        cancel.setOnClickListener(view -> dialog.dismiss());
        Button delete = detailActionButton("삭제");
        delete.setOnClickListener(view -> {
            UserPlaylists.delete(this, playlistId);
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
            focusedParentArtistGroup = null;
            dialog.dismiss();
            toast("재생목록을 삭제했습니다.");
            markPlaylistDataChanged();
            renderLibraryDependentTabs();
        });
        actions.addView(cancel, fixedButtonParams(76, 38, 8));
        actions.addView(delete, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
    }

    private String playlistIdFromGroupKey(String key) {
        String value = key == null ? "" : key.trim();
        return value.startsWith(PLAYLIST_GROUP_PREFIX)
                ? value.substring(PLAYLIST_GROUP_PREFIX.length())
                : "";
    }

    private View groupCoverView(LibraryGroup group) {
        if (group.coverTrack != null) {
            return trackCoverView(group.coverTrack);
        }
        TextView placeholder = text("YT", 22, android.R.color.white, true);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackground(rounded(color(R.color.ytet_accent_dark), 8));
        return placeholder;
    }

    private void playLibraryGroup(LibraryGroup group) {
        playLibraryGroup(group, 0, true);
    }

    private void playLibraryGroup(LibraryGroup group, int startIndex, boolean shuffle) {
        if (group == null || group.tracks.isEmpty()) {
            toast("재생할 음악이 없습니다.");
            return;
        }
        int safeIndex = Math.max(0, Math.min(startIndex, group.tracks.size() - 1));
        LibraryFilter groupFilter = focusedLibraryGroupFilter == null ? libraryFilter : focusedLibraryGroupFilter;
        String category;
        if (groupFilter == LibraryFilter.PLAYLIST) {
            category = "재생목록";
        } else if (groupFilter == LibraryFilter.ALBUM) {
            category = "앨범";
        } else {
            category = "아티스트";
        }
        MusicStation station = new MusicStation(
                category + "-" + Integer.toHexString(group.key.hashCode()) + (shuffle ? "-shuffle" : "-ordered"),
                group.title,
                category,
                group.subtitle,
                group.tracks.size() + "곡 재생",
                shuffle ? MusicStation.MixType.ALL : MusicStation.MixType.TRACK,
                "",
                color(R.color.ytet_accent)
        );
        activeStation = station;
        activeQueuePreview = new ArrayList<>(group.tracks);
        applyPreviewTrackTheme(group.tracks.get(safeIndex));
        playbackHasQueue = true;
        playbackPlaying = false;
        playbackPreparing = true;
        playbackWillPlay = true;
        playbackError = false;
        playbackTitle = station.title();
        playbackMeta = station.subtitle();
        setStreamingStatus("준비 중: " + station.title());
        updateNowPlayingBar();
        startPlayback(PlaybackService.playQueueIntent(this, station, group.tracks, safeIndex));
    }

    private boolean trackMatchesQuery(DeviceAudioTrack track, String query) {
        return searchIndex(track).contains(query);
    }

    private String searchIndex(DeviceAudioTrack track) {
        if (track == null) {
            return "";
        }
        String cached = librarySearchIndex.get(track.id());
        if (cached != null) {
            return cached;
        }
        StringBuilder builder = new StringBuilder();
        appendSearchText(builder, track.title());
        appendSearchText(builder, track.artist());
        appendSearchText(builder, track.representativeArtist());
        appendSearchText(builder, track.album());
        appendSearchText(builder, track.folder());
        appendSearchText(builder, track.displayName());
        String value = builder.toString().toLowerCase(Locale.ROOT);
        librarySearchIndex.put(track.id(), value);
        return value;
    }

    private void appendSearchText(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(value);
    }

    private View trackCard(DeviceAudioTrack track) {
        LinearLayout card = panel();
        card.setPadding(dp(10), dp(10), dp(10), dp(12));
        applyTrackSelectionBackground(card, track);
        card.setOnClickListener(view -> {
            selectLibraryTrack(track);
            playVisibleLibraryTrack(track);
        });
        card.setOnLongClickListener(view -> {
            selectLibraryTrack(track);
            showTrackQuickActions(track);
            return true;
        });
        registerLibraryTrackView(track, card);

        SquareFrameLayout coverFrame = new SquareFrameLayout(this);
        coverFrame.addView(trackCoverView(track), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        ImageButton more = trackMoreButton(track);
        FrameLayout.LayoutParams moreParams = new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.TOP | Gravity.END);
        moreParams.setMargins(0, dp(6), dp(6), 0);
        coverFrame.addView(more, moreParams);
        card.addView(coverFrame, marginBottom(10));

        TextView title = text(track.title(), 14, R.color.ytet_text, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(title, marginBottom(4));

        TextView artist = muted(track.artist(), 12);
        artist.setSingleLine(true);
        artist.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(artist, marginBottom(2));
        card.addView(muted(MusicLibrary.formatDuration(track.durationMs()), 12), matchWrap());
        return card;
    }

    private View trackRow(DeviceAudioTrack track) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        applyTrackSelectionBackground(row, track);
        row.setOnClickListener(view -> {
            selectLibraryTrack(track);
            playVisibleLibraryTrack(track);
        });
        row.setOnLongClickListener(view -> {
            selectLibraryTrack(track);
            showTrackQuickActions(track);
            return true;
        });
        registerLibraryTrackView(track, row);

        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(58), dp(58));
        coverParams.setMargins(0, 0, dp(12), 0);
        row.addView(trackCoverView(track), coverParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(track.title(), 15, R.color.ytet_text, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(title, marginBottom(4));
        TextView meta = muted(track.artist() + " · " + MusicLibrary.formatDuration(track.durationMs()), 12);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(meta, matchWrap());
        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ImageButton more = trackMoreButton(track);
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        moreParams.setMargins(dp(8), 0, 0, 0);
        row.addView(more, moreParams);
        return row;
    }

    private ImageButton trackMoreButton(DeviceAudioTrack track) {
        ImageButton more = playerIconButton(R.drawable.ic_more_vert, "상세정보", false, true);
        more.setPadding(dp(9), dp(9), dp(9), dp(9));
        more.setOnClickListener(view -> showTrackDetails(track));
        return more;
    }

    private void selectLibraryTrack(DeviceAudioTrack track) {
        long previousSelectedId = selectedTrack == null ? Long.MIN_VALUE : selectedTrack.id();
        selectedTrack = track;
        refreshTrackSelection(previousSelectedId);
        refreshTrackSelection(track == null ? Long.MIN_VALUE : track.id());
    }

    private void registerLibraryTrackView(DeviceAudioTrack track, View view) {
        if (track == null || view == null) {
            return;
        }
        libraryTrackItemViews.put(track.id(), view);
    }

    private void refreshTrackSelection(long trackId) {
        if (trackId == Long.MIN_VALUE) {
            return;
        }
        View view = libraryTrackItemViews.get(trackId);
        if (view != null) {
            applyTrackSelectionBackground(view, findLibraryTrackById(trackId));
        }
    }

    private DeviceAudioTrack findLibraryTrackById(long trackId) {
        if (selectedTrack != null && selectedTrack.id() == trackId) {
            return selectedTrack;
        }
        for (DeviceAudioTrack track : libraryTracks) {
            if (track.id() == trackId) {
                return track;
            }
        }
        return null;
    }

    private void applyTrackSelectionBackground(View view, DeviceAudioTrack track) {
        view.setBackground(rounded(isSelectedTrack(track)
                ? selectedTrackBackgroundColor()
                : color(R.color.ytet_panel), 8));
    }

    private boolean isSelectedTrack(DeviceAudioTrack track) {
        return selectedTrack != null && track != null && selectedTrack.id() == track.id();
    }

    private int selectedTrackBackgroundColor() {
        return 0xFF30333B;
    }

    private View trackCoverView(DeviceAudioTrack track) {
        if (track != null && track.albumArtUri() != null && !track.albumArtUri().trim().isEmpty()) {
            if (isHttpUrl(track.albumArtUri())) {
                return onlineImageView(track.albumArtUri(), track.title(), 58, 8);
            }
            ImageView image = new ImageView(this);
            image.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageURI(Uri.parse(track.albumArtUri()));
            return image;
        }
        TextView placeholder = text(trackInitials(track), 22, android.R.color.white, true);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackground(rounded(color(R.color.ytet_accent_dark), 8));
        return placeholder;
    }

    private View onlineImageView(String imageUrl, String fallbackText, int sizeDp, int radiusDp) {
        return onlineImageView(imageUrl, fallbackText, sizeDp, radiusDp, 640, ImageView.ScaleType.CENTER_CROP);
    }

    private View onlineImageView(
            String imageUrl,
            String fallbackText,
            int sizeDp,
            int radiusDp,
            int maxBitmapDimension,
            ImageView.ScaleType scaleType
    ) {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(rounded(color(R.color.ytet_panel_alt), radiusDp));
        TextView placeholder = text(initialsForText(fallbackText), Math.max(11, Math.min(22, sizeDp / 3)), android.R.color.white, true);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackground(rounded(color(R.color.ytet_accent_dark), radiusDp));
        frame.addView(placeholder, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        if (!isHttpUrl(imageUrl)) {
            return frame;
        }
        ImageView image = new ImageView(this);
        image.setScaleType(scaleType);
        image.setAlpha(0f);
        image.setTag(imageUrl.trim());
        frame.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        loadRemoteImage(image, imageUrl.trim(), maxBitmapDimension);
        return frame;
    }

    private void loadRemoteImage(ImageView target, String imageUrl) {
        loadRemoteImage(target, imageUrl, 640);
    }

    private void loadRemoteImage(ImageView target, String imageUrl, int maxBitmapDimension) {
        imageExecutor.execute(() -> {
            Bitmap bitmap = loadRemoteBitmap(imageUrl, maxBitmapDimension);
            runOnUiThread(() -> {
                if (bitmap == null || target == null || !TextUtils.equals(String.valueOf(target.getTag()), imageUrl)) {
                    return;
                }
                target.setImageBitmap(bitmap);
                target.animate().alpha(1f).setDuration(180L).start();
            });
        });
    }

    private Bitmap loadRemoteBitmap(String imageUrl, int maxBitmapDimension) {
        for (String candidateUrl : remoteImageCandidates(imageUrl)) {
            Bitmap bitmap = loadRemoteBitmapCandidate(candidateUrl, maxBitmapDimension);
            if (bitmap != null) {
                return bitmap;
            }
        }
        return null;
    }

    private Bitmap loadRemoteBitmapCandidate(String imageUrl, int maxBitmapDimension) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setConnectTimeout(4500);
            connection.setReadTimeout(6500);
            connection.setInstanceFollowRedirects(true);
            try (InputStream stream = connection.getInputStream()) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                if (bitmap == null) {
                    return null;
                }
                int dimensionLimit = Math.max(320, maxBitmapDimension);
                int maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
                if (maxDimension <= dimensionLimit) {
                    return bitmap;
                }
                float ratio = (float) dimensionLimit / maxDimension;
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

    private List<String> remoteImageCandidates(String imageUrl) {
        List<String> candidates = new ArrayList<>();
        String clean = imageUrl == null ? "" : imageUrl.trim();
        if (clean.isEmpty()) {
            return candidates;
        }
        if (clean.contains("ytimg.com/")) {
            int queryIndex = clean.indexOf('?');
            String withoutQuery = queryIndex >= 0 ? clean.substring(0, queryIndex) : clean;
            int slashIndex = withoutQuery.lastIndexOf('/');
            if (slashIndex > 0 && slashIndex < withoutQuery.length() - 1) {
                String base = withoutQuery.substring(0, slashIndex + 1);
                String extension = withoutQuery.endsWith(".webp") || withoutQuery.contains("/vi_webp/")
                        ? ".webp"
                        : ".jpg";
                addUnique(candidates, base + "maxresdefault" + extension);
                addUnique(candidates, base + "sddefault" + extension);
                addUnique(candidates, base + "hq720" + extension);
                addUnique(candidates, withoutQuery);
            }
        }
        addUnique(candidates, clean);
        return candidates;
    }

    private void addUnique(List<String> values, String value) {
        if (value == null || value.trim().isEmpty() || values.contains(value)) {
            return;
        }
        values.add(value.trim());
    }

    private String initialsForText(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            return "YT";
        }
        return clean.substring(0, Math.min(2, clean.length())).toUpperCase(Locale.ROOT);
    }

    private boolean isHttpUrl(String value) {
        String clean = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return clean.startsWith("http://") || clean.startsWith("https://");
    }

    private String trackInitials(DeviceAudioTrack track) {
        String title = track == null ? "" : track.title().trim();
        if (title.isEmpty()) {
            return "YT";
        }
        return title.substring(0, Math.min(2, title.length())).toUpperCase(Locale.ROOT);
    }

    private void showTrackQuickActions(DeviceAudioTrack track) {
        if (track == null) {
            return;
        }
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        tracks.add(track);
        showQuickActions(track.title(), track.artist() + " · " + MusicLibrary.formatDuration(track.durationMs()), track, tracks);
    }

    private void showLibraryGroupQuickActions(LibraryGroup group) {
        if (group == null || group.tracks.isEmpty()) {
            return;
        }
        showQuickActions(group.title, group.subtitle, group.coverTrack, group.tracks);
    }

    private void showQuickActions(String titleText, String metaText, DeviceAudioTrack coverTrack, List<DeviceAudioTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(18), dp(20), dp(22));
        body.setBackground(roundedStroke(0xF0181818, 0x22FFFFFF, 22, 1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(46), dp(46));
        coverParams.setMargins(0, 0, dp(14), 0);
        header.addView(trackCoverView(coverTrack), coverParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(titleText, 16, R.color.ytet_text, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(title, marginBottom(3));
        TextView meta = muted(metaText, 13);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(meta, matchWrap());
        header.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(header, marginBottom(16));

        View divider = new View(this);
        divider.setBackgroundColor(0x22FFFFFF);
        body.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));

        body.addView(trackQuickActionRow(R.drawable.ic_queue_play_next, "다음 곡으로 재생", () -> {
            dialog.dismiss();
            playTracksNext(tracks);
        }), matchWrap());
        body.addView(trackQuickActionRow(R.drawable.ic_queue_add, "현재 재생목록에 추가", () -> {
            dialog.dismiss();
            addTracksToCurrentQueue(tracks);
        }), matchWrap());
        body.addView(trackQuickActionRow(R.drawable.ic_playlist_save, "재생목록에 저장", () -> {
            dialog.dismiss();
            showPlaylistPickerDialog(tracks);
        }), matchWrap());

        dialog.setContentView(body);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(0.56f);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private View trackQuickActionRow(int iconRes, String label, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(16), dp(8), dp(8));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(view -> action.run());

        ImageView icon = new ImageView(this);
        Drawable drawable = getDrawable(iconRes);
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(color(R.color.ytet_text));
            icon.setImageDrawable(drawable);
        }
        row.addView(icon, marginRight(18, dp(32), dp(32)));
        row.addView(text(label, 16, R.color.ytet_text, false), matchWrap());
        return row;
    }

    private void playTracksNext(List<DeviceAudioTrack> tracks) {
        List<DeviceAudioTrack> queueTracks = cleanTrackList(tracks);
        if (queueTracks.isEmpty()) {
            toast("추가할 음악이 없습니다.");
            return;
        }
        updateLocalQueuePreview(queueTracks, true);
        startPlayback(PlaybackService.queueEditIntent(this, PlaybackService.ACTION_PLAY_NEXT, queueTracks));
        toast(queueTracks.size() == 1 ? "다음 곡으로 추가했습니다." : queueTracks.size() + "곡을 다음 곡으로 추가했습니다.");
    }

    private void addTracksToCurrentQueue(List<DeviceAudioTrack> tracks) {
        List<DeviceAudioTrack> queueTracks = cleanTrackList(tracks);
        if (queueTracks.isEmpty()) {
            toast("추가할 음악이 없습니다.");
            return;
        }
        updateLocalQueuePreview(queueTracks, false);
        startPlayback(PlaybackService.queueEditIntent(this, PlaybackService.ACTION_ADD_TO_QUEUE, queueTracks));
        toast(queueTracks.size() == 1 ? "현재 재생목록에 추가했습니다." : queueTracks.size() + "곡을 현재 재생목록에 추가했습니다.");
    }

    private List<DeviceAudioTrack> cleanTrackList(List<DeviceAudioTrack> tracks) {
        List<DeviceAudioTrack> clean = new ArrayList<>();
        if (tracks == null) {
            return clean;
        }
        for (DeviceAudioTrack track : tracks) {
            if (track != null) {
                clean.add(track);
            }
        }
        return clean;
    }

    private void updateLocalQueuePreview(List<DeviceAudioTrack> tracks, boolean playNext) {
        if (tracks == null || tracks.isEmpty()) {
            return;
        }
        List<DeviceAudioTrack> preview = new ArrayList<>(activeQueuePreview);
        if (preview.isEmpty()) {
            preview.addAll(tracks);
            activeQueuePreview = preview;
            playbackQueueIndex = 0;
            playbackQueueSize = preview.size();
            playbackHasQueue = true;
            activeStation = new MusicStation(
                    "manual-queue",
                    "현재 재생목록",
                    "재생목록",
                    preview.size() + "곡",
                    "사용자가 추가한 음악",
                    MusicStation.MixType.TRACK,
                    "",
                    color(R.color.ytet_accent)
            );
            applyPreviewTrackTheme(preview.get(0));
            playbackTitle = preview.get(0).title();
            playbackMeta = preview.get(0).artist() + " · " + preview.get(0).album();
            playbackArtist = preview.get(0).artist();
            playbackAlbum = preview.get(0).album();
            playbackAlbumArtUri = preview.get(0).albumArtUri();
            playbackPreparing = true;
            playbackWillPlay = true;
            updateNowPlayingBar();
            updateExpandedPlayer();
            return;
        }
        int insertIndex = playNext
                ? Math.max(0, Math.min(preview.size(), playbackQueueIndex + 1))
                : preview.size();
        preview.addAll(insertIndex, tracks);
        activeQueuePreview = preview;
        playbackQueueSize = preview.size();
        updateExpandedPlayer();
        updateQueueDialog();
    }

    private void showPlaylistPickerDialog(List<DeviceAudioTrack> tracksToAdd) {
        List<DeviceAudioTrack> tracks = cleanTrackList(tracksToAdd);
        if (tracks.isEmpty()) {
            toast("저장할 음악이 없습니다.");
            return;
        }
        List<UserPlaylists.Playlist> playlists = UserPlaylists.list(this);
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout body = dialogBody("재생목록에 저장");
        body.addView(muted(tracks.size() == 1
                ? tracks.get(0).title()
                : tracks.size() + "곡을 저장합니다.", 13), marginBottom(12));
        body.addView(playlistPickerRow(
                "새 재생목록에 추가",
                "새로 만들고 선택한 음악을 바로 저장",
                () -> {
                    dialog.dismiss();
                    showCreatePlaylistDialog(tracks);
                }
        ), marginBottom(8));

        if (playlists.isEmpty()) {
            body.addView(muted("아직 저장된 재생목록이 없습니다.", 13), marginBottom(12));
        } else {
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            for (UserPlaylists.Playlist playlist : playlists) {
                LibraryGroup group = playlistGroup(playlist);
                list.addView(playlistPickerRow(
                        playlist.title(),
                        group.subtitle,
                        () -> {
                            dialog.dismiss();
                            saveTracksToPlaylist(playlist, tracks);
                        }
                ), marginBottom(8));
            }
            scroll.addView(list, matchWrap());
            body.addView(scroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(300)
            ));
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button close = detailActionButton("닫기");
        close.setOnClickListener(view -> dialog.dismiss());
        actions.addView(close, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.setView(body);
        dialog.show();
        styleDetailDialog(dialog);
    }

    private View playlistPickerRow(String title, String subtitle, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(rounded(color(R.color.ytet_panel), 10));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(view -> action.run());

        ImageView icon = new ImageView(this);
        Drawable drawable = getDrawable(R.drawable.ic_queue_music);
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(color(R.color.ytet_text));
            icon.setImageDrawable(drawable);
        }
        row.addView(icon, marginRight(12, dp(34), dp(34)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 15, R.color.ytet_text, true);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(titleView, marginBottom(3));
        TextView subtitleView = muted(subtitle, 12);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(subtitleView, matchWrap());
        row.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void saveTracksToPlaylist(UserPlaylists.Playlist playlist, List<DeviceAudioTrack> tracks) {
        if (playlist == null) {
            return;
        }
        UserPlaylists.Playlist updated = UserPlaylists.addTracks(this, playlist.id(), trackIdsForTracks(tracks));
        if (updated == null) {
            toast("재생목록을 찾을 수 없습니다.");
            return;
        }
        toast(updated.title() + "에 저장했습니다.");
        markPlaylistDataChanged();
        renderLibraryDependentTabs();
    }

    private void showTrackDetails(DeviceAudioTrack track) {
        selectLibraryTrack(track);
        LinearLayout body = dialogBody("상세정보");
        body.addView(trackDetailItem("제목", track.title()), marginBottom(10));
        body.addView(trackDetailItem("아티스트", track.artist()), marginBottom(10));
        body.addView(trackDetailItem("앨범", track.album()), marginBottom(10));
        body.addView(trackDetailItem("재생시간", MusicLibrary.formatDuration(track.durationMs())), marginBottom(10));
        body.addView(trackDetailItem("폴더", track.folder()), marginBottom(10));
        body.addView(trackDetailItem("파일명", track.displayName()), marginBottom(10));
        body.addView(trackDetailItem("용량", MusicLibrary.formatBytes(track.sizeBytes())), marginBottom(14));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button share = detailActionButton("공유");
        share.setOnClickListener(view -> {
            selectedTrack = track;
            dialog.dismiss();
            shareSelectedTrack();
        });
        Button edit = detailActionButton("수정");
        edit.setOnClickListener(view -> {
            selectedTrack = track;
            dialog.dismiss();
            showEditTrackMetadataDialog(track);
        });
        Button delete = detailActionButton("삭제");
        delete.setOnClickListener(view -> {
            selectedTrack = track;
            dialog.dismiss();
            deleteSelectedTrack();
        });
        actions.addView(share, fixedButtonParams(76, 38, 8));
        actions.addView(edit, fixedButtonParams(76, 38, 8));
        actions.addView(delete, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
    }

    private void showEditTrackMetadataDialog(DeviceAudioTrack track) {
        LinearLayout body = dialogBody("정보 수정");
        EditText titleInput = metadataEditField("제목", track.title());
        EditText artistInput = metadataEditField("아티스트", track.artist());
        EditText albumInput = metadataEditField("앨범", track.album());
        body.addView(metadataEditLabel("제목"), marginBottom(4));
        body.addView(titleInput, marginBottom(10));
        body.addView(metadataEditLabel("아티스트"), marginBottom(4));
        body.addView(artistInput, marginBottom(10));
        body.addView(metadataEditLabel("앨범"), marginBottom(4));
        body.addView(albumInput, marginBottom(14));
        body.addView(muted("앱 안의 표시 정보를 수정합니다. 실제 파일명과 파일 내부 태그는 변경하지 않습니다.", 11), marginBottom(14));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button cancel = detailActionButton("취소");
        cancel.setOnClickListener(view -> dialog.dismiss());
        Button save = detailActionButton("저장");
        save.setOnClickListener(view -> {
            DeviceAudioTrack edited = TrackMetadataOverrides.save(
                    this,
                    track,
                    titleInput.getText().toString(),
                    artistInput.getText().toString(),
                    albumInput.getText().toString()
            );
            applyEditedTrack(edited);
            dialog.dismiss();
            toast("표시 정보를 수정했습니다.");
        });
        actions.addView(cancel, fixedButtonParams(76, 38, 8));
        actions.addView(save, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
    }

    private TextView metadataEditLabel(String label) {
        TextView view = muted(label, 12);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private EditText metadataEditField(String hint, String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setTextColor(color(R.color.ytet_text));
        input.setHintTextColor(color(R.color.ytet_muted));
        input.setTextSize(15);
        input.setMinHeight(dp(50));
        input.setSelectAllOnFocus(false);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(roundedStroke(0x22000000, 0x22FFFFFF, 8, 1));
        return input;
    }

    private void applyEditedTrack(DeviceAudioTrack edited) {
        if (edited == null) {
            return;
        }
        selectedTrack = edited;
        libraryTracks = replaceEditedTrack(libraryTracks, edited);
        homeTracks = replaceEditedTrack(homeTracks, edited);
        activeQueuePreview = replaceEditedTrack(activeQueuePreview, edited);
        librarySearchIndex.remove(edited.id());
        markLibraryDataChanged();
        if (playbackTrackId == edited.id()) {
            playbackTitle = edited.title();
            playbackArtist = edited.artist();
            playbackAlbum = edited.album();
            playbackMeta = edited.artist() + " · " + edited.album();
            playbackAlbumArtUri = edited.albumArtUri();
        }
        renderLibraryDependentTabs();
        updateNowPlayingBar();
        updateExpandedPlayer();
        updateQueueDialog();
    }

    private void applyEditedTracks(List<DeviceAudioTrack> editedTracks) {
        if (editedTracks == null || editedTracks.isEmpty()) {
            return;
        }
        Map<Long, DeviceAudioTrack> edits = new HashMap<>();
        for (DeviceAudioTrack track : editedTracks) {
            if (track != null) {
                edits.put(track.id(), track);
                librarySearchIndex.remove(track.id());
            }
        }
        if (edits.isEmpty()) {
            return;
        }
        libraryTracks = replaceEditedTracks(libraryTracks, edits);
        homeTracks = replaceEditedTracks(homeTracks, edits);
        activeQueuePreview = replaceEditedTracks(activeQueuePreview, edits);
        markLibraryDataChanged();
        DeviceAudioTrack current = edits.get(playbackTrackId);
        if (current != null) {
            playbackTitle = current.title();
            playbackArtist = current.artist();
            playbackAlbum = current.album();
            playbackMeta = current.artist() + " · " + current.album();
            playbackAlbumArtUri = current.albumArtUri();
        }
        selectedTrack = null;
        focusedLibraryGroup = null;
        focusedLibraryGroupFilter = null;
        focusedParentArtistGroup = null;
        renderLibraryDependentTabs();
        updateNowPlayingBar();
        updateExpandedPlayer();
        updateQueueDialog();
    }

    private List<DeviceAudioTrack> replaceEditedTrack(List<DeviceAudioTrack> tracks, DeviceAudioTrack edited) {
        List<DeviceAudioTrack> replaced = new ArrayList<>();
        if (tracks == null) {
            return replaced;
        }
        for (DeviceAudioTrack track : tracks) {
            replaced.add(track != null && track.id() == edited.id() ? edited : track);
        }
        return replaced;
    }

    private List<DeviceAudioTrack> replaceEditedTracks(List<DeviceAudioTrack> tracks, Map<Long, DeviceAudioTrack> edits) {
        List<DeviceAudioTrack> replaced = new ArrayList<>();
        if (tracks == null) {
            return replaced;
        }
        for (DeviceAudioTrack track : tracks) {
            DeviceAudioTrack edited = track == null ? null : edits.get(track.id());
            replaced.add(edited == null ? track : edited);
        }
        return replaced;
    }

    private LinearLayout dialogBody(String title) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(22), dp(18), dp(22), dp(18));
        body.addView(text(title, 18, R.color.ytet_text, true), marginBottom(16));
        return body;
    }

    private void styleDetailDialog(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(roundedStroke(0xEE101010, 0x33FFFFFF, 18, 1));
    }

    private View trackDetailItem(String title, String value) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.addView(muted(title, 11), marginBottom(2));
        TextView content = text(value == null || value.trim().isEmpty() ? "-" : value, 13, R.color.ytet_text, false);
        content.setMaxLines(3);
        content.setEllipsize(TextUtils.TruncateAt.END);
        item.addView(content, matchWrap());
        return item;
    }

    private View buildExtractorTab() {
        LinearLayout root = screenRoot();

        addTopVisualAlignmentSpacer(root);
        root.addView(label("YouTube URL"), marginBottom(8));
        urlInput = new UrlEditText(this);
        urlInput.setSingleLine(true);
        urlInput.setText(extractorUrl);
        urlInput.setHint("https://youtu.be/...");
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        styleUrlInput(urlInput);
        urlInput.setSelection(urlInput.getText().length());
        urlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                extractorUrl = text == null ? "" : text.toString();
            }

            @Override
            public void afterTextChanged(Editable text) {
            }
        });
        root.addView(urlInput, controlParams(58, 18));

        root.addView(label("모드"), marginBottom(8));
        mediaGroup = new RadioGroup(this);
        mediaGroup.setOrientation(RadioGroup.HORIZONTAL);
        audioRadio = new RadioButton(this);
        audioRadio.setId(View.generateViewId());
        audioRadio.setText("음원");
        audioRadio.setTextColor(color(R.color.ytet_text));
        videoRadio = new RadioButton(this);
        videoRadio.setId(View.generateViewId());
        videoRadio.setText("영상");
        videoRadio.setTextColor(color(R.color.ytet_text));
        mediaGroup.addView(audioRadio, radioParams());
        mediaGroup.addView(videoRadio, radioParams());
        mediaGroup.check(extractorMediaType == MediaType.VIDEO ? videoRadio.getId() : audioRadio.getId());
        mediaGroup.setOnCheckedChangeListener((group, checkedId) -> {
            extractorMediaType = selectedMediaType();
            extractorOption = extractorMediaType == MediaType.VIDEO
                    ? VideoQuality.BEST.value()
                    : AudioFormat.M4A.value();
            updateModeOptions();
            updateFolderLabel();
        });
        root.addView(mediaGroup, marginBottom(16));

        root.addView(label("포맷 / 품질"), marginBottom(8));
        optionSpinner = new Spinner(this);
        optionSpinner.setBackgroundColor(color(R.color.ytet_panel_alt));
        optionSpinner.setPadding(dp(4), 0, dp(4), 0);
        root.addView(optionSpinner, controlParams(56, 14));

        playlistCheck = new CheckBox(this);
        playlistCheck.setText("전체 플레이리스트 추출");
        playlistCheck.setTextColor(color(R.color.ytet_text));
        playlistCheck.setChecked(extractorIncludePlaylist);
        root.addView(playlistCheck, marginBottom(10));

        metadataEnhanceCheck = new CheckBox(this);
        metadataEnhanceCheck.setText("실제 제목/아티스트 보정");
        metadataEnhanceCheck.setTextColor(color(R.color.ytet_text));
        metadataEnhanceCheck.setChecked(extractorEnhanceMetadata);
        root.addView(metadataEnhanceCheck, marginBottom(10));

        subtitlesCheck = new CheckBox(this);
        subtitlesCheck.setText("한국어/영어 등록 자막 포함");
        subtitlesCheck.setTextColor(color(R.color.ytet_text));
        subtitlesCheck.setChecked(extractorIncludeSubtitles);
        root.addView(subtitlesCheck, marginBottom(18));

        root.addView(label("저장 폴더"), marginBottom(8));
        LinearLayout folderActions = new LinearLayout(this);
        folderActions.setOrientation(LinearLayout.HORIZONTAL);
        chooseFolderButton = secondaryButton("사용자 지정 폴더 선택");
        chooseFolderButton.setOnClickListener(view -> chooseOutputFolder());
        folderActions.addView(chooseFolderButton, weightedControlParams(8, 8));
        resetOutputButton = secondaryButton("기본값");
        resetOutputButton.setOnClickListener(view -> resetOutputFolder());
        folderActions.addView(resetOutputButton, weightedControlParams(2, 0));
        root.addView(folderActions, marginBottom(8));

        folderText = muted("", 13);
        root.addView(folderText, marginBottom(20));

        LinearLayout extractionActions = new LinearLayout(this);
        extractionActions.setOrientation(LinearLayout.HORIZONTAL);
        extractButton = primaryButton("추출");
        extractButton.setOnClickListener(view -> startExtraction());
        extractionActions.addView(extractButton, weightedControlParams(7, 8));
        cancelExtractButton = dangerButton("취소");
        cancelExtractButton.setOnClickListener(view -> cancelExtraction());
        extractionActions.addView(cancelExtractButton, weightedControlParams(3, 0));
        root.addView(extractionActions, marginBottom(18));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        root.addView(progressBar, marginBottom(12));

        statusText = muted("", 14);
        root.addView(statusText, marginBottom(12));

        resultText = text("", 14, R.color.ytet_text, false);
        resultText.setLineSpacing(0, 1.12f);
        root.addView(resultText, matchWrap());

        updateModeOptions();
        selectOption(extractorMediaType, extractorOption);
        updateFolderLabel();
        applyExtractionStateToViews();
        setBusy(extractionBusy);
        return root;
    }

    private MusicStation firstHomeStation() {
        List<MusicStation> stations = StationCatalog.recommendedStations(homeTracks);
        return stations.isEmpty() ? null : stations.get(0);
    }

    private MusicStation firstStation() {
        List<MusicStation> stations = StationCatalog.recommendedStations(libraryTracks);
        return stations.isEmpty() ? null : stations.get(0);
    }

    private MusicStation singleTrackStation(DeviceAudioTrack track) {
        return new MusicStation(
                "track-" + track.id(),
                "선택한 파일",
                "파일 재생",
                track.artist(),
                track.displayName(),
                MusicStation.MixType.TRACK,
                Long.toString(track.id()),
                color(R.color.ytet_accent)
        );
    }

    private void playStation(MusicStation station) {
        playStation(station, libraryTracks);
    }

    private void playStation(MusicStation station, List<DeviceAudioTrack> sourceTracks) {
        if (station == null) {
            toast("재생할 음악이 없습니다.");
            return;
        }
        List<DeviceAudioTrack> queue = MusicLibrary.tracksForStation(sourceTracks, station);
        if (queue.isEmpty()) {
            toast("이 믹스에 포함할 음악이 없습니다.");
            return;
        }
        activeStation = station;
        activeQueuePreview = new ArrayList<>(queue);
        applyPreviewTrackTheme(queue.get(0));
        playbackHasQueue = true;
        playbackPlaying = false;
        playbackPreparing = true;
        playbackWillPlay = true;
        playbackError = false;
        playbackTitle = station.title();
        playbackMeta = station.subtitle();
        setStreamingStatus("준비 중: " + station.title());
        updateNowPlayingBar();
        startPlayback(PlaybackService.playQueueIntent(this, station, queue));
    }

    private void playOnlineStream(OnlineStreamSection section, int startIndex) {
        List<DeviceAudioTrack> queue = onlineTracks(section);
        if (queue.isEmpty()) {
            toast("재생할 온라인 영상이 없습니다.");
            return;
        }
        int safeIndex = Math.max(0, Math.min(startIndex, queue.size() - 1));
        MusicStation station = new MusicStation(
                "stream-" + section.channelId(),
                section.channelTitle(),
                "스트림",
                "온라인 추천",
                queue.size() + "개 영상",
                MusicStation.MixType.TRACK,
                "",
                color(R.color.ytet_accent)
        );
        activeStation = station;
        activeQueuePreview = new ArrayList<>(queue);
        applyPreviewTrackTheme(queue.get(safeIndex));
        playbackHasQueue = true;
        playbackPlaying = false;
        playbackPreparing = true;
        playbackWillPlay = true;
        playbackError = false;
        playbackTitle = queue.get(safeIndex).title();
        playbackMeta = queue.get(safeIndex).artist() + " · " + station.title();
        setStreamingStatus("온라인 스트림 준비 중: " + queue.get(safeIndex).title());
        updateNowPlayingBar();
        startPlayback(PlaybackService.playOnlineQueueIntent(this, station, queue, safeIndex));
    }

    private List<DeviceAudioTrack> onlineTracks(OnlineStreamSection section) {
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        if (section == null) {
            return tracks;
        }
        for (OnlineStreamVideo video : section.videos()) {
            if (video.watchUrl().trim().isEmpty()) {
                continue;
            }
            tracks.add(new DeviceAudioTrack(
                    video.playbackId(),
                    video.title(),
                    video.channelTitle(),
                    section.channelTitle(),
                    video.title(),
                    "온라인 스트림",
                    video.watchUrl(),
                    video.thumbnailUrl(),
                    0L,
                    0,
                    System.currentTimeMillis(),
                    video.durationMs(),
                    0L,
                    video.channelTitle()
            ));
        }
        return tracks;
    }

    private void playTrack(DeviceAudioTrack track) {
        if (track == null) {
            return;
        }
        MusicStation station = singleTrackStation(track);
        activeStation = station;
        playbackHasQueue = true;
        applyPreviewTrackTheme(track);
        playbackPlaying = false;
        playbackPreparing = true;
        playbackWillPlay = true;
        playbackError = false;
        playbackTitle = track.title();
        playbackMeta = track.artist() + " · " + track.album();
        setStreamingStatus("준비 중: " + track.title());
        updateNowPlayingBar();
        List<DeviceAudioTrack> queue = new ArrayList<>();
        queue.add(track);
        activeQueuePreview = new ArrayList<>(queue);
        startPlayback(PlaybackService.playQueueIntent(this, station, queue));
    }

    private void playVisibleLibraryTrack(DeviceAudioTrack track) {
        if (track == null) {
            return;
        }
        List<DeviceAudioTrack> queue = visibleLibraryTracks();
        int startIndex = indexOfTrack(queue, track);
        if (queue.isEmpty() || startIndex < 0) {
            playTrack(track);
            return;
        }
        String trimmedQuery = librarySearchQuery == null ? "" : librarySearchQuery.trim();
        boolean searching = !trimmedQuery.isEmpty();
        MusicStation station = new MusicStation(
                "library-visible-" + Integer.toHexString((librarySource + "|" + librarySort.key + "|" + trimmedQuery).hashCode()),
                searching ? "검색 결과" : "전체 음악",
                "내 음악",
                searching ? trimmedQuery : "현재 목록 순서",
                queue.size() + "곡 재생",
                MusicStation.MixType.TRACK,
                "",
                color(R.color.ytet_accent)
        );
        activeStation = station;
        activeQueuePreview = new ArrayList<>(queue);
        applyPreviewTrackTheme(queue.get(startIndex));
        playbackHasQueue = true;
        playbackPlaying = false;
        playbackPreparing = true;
        playbackWillPlay = true;
        playbackError = false;
        playbackTitle = queue.get(startIndex).title();
        playbackMeta = queue.get(startIndex).artist() + " · " + queue.get(startIndex).album();
        setStreamingStatus("준비 중: " + queue.get(startIndex).title());
        updateNowPlayingBar();
        startPlayback(PlaybackService.playQueueIntent(this, station, queue, startIndex));
    }

    private int indexOfTrack(List<DeviceAudioTrack> tracks, DeviceAudioTrack target) {
        if (tracks == null || target == null) {
            return -1;
        }
        for (int index = 0; index < tracks.size(); index++) {
            DeviceAudioTrack track = tracks.get(index);
            if (track != null && track.id() == target.id()) {
                return index;
            }
        }
        return -1;
    }

    private void playQueueTrack(DeviceAudioTrack track, int index) {
        if (track == null || activeQueuePreview.isEmpty() || index < 0 || index >= activeQueuePreview.size()) {
            playTrack(track);
            return;
        }
        MusicStation station = activeStation == null ? singleTrackStation(track) : activeStation;
        activeStation = station;
        selectedTrack = track;
        playbackHasQueue = true;
        applyPreviewTrackTheme(track);
        playbackPlaying = false;
        playbackPreparing = true;
        playbackWillPlay = true;
        playbackError = false;
        playbackTitle = track.title();
        playbackMeta = track.artist() + " · " + track.album();
        activeQueuePreview = new ArrayList<>(activeQueuePreview);
        playbackQueueIndex = index;
        setStreamingStatus("준비 중: " + track.title());
        updateNowPlayingBar();
        updateQueueDialog();
        startPlayback(PlaybackService.seekQueueIndexIntent(this, index));
    }

    private void toggleStreamPlayback() {
        if (playbackHasQueue || activeStation != null) {
            startPlayback(PlaybackService.commandIntent(this, PlaybackService.ACTION_TOGGLE));
            return;
        }
        if (!playbackPreparing) {
            playStation(activeStation == null ? firstStation() : activeStation);
        }
    }

    private void previousTrack() {
        if (hasPreviousTrack()) {
            startPlayback(PlaybackService.commandIntent(this, PlaybackService.ACTION_PREVIOUS));
        }
    }

    private void nextTrack() {
        if (hasNextTrack()) {
            startPlayback(PlaybackService.commandIntent(this, PlaybackService.ACTION_NEXT));
        }
    }

    private boolean handleNowPlayingInfoTouch(View view, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            nowPlayingSwipeStartX = event.getRawX();
            nowPlayingSwipeStartY = event.getRawY();
            nowPlayingSwipeTracking = true;
            nowPlayingSwipeConsumed = false;
            cancelNowPlayingContentAnimations();
            hideNowPlayingSwipePreview();
            return true;
        }
        if (!nowPlayingSwipeTracking) {
            return false;
        }
        float dx = event.getRawX() - nowPlayingSwipeStartX;
        float dy = event.getRawY() - nowPlayingSwipeStartY;
        if (action == MotionEvent.ACTION_MOVE) {
            int slop = ViewConfiguration.get(this).getScaledTouchSlop();
            if (!nowPlayingSwipeConsumed
                    && Math.abs(dx) > slop
                    && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                nowPlayingSwipeConsumed = true;
                view.getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (nowPlayingSwipeConsumed) {
                int direction = dx < 0f ? NOW_PLAYING_TRANSITION_NEXT : NOW_PLAYING_TRANSITION_PREVIOUS;
                boolean canMove = direction == NOW_PLAYING_TRANSITION_PREVIOUS ? hasPreviousTrack() : hasNextTrack();
                float resistance = canMove ? NOW_PLAYING_SWIPE_RESISTANCE : NOW_PLAYING_BLOCKED_SWIPE_RESISTANCE;
                float maxOffset = nowPlayingSwipeMaxOffset(view);
                float offset = Math.max(-maxOffset, Math.min(maxOffset, dx * resistance));
                applyNowPlayingSwipeOffset(direction, canMove, offset, maxOffset);
                return true;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            nowPlayingSwipeTracking = false;
            view.getParent().requestDisallowInterceptTouchEvent(false);
            if (nowPlayingSwipeConsumed && action == MotionEvent.ACTION_UP) {
                int direction = dx < 0f ? NOW_PLAYING_TRANSITION_NEXT : NOW_PLAYING_TRANSITION_PREVIOUS;
                boolean canMove = direction == NOW_PLAYING_TRANSITION_PREVIOUS ? hasPreviousTrack() : hasNextTrack();
                if (canMove && nowPlayingSwipeVisualProgress(view, dx, canMove) >= NOW_PLAYING_SWIPE_COMMIT_PROGRESS) {
                    triggerNowPlayingSwipe(direction);
                    return true;
                }
            }
            if (!nowPlayingSwipeConsumed && action == MotionEvent.ACTION_UP) {
                showExpandedPlayer();
            } else {
                snapNowPlayingInfoFrame();
            }
            nowPlayingSwipeConsumed = false;
            return true;
        }
        return true;
    }

    private float nowPlayingSwipeMaxOffset(View view) {
        int width = view == null ? 0 : view.getWidth();
        return Math.max(dp(150), width + dp(12));
    }

    private float nowPlayingSwipeVisualProgress(View view, float dx, boolean canMove) {
        float resistance = canMove ? NOW_PLAYING_SWIPE_RESISTANCE : NOW_PLAYING_BLOCKED_SWIPE_RESISTANCE;
        float maxOffset = nowPlayingSwipeMaxOffset(view);
        float offset = Math.max(-maxOffset, Math.min(maxOffset, dx * resistance));
        return Math.min(1f, Math.abs(offset) / Math.max(1f, maxOffset));
    }

    private void applyNowPlayingSwipeOffset(int direction, boolean canMove, float offset, float maxOffset) {
        float progress = Math.min(1f, Math.abs(offset) / Math.max(1f, maxOffset));
        if (nowPlayingCurrentFrame != null) {
            float currentFade = canMove
                    ? Math.max(0f, (progress - NOW_PLAYING_CURRENT_FADE_START) / NOW_PLAYING_CURRENT_FADE_RANGE)
                    : progress;
            nowPlayingCurrentFrame.setTranslationX(offset);
            nowPlayingCurrentFrame.setAlpha(1f - Math.min(1f, currentFade) * (canMove ? 0.90f : 0.24f));
        }
        if (!canMove) {
            hideNowPlayingSwipePreview();
            return;
        }

        DeviceAudioTrack previewTrack = nowPlayingTrackForDirection(direction);
        if (previewTrack == null) {
            hideNowPlayingSwipePreview();
            return;
        }
        setNowPlayingSwipePreviewTrack(previewTrack, direction);
        if (nowPlayingPreviewFrame != null) {
            float previewStart = direction == NOW_PLAYING_TRANSITION_NEXT ? maxOffset : -maxOffset;
            float previewProgress = Math.max(0f,
                    (progress - NOW_PLAYING_PREVIEW_FADE_START) / NOW_PLAYING_PREVIEW_FADE_RANGE);
            nowPlayingPreviewFrame.setVisibility(View.VISIBLE);
            nowPlayingPreviewFrame.setTranslationX(previewStart + offset);
            nowPlayingPreviewFrame.setAlpha(Math.min(1f, previewProgress * 1.12f));
        }
    }

    private void setNowPlayingSwipePreviewTrack(DeviceAudioTrack track, int direction) {
        if (track == null
                || nowPlayingPreviewCover == null
                || nowPlayingPreviewTitle == null
                || nowPlayingPreviewMeta == null) {
            return;
        }
        if (renderedNowPlayingPreviewTrackId == track.id()
                && renderedNowPlayingPreviewDirection == direction) {
            return;
        }
        renderedNowPlayingPreviewTrackId = track.id();
        renderedNowPlayingPreviewDirection = direction;
        nowPlayingPreviewCover.removeAllViews();
        nowPlayingPreviewCover.addView(nowPlayingCoverView(track), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setMarqueeText(nowPlayingPreviewTitle, track.title());
        setMarqueeText(nowPlayingPreviewMeta, track.artist() + " · " + valueOrDefault(track.album(), "앨범 정보 없음"));
    }

    private void hideNowPlayingSwipePreview() {
        renderedNowPlayingPreviewTrackId = Long.MIN_VALUE;
        renderedNowPlayingPreviewDirection = NOW_PLAYING_TRANSITION_NONE;
        if (nowPlayingPreviewFrame == null) {
            return;
        }
        nowPlayingPreviewFrame.animate().cancel();
        nowPlayingPreviewFrame.setVisibility(View.INVISIBLE);
        nowPlayingPreviewFrame.setAlpha(0f);
        nowPlayingPreviewFrame.setTranslationX(0f);
    }

    private DeviceAudioTrack nowPlayingTrackForDirection(int direction) {
        if (activeQueuePreview.isEmpty() || playbackQueueIndex < 0) {
            return null;
        }
        int index = playbackQueueIndex + (direction == NOW_PLAYING_TRANSITION_NEXT ? 1 : -1);
        if (index >= activeQueuePreview.size() && playbackRepeatMode == PlaybackService.REPEAT_ALL) {
            index = 0;
        } else if (index < 0 && playbackRepeatMode == PlaybackService.REPEAT_ALL) {
            index = activeQueuePreview.size() - 1;
        }
        if (index < 0 || index >= activeQueuePreview.size()) {
            return null;
        }
        return activeQueuePreview.get(index);
    }

    private void triggerNowPlayingSwipe(int direction) {
        if (nowPlayingCurrentFrame == null) {
            return;
        }
        pendingNowPlayingTransitionDirection = direction;
        long requestedTrackId = playbackTrackId;
        float maxOffset = nowPlayingSwipeMaxOffset(nowPlayingInfoFrame);
        float currentTarget = direction == NOW_PLAYING_TRANSITION_NEXT ? -maxOffset : maxOffset;
        nowPlayingCurrentFrame.animate()
                .translationX(currentTarget)
                .alpha(0f)
                .setInterpolator(new DecelerateInterpolator(1.7f))
                .setDuration(180L)
                .start();
        if (nowPlayingPreviewFrame != null && nowPlayingPreviewFrame.getVisibility() == View.VISIBLE) {
            nowPlayingPreviewFrame.animate().cancel();
            nowPlayingPreviewFrame.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setInterpolator(new DecelerateInterpolator(1.8f))
                    .setDuration(210L)
                    .start();
        }
        if (direction == NOW_PLAYING_TRANSITION_NEXT) {
            nextTrack();
        } else if (direction == NOW_PLAYING_TRANSITION_PREVIOUS) {
            previousTrack();
        }
        mainHandler.postDelayed(() -> {
            if (pendingNowPlayingTransitionDirection != NOW_PLAYING_TRANSITION_NONE
                    && playbackTrackId == requestedTrackId) {
                pendingNowPlayingTransitionDirection = NOW_PLAYING_TRANSITION_NONE;
                nowPlayingSwipeConsumed = false;
                snapNowPlayingInfoFrame();
            }
        }, 800L);
    }

    private void snapNowPlayingInfoFrame() {
        if (nowPlayingCurrentFrame == null) {
            return;
        }
        nowPlayingCurrentFrame.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(160L)
                .start();
        if (nowPlayingPreviewFrame != null && nowPlayingPreviewFrame.getVisibility() == View.VISIBLE) {
            nowPlayingPreviewFrame.animate()
                    .translationX(0f)
                    .alpha(0f)
                    .setDuration(140L)
                    .withEndAction(this::hideNowPlayingSwipePreview)
                    .start();
        }
    }

    private boolean hasPreviousTrack() {
        return playbackHasQueue
                && playbackQueueSize > 1
                && (playbackQueueIndex > 0 || playbackRepeatMode == PlaybackService.REPEAT_ALL);
    }

    private boolean hasNextTrack() {
        return playbackHasQueue
                && playbackQueueSize > 1
                && (playbackQueueIndex < playbackQueueSize - 1 || playbackRepeatMode == PlaybackService.REPEAT_ALL);
    }

    private boolean canShuffleQueue() {
        return playbackHasQueue && playbackQueueSize > 1;
    }

    private void toggleShuffle() {
        if (canShuffleQueue()) {
            startPlayback(PlaybackService.commandIntent(this, PlaybackService.ACTION_TOGGLE_SHUFFLE));
        }
    }

    private void toggleRepeat() {
        if (playbackHasQueue) {
            startPlayback(PlaybackService.commandIntent(this, PlaybackService.ACTION_TOGGLE_REPEAT));
        }
    }

    private void startPlayback(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && (PlaybackService.ACTION_PLAY_QUEUE.equals(intent.getAction())
                || PlaybackService.ACTION_PLAY_ONLINE_QUEUE.equals(intent.getAction()))) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void setStreamingStatus(String status) {
        streamStatus = status;
    }

    private void updateNowPlayingBar() {
        if (nowPlayingBar == null || nowPlayingTitle == null || nowPlayingMeta == null || playPauseButton == null) {
            return;
        }
        boolean idle = !playbackHasQueue && activeStation == null;
        if (idle) {
            boolean wasVisible = nowPlayingBar.getVisibility() == View.VISIBLE;
            cancelNowPlayingContentAnimations();
            updatePlaybackThemeColor(true);
            nowPlayingBar.setVisibility(View.GONE);
            nowPlayingContentInitialized = false;
            renderedNowPlayingIdle = true;
            renderedNowPlayingTrackId = -1L;
            renderedNowPlayingTitle = "";
            renderedNowPlayingMeta = "";
            if (nowPlayingProgress != null) {
                nowPlayingProgress.setProgress(0L, 0L, false);
            }
            if (wasVisible) {
                updateMainContentBottomPadding(mainNavigationInset);
            }
            return;
        }
        boolean wasHidden = nowPlayingBar.getVisibility() != View.VISIBLE;
        nowPlayingBar.setVisibility(View.VISIBLE);
        if (wasHidden) {
            updateMainContentBottomPadding(mainNavigationInset);
        }
        String title = idle ? "로컬 재생 대기" : playbackTitle;
        String meta = idle
                ? "기기 음악을 스캔하면 재생할 수 있습니다."
                : playbackPreparing || playbackError ? streamStatus : miniPlaybackMeta();
        long renderTrackId = idle ? -1L : playbackTrackId;
        boolean contentChanged = !nowPlayingContentInitialized
                || renderedNowPlayingIdle != idle
                || renderedNowPlayingTrackId != renderTrackId
                || !TextUtils.equals(renderedNowPlayingTitle, title)
                || !TextUtils.equals(renderedNowPlayingMeta, meta);

        updatePlaybackThemeColor(idle);
        nowPlayingBar.setBackground(nowPlayingBarBackground(idle));
        if (nowPlayingProgress != null) {
            nowPlayingProgress.setProgress(playbackDurationMs, playbackPositionMs, !idle && playbackDurationMs > 0L);
        }
        if (contentChanged) {
            applyNowPlayingContent(idle, title, meta, nowPlayingContentInitialized);
        } else if (!nowPlayingSwipeConsumed) {
            resetNowPlayingInfoFrame();
        }
        renderedNowPlayingIdle = idle;
        renderedNowPlayingTrackId = renderTrackId;
        renderedNowPlayingTitle = title;
        renderedNowPlayingMeta = meta;
        nowPlayingContentInitialized = true;

        boolean waitingToPlay = playbackPlaying || playbackWillPlay;
        playPauseButton.setImageResource(waitingToPlay ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        playPauseButton.setContentDescription(waitingToPlay ? "일시정지" : "재생");
    }

    private void applyNowPlayingContent(boolean idle, String title, String meta, boolean animate) {
        if (!animate) {
            setNowPlayingCover(idle);
            setMarqueeText(nowPlayingTitle, title);
            setMarqueeText(nowPlayingMeta, meta);
            setNowPlayingContentAlpha(1f);
            resetNowPlayingInfoFrame();
            return;
        }
        cancelNowPlayingContentAnimations();
        int direction = pendingNowPlayingTransitionDirection;
        pendingNowPlayingTransitionDirection = NOW_PLAYING_TRANSITION_NONE;
        if (direction != NOW_PLAYING_TRANSITION_NONE
                && nowPlayingPreviewFrame != null
                && nowPlayingPreviewFrame.getVisibility() == View.VISIBLE) {
            float previewTranslation = nowPlayingPreviewFrame.getTranslationX();
            float previewAlpha = nowPlayingPreviewFrame.getAlpha();
            setNowPlayingCover(idle);
            setMarqueeText(nowPlayingTitle, title);
            setMarqueeText(nowPlayingMeta, meta);
            hideNowPlayingSwipePreview();
            if (nowPlayingCurrentFrame != null) {
                nowPlayingCurrentFrame.setTranslationX(previewTranslation);
                nowPlayingCurrentFrame.setAlpha(Math.max(0.72f, previewAlpha));
                nowPlayingCurrentFrame.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setInterpolator(new DecelerateInterpolator(1.8f))
                        .setDuration(170L)
                        .start();
            } else {
                resetNowPlayingInfoFrame();
            }
            nowPlayingSwipeConsumed = false;
            return;
        }
        if (nowPlayingCurrentFrame != null) {
            nowPlayingCurrentFrame.animate()
                    .alpha(0f)
                    .setDuration(110L)
                    .withEndAction(() -> {
                        setNowPlayingCover(idle);
                        setMarqueeText(nowPlayingTitle, title);
                        setMarqueeText(nowPlayingMeta, meta);
                        setNowPlayingContentAlpha(1f);
                        nowPlayingCurrentFrame.setTranslationX(0f);
                        nowPlayingCurrentFrame.setAlpha(0f);
                        nowPlayingCurrentFrame.animate()
                                .alpha(1f)
                                .setDuration(170L)
                                .start();
                    })
                    .start();
            return;
        }
        nowPlayingCover.animate().alpha(0f).setDuration(90L).start();
        nowPlayingMeta.animate().alpha(0f).setDuration(90L).start();
        nowPlayingTitle.animate()
                .alpha(0f)
                .setDuration(90L)
                .withEndAction(() -> {
                    setNowPlayingCover(idle);
                    setMarqueeText(nowPlayingTitle, title);
                    setMarqueeText(nowPlayingMeta, meta);
                    setNowPlayingContentAlpha(0f);
                    nowPlayingCover.animate().alpha(1f).setDuration(160L).start();
                    nowPlayingTitle.animate().alpha(1f).setDuration(160L).start();
                    nowPlayingMeta.animate().alpha(1f).setDuration(160L).start();
                })
                .start();
    }

    private void cancelNowPlayingContentAnimations() {
        if (nowPlayingCurrentFrame != null) {
            nowPlayingCurrentFrame.animate().cancel();
        }
        if (nowPlayingPreviewFrame != null) {
            nowPlayingPreviewFrame.animate().cancel();
        }
        if (nowPlayingCover != null) {
            nowPlayingCover.animate().cancel();
        }
        if (nowPlayingTitle != null) {
            nowPlayingTitle.animate().cancel();
        }
        if (nowPlayingMeta != null) {
            nowPlayingMeta.animate().cancel();
        }
    }

    private void resetNowPlayingInfoFrame() {
        if (nowPlayingInfoFrame != null) {
            nowPlayingInfoFrame.setAlpha(1f);
            nowPlayingInfoFrame.setTranslationX(0f);
        }
        if (nowPlayingCurrentFrame != null) {
            nowPlayingCurrentFrame.setAlpha(1f);
            nowPlayingCurrentFrame.setTranslationX(0f);
        }
        hideNowPlayingSwipePreview();
    }

    private void setNowPlayingContentAlpha(float alpha) {
        if (nowPlayingCurrentFrame != null) {
            nowPlayingCurrentFrame.setAlpha(alpha);
        }
        if (nowPlayingCover != null) {
            nowPlayingCover.setAlpha(alpha);
        }
        if (nowPlayingTitle != null) {
            nowPlayingTitle.setAlpha(alpha);
        }
        if (nowPlayingMeta != null) {
            nowPlayingMeta.setAlpha(alpha);
        }
    }

    private void setMarqueeText(TextView view, String value) {
        if (view == null) {
            return;
        }
        String nextValue = value == null ? "" : value;
        if (TextUtils.equals(view.getText(), nextValue)) {
            view.setSelected(true);
            return;
        }
        view.setText(nextValue);
        view.setSelected(false);
        view.post(() -> view.setSelected(true));
    }

    private void setNowPlayingCover(boolean idle) {
        if (nowPlayingCover == null) {
            return;
        }
        nowPlayingCover.removeAllViews();
        nowPlayingCover.addView(nowPlayingCoverView(idle), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
    }

    private View nowPlayingCoverView(boolean idle) {
        return nowPlayingCoverView(
                idle,
                playbackAlbumArtUri,
                idle ? "RY" : coverInitials(),
                idle ? color(R.color.ytet_panel_alt) : color(R.color.ytet_accent_dark)
        );
    }

    private View nowPlayingCoverView(DeviceAudioTrack track) {
        String title = track == null ? "" : valueOrDefault(track.title(), "");
        String initials = title.isEmpty()
                ? "RY"
                : title.substring(0, Math.min(2, title.length())).toUpperCase();
        return nowPlayingCoverView(
                false,
                track == null ? "" : track.albumArtUri(),
                initials,
                color(R.color.ytet_accent_dark)
        );
    }

    private View nowPlayingCoverView(boolean idle, String artworkUri, String initials, int placeholderColor) {
        if (!idle && artworkUri != null && !artworkUri.trim().isEmpty()) {
            if (isHttpUrl(artworkUri)) {
                return onlineImageView(artworkUri, initials, 44, 8);
            }
            ImageView image = new ImageView(this);
            image.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageURI(Uri.parse(artworkUri));
            return image;
        }
        TextView placeholder = text(initials, 13, android.R.color.white, true);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackground(rounded(placeholderColor, 8));
        return placeholder;
    }

    private void applyPreviewTrackTheme(DeviceAudioTrack track) {
        if (track == null) {
            return;
        }
        playbackTrackId = track.id();
        playbackArtist = valueOrDefault(track.artist(), "알 수 없는 아티스트");
        playbackAlbum = valueOrDefault(track.album(), "앨범 정보 없음");
        playbackAlbumArtUri = valueOrDefault(track.albumArtUri(), "");
        updatePlaybackThemeColor(false);
    }

    private void updatePlaybackThemeColor(boolean idle) {
        String artworkUri = idle ? "" : valueOrDefault(playbackAlbumArtUri, "");
        if (artworkUri.equals(playbackThemeAlbumArtUri)) {
            return;
        }
        playbackThemeAlbumArtUri = artworkUri;
        playbackThemeLoadingUri = "";
        if (artworkUri.isEmpty()) {
            playbackThemeColor = fallbackPlayerThemeColor(idle);
        } else if (isHttpUrl(artworkUri)) {
            playbackThemeColor = fallbackPlayerThemeColor(false);
            loadRemoteArtworkThemeColor(artworkUri);
        } else {
            playbackThemeColor = readArtworkThemeColor(artworkUri);
        }
    }

    private void loadRemoteArtworkThemeColor(String artworkUri) {
        playbackThemeLoadingUri = artworkUri;
        imageExecutor.execute(() -> {
            Bitmap bitmap = loadRemoteBitmap(artworkUri, 160);
            int themeColor = bitmap == null
                    ? fallbackPlayerThemeColor(false)
                    : normalizeArtworkThemeColor(sampleArtworkColor(bitmap));
            if (bitmap != null) {
                bitmap.recycle();
            }
            runOnUiThread(() -> finishRemoteArtworkThemeColor(artworkUri, themeColor));
        });
    }

    private void finishRemoteArtworkThemeColor(String artworkUri, int themeColor) {
        if (!TextUtils.equals(playbackThemeLoadingUri, artworkUri)) {
            return;
        }
        playbackThemeLoadingUri = "";
        if (!TextUtils.equals(playbackThemeAlbumArtUri, artworkUri)
                || !TextUtils.equals(valueOrDefault(playbackAlbumArtUri, ""), artworkUri)) {
            return;
        }
        if (playbackThemeColor == themeColor) {
            return;
        }
        playbackThemeColor = themeColor;
        refreshPlaybackThemeSurfaces();
    }

    private void refreshPlaybackThemeSurfaces() {
        updateNowPlayingBar();
        updateExpandedPlayer();
        updateQueueDialog();
    }

    private GradientDrawable nowPlayingBarBackground(boolean idle) {
        int base = idle ? color(R.color.ytet_panel) : playbackThemeColor;
        int end = idle
                ? color(R.color.ytet_panel)
                : blendColors(base, color(R.color.ytet_panel), 0.46f);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{base, end});
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private GradientDrawable bottomVignetteBackground() {
        int red = Color.red(BOTTOM_CHROME_BASE);
        int green = Color.green(BOTTOM_CHROME_BASE);
        int blue = Color.blue(BOTTOM_CHROME_BASE);
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        Color.argb(8, red, green, blue),
                        Color.argb(14, red, green, blue),
                        Color.argb(22, red, green, blue),
                        Color.argb(34, red, green, blue),
                        Color.argb(50, red, green, blue),
                        Color.argb(70, red, green, blue),
                        Color.argb(94, red, green, blue),
                        Color.argb(122, red, green, blue),
                        Color.argb(154, red, green, blue),
                        Color.argb(186, red, green, blue),
                        Color.argb(216, red, green, blue),
                        Color.argb(238, red, green, blue),
                        Color.argb(250, red, green, blue),
                        Color.argb(255, red, green, blue)
                }
        );
    }

    private GradientDrawable bottomChromeBackground() {
        int red = Color.red(BOTTOM_CHROME_BASE);
        int green = Color.green(BOTTOM_CHROME_BASE);
        int blue = Color.blue(BOTTOM_CHROME_BASE);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.argb(156, red, green, blue));
        return drawable;
    }

    private GradientDrawable expandedPlayerBackground(boolean roundedTop) {
        int base = playbackThemeColor;
        int background = color(R.color.ytet_background);
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        base,
                        blendColors(base, background, 0.36f),
                        blendColors(base, background, 0.72f),
                        background
                }
        );
        if (roundedTop) {
            float radius = dp(22);
            drawable.setCornerRadii(new float[]{radius, radius, radius, radius, 0f, 0f, 0f, 0f});
        }
        return drawable;
    }

    private void applyExpandedPlayerWindow(Window window) {
        if (window == null) {
            return;
        }
        int statusColor = playerStatusBarColor();
        int navigationColor = blendColors(color(R.color.ytet_background), statusColor, 0.16f);
        applyExpandedDialogBars(window, statusColor, navigationColor);
    }

    private void applyMainWindowBars() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            window.setAttributes(attributes);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
        }
        View decor = window.getDecorView();
        int flags = decor.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decor.setSystemUiVisibility(flags);
    }

    private void applyMainContentInsets(View root) {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets topInsets = insets.getInsets(WindowInsets.Type.statusBars() | WindowInsets.Type.displayCutout());
                Insets bottomInsets = insets.getInsets(WindowInsets.Type.navigationBars());
                topInset = topInsets.top;
                bottomInset = bottomInsets.bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(0, topInset, 0, 0);
            if (bottomNavigationGuard != null) {
                setViewHeight(bottomNavigationGuard, bottomInset);
            }
            if (bottomVignette != null) {
                setViewHeight(bottomVignette, bottomVignetteHeight(bottomInset));
            }
            updateMainContentBottomPadding(bottomInset);
            return insets;
        });
        root.post(root::requestApplyInsets);
    }

    private void updateMainContentBottomPadding(int navigationInset) {
        mainNavigationInset = navigationInset;
        if (contentScrollView == null) {
            return;
        }
        contentScrollView.setPadding(0, 0, 0, bottomChromeBaseHeight() + navigationInset + dp(8));
        if (libraryRecyclerView != null) {
            libraryRecyclerView.setPadding(0, 0, 0, bottomChromeBaseHeight() + navigationInset + dp(8));
        }
    }

    private int bottomChromeBaseHeight() {
        int nowPlayingHeight = nowPlayingBar != null && nowPlayingBar.getVisibility() == View.VISIBLE ? dp(64) : 0;
        return nowPlayingHeight + bottomTabsHeight();
    }

    private int bottomTabsHeight() {
        return dp(50);
    }

    private int bottomVignetteHeight(int navigationInset) {
        return Math.max(dp(60), navigationInset + bottomTabsHeight());
    }

    private int queueSheetCollapsedHeight() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        return Math.max(dp(340), Math.round(screenHeight * 0.52f));
    }

    private void applyQueueWindow(Window window) {
        if (window == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                | WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = 0f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }
        window.setAttributes(attributes);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
        }
        View decor = window.getDecorView();
        decor.setSystemUiVisibility(decor.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    private void applyOpaqueDialogBars(Window window, int statusColor, int navigationColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true);
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            window.setAttributes(attributes);
        }
        window.setBackgroundDrawable(new ColorDrawable(statusColor));
        window.setStatusBarColor(statusColor);
        window.setNavigationBarColor(navigationColor);
        View decor = window.getDecorView();
        decor.setSystemUiVisibility(decor.getSystemUiVisibility()
                & ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                & ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    private void applyExpandedDialogBars(Window window, int statusColor, int navigationColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            attributes.gravity = Gravity.TOP | Gravity.START;
            attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
            attributes.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(attributes);
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(navigationColor);
        View decor = window.getDecorView();
        decor.setBackgroundColor(Color.TRANSPARENT);
        int flags = decor.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decor.setSystemUiVisibility(flags);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    private int playerStatusScrimColor() {
        return Color.argb(144, 0, 0, 0);
    }

    private int playerStatusBarColor() {
        int base = playbackThemeColor;
        float[] hsv = new float[3];
        Color.colorToHSV(base, hsv);
        hsv[1] = Math.min(1f, Math.max(0.42f, hsv[1] * 1.18f));
        hsv[2] = Math.max(0.08f, Math.min(0.22f, hsv[2] * 0.58f));
        return Color.HSVToColor(hsv);
    }

    private int expandedPlayerStatusInset() {
        return expandedPlayerDrawsBehindSystemBars()
                ? Math.max(systemBarDimension("status_bar_height"), dp(24))
                : 0;
    }

    private int expandedPlayerNavigationInset() {
        return expandedPlayerDrawsBehindSystemBars()
                ? systemBarDimension("navigation_bar_height")
                : 0;
    }

    private void applyExpandedPlayerInsets(
            DragDismissLayout root,
            View statusScrim,
            LinearLayout content,
            ExpandedPlayerLayout layout
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets topInsets = insets.getInsets(WindowInsets.Type.statusBars() | WindowInsets.Type.displayCutout());
            Insets bottomInsets = insets.getInsets(WindowInsets.Type.navigationBars());
            int topInset = Math.max(topInsets.top, expandedPlayerStatusInset());
            int bottomInset = Math.max(bottomInsets.bottom, expandedPlayerNavigationInset());
            setViewHeight(statusScrim, topInset);
            content.setPadding(
                    layout.horizontalPadding,
                    layout.topPadding + topInset,
                    layout.horizontalPadding,
                    layout.bottomPadding + bottomInset
            );
            return insets;
        });
        root.requestApplyInsets();
    }

    private ExpandedPlayerLayout expandedPlayerLayout(int statusInset, int navigationInset) {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int topPadding = dp(22);
        int bottomPadding = dp(24);
        int availableHeight = Math.max(
                dp(360),
                screenHeight - statusInset - navigationInset - topPadding - bottomPadding
        );
        boolean compact = availableHeight < dp(640);
        boolean tight = availableHeight < dp(560);
        boolean tiny = availableHeight < dp(500);

        int topGap = tiny ? dp(8) : tight ? dp(12) : compact ? dp(18) : dp(26);
        int coverBottom = tiny ? dp(6) : tight ? dp(8) : compact ? dp(10) : dp(14);
        int sleepStatusBottom = tiny ? dp(2) : compact ? dp(4) : dp(8);
        int titleBottom = tiny ? dp(2) : dp(4);
        int artistBottom = tiny ? dp(1) : dp(2);
        int albumBottom = tiny ? dp(3) : compact ? dp(5) : dp(8);
        int progressTextBottom = tiny ? dp(4) : compact ? dp(6) : dp(10);
        int controlsBottom = tiny ? dp(2) : compact ? dp(4) : dp(6);
        int progressHeight = tiny ? dp(34) : compact ? dp(38) : dp(42);
        int controlButtonHeight = tiny ? dp(44) : compact ? dp(47) : dp(50);
        int toolsHeight = tiny ? dp(76) : tight ? dp(84) : compact ? dp(90) : dp(98);
        int queueHandleHeight = tiny ? dp(46) : dp(52);
        int toolIconSize = tiny ? dp(50) : tight ? dp(54) : dp(58);
        int queueIconSize = Math.max(dp(46), toolIconSize - dp(4));
        int titleTextSp = tiny ? 20 : compact ? 22 : 23;

        int fixedWithoutCover = 0;
        fixedWithoutCover += dp(42) + topGap;
        fixedWithoutCover += dp(42) + sleepStatusBottom;
        fixedWithoutCover += estimatedTextLineHeight(titleTextSp) + titleBottom;
        fixedWithoutCover += estimatedTextLineHeight(14) + artistBottom;
        fixedWithoutCover += estimatedTextLineHeight(12) + albumBottom;
        fixedWithoutCover += progressHeight;
        fixedWithoutCover += estimatedTextLineHeight(12) + progressTextBottom;
        fixedWithoutCover += controlButtonHeight + controlsBottom;
        fixedWithoutCover += toolsHeight;
        fixedWithoutCover += queueHandleHeight;

        int coverTarget = Math.min(dp(284), Math.max(dp(168), Math.round(availableHeight * 0.38f)));
        int coverMax = availableHeight - fixedWithoutCover - coverBottom;
        int coverMin = tiny ? dp(112) : tight ? dp(136) : dp(164);
        int coverHeight = clampPx(coverTarget, coverMin, dp(284));
        if (coverMax > 0) {
            coverHeight = Math.min(coverHeight, coverMax);
        }
        coverHeight = Math.max(dp(96), coverHeight);

        return new ExpandedPlayerLayout(
                dp(20),
                topPadding,
                bottomPadding,
                topGap,
                coverHeight,
                coverBottom,
                sleepStatusBottom,
                titleTextSp,
                titleBottom,
                artistBottom,
                albumBottom,
                progressHeight,
                progressTextBottom,
                controlButtonHeight,
                controlsBottom,
                toolsHeight,
                queueHandleHeight,
                toolIconSize,
                queueIconSize
        );
    }

    private int estimatedTextLineHeight(int sizeSp) {
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        return Math.round(sizeSp * scaledDensity + dp(5));
    }

    private int clampPx(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ExpandedPlayerLayout {
        final int horizontalPadding;
        final int topPadding;
        final int bottomPadding;
        final int topGap;
        final int coverHeight;
        final int coverBottom;
        final int sleepStatusBottom;
        final int titleTextSp;
        final int titleBottom;
        final int artistBottom;
        final int albumBottom;
        final int progressHeight;
        final int progressTextBottom;
        final int controlButtonHeight;
        final int controlsBottom;
        final int toolsHeight;
        final int queueHandleHeight;
        final int toolIconSize;
        final int queueIconSize;

        ExpandedPlayerLayout(
                int horizontalPadding,
                int topPadding,
                int bottomPadding,
                int topGap,
                int coverHeight,
                int coverBottom,
                int sleepStatusBottom,
                int titleTextSp,
                int titleBottom,
                int artistBottom,
                int albumBottom,
                int progressHeight,
                int progressTextBottom,
                int controlButtonHeight,
                int controlsBottom,
                int toolsHeight,
                int queueHandleHeight,
                int toolIconSize,
                int queueIconSize
        ) {
            this.horizontalPadding = horizontalPadding;
            this.topPadding = topPadding;
            this.bottomPadding = bottomPadding;
            this.topGap = topGap;
            this.coverHeight = coverHeight;
            this.coverBottom = coverBottom;
            this.sleepStatusBottom = sleepStatusBottom;
            this.titleTextSp = titleTextSp;
            this.titleBottom = titleBottom;
            this.artistBottom = artistBottom;
            this.albumBottom = albumBottom;
            this.progressHeight = progressHeight;
            this.progressTextBottom = progressTextBottom;
            this.controlButtonHeight = controlButtonHeight;
            this.controlsBottom = controlsBottom;
            this.toolsHeight = toolsHeight;
            this.queueHandleHeight = queueHandleHeight;
            this.toolIconSize = toolIconSize;
            this.queueIconSize = queueIconSize;
        }
    }

    private void setViewHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null || params.height == height) {
            return;
        }
        params.height = height;
        view.setLayoutParams(params);
    }

    private boolean expandedPlayerDrawsBehindSystemBars() {
        return true;
    }

    private int systemBarDimension(String name) {
        int identifier = getResources().getIdentifier(name, "dimen", "android");
        if (identifier == 0) {
            return 0;
        }
        return getResources().getDimensionPixelSize(identifier);
    }

    private int readArtworkThemeColor(String artworkUri) {
        Bitmap bitmap = null;
        try {
            Uri uri = Uri.parse(artworkUri);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(input, null, bounds);
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = artworkSampleSize(bounds.outWidth, bounds.outHeight);
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(input, null, options);
            }
            if (bitmap == null) {
                return fallbackPlayerThemeColor(false);
            }
            return normalizeArtworkThemeColor(sampleArtworkColor(bitmap));
        } catch (Exception exception) {
            return fallbackPlayerThemeColor(false);
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private int artworkSampleSize(int width, int height) {
        int sampleSize = 1;
        int largest = Math.max(width, height);
        while (largest / sampleSize > 96) {
            sampleSize *= 2;
        }
        return Math.max(1, sampleSize);
    }

    private int sampleArtworkColor(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int stepX = Math.max(1, width / 42);
        int stepY = Math.max(1, height / 42);
        float[] hsv = new float[3];
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        long weightTotal = 0L;
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.alpha(pixel) < 160) {
                    continue;
                }
                Color.colorToHSV(pixel, hsv);
                if (hsv[2] < 0.08f || (hsv[2] > 0.94f && hsv[1] < 0.18f)) {
                    continue;
                }
                long weight = Math.max(1L, Math.round(1f + hsv[1] * 5f));
                red += Color.red(pixel) * weight;
                green += Color.green(pixel) * weight;
                blue += Color.blue(pixel) * weight;
                weightTotal += weight;
            }
        }
        if (weightTotal == 0L) {
            return fallbackPlayerThemeColor(false);
        }
        return Color.rgb(
                (int) (red / weightTotal),
                (int) (green / weightTotal),
                (int) (blue / weightTotal)
        );
    }

    private int normalizeArtworkThemeColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if (hsv[1] < 0.12f) {
            hsv[1] = 0.06f;
            hsv[2] = 0.23f;
        } else {
            hsv[1] = Math.max(0.34f, Math.min(0.84f, hsv[1] * 1.12f));
            hsv[2] = Math.max(0.18f, Math.min(0.40f, hsv[2] * 0.58f));
        }
        return Color.HSVToColor(hsv);
    }

    private int fallbackPlayerThemeColor(boolean idle) {
        return idle ? color(R.color.ytet_panel) : color(R.color.ytet_accent_dark);
    }

    private int blendColors(int fromColor, int toColor, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        float inverse = 1f - clamped;
        return Color.rgb(
                Math.round(Color.red(fromColor) * inverse + Color.red(toColor) * clamped),
                Math.round(Color.green(fromColor) * inverse + Color.green(toColor) * clamped),
                Math.round(Color.blue(fromColor) * inverse + Color.blue(toColor) * clamped)
        );
    }

    private String miniPlaybackMeta() {
        String artist = valueOrDefault(playbackArtist, "알 수 없는 아티스트");
        String album = valueOrDefault(playbackAlbum, "");
        if (album.isEmpty() || "앨범 정보 없음".equals(album)) {
            return artist;
        }
        return artist + " · " + album;
    }

    private void showExpandedPlayer() {
        if (!playbackHasQueue && activeStation == null) {
            playStation(firstStation());
            return;
        }
        if (playerDialog == null) {
            playerDialog = new Dialog(this, R.style.Theme_Ytet_PlayerDialog);
            playerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            playerDialog.setOnDismissListener(dialog -> {
                expandedPlaybackSeekBar = null;
                expandedPlaybackProgressText = null;
                expandedSleepTimerRemainingText = null;
                renderedExpandedPlayerSignature = "";
            });
        }
        applyExpandedPlayerWindow(playerDialog.getWindow());
        playerDialog.setContentView(buildExpandedPlayerContent());
        renderedExpandedPlayerSignature = expandedPlayerRenderSignature();
        playerDialog.show();
        applyExpandedPlayerWindow(playerDialog.getWindow());
    }

    private void updateExpandedPlayer() {
        updateExpandedPlaybackProgress();
        if (playerDialog == null
                || !playerDialog.isShowing()
                || suppressPlayerDragDismiss
                || playerDragDismissActive
                || playbackSeeking) {
            return;
        }
        String signature = expandedPlayerRenderSignature();
        if (TextUtils.equals(renderedExpandedPlayerSignature, signature)) {
            updateExpandedDynamicText();
            return;
        }
        applyExpandedPlayerWindow(playerDialog.getWindow());
        playerDialog.setContentView(buildExpandedPlayerContent());
        renderedExpandedPlayerSignature = signature;
        applyExpandedPlayerWindow(playerDialog.getWindow());
    }

    private void updateExpandedPlaybackProgress() {
        if (playbackSeeking) {
            return;
        }
        if (expandedPlaybackSeekBar != null) {
            expandedPlaybackSeekBar.setProgress(playbackDurationMs, playbackPositionMs);
        }
        if (expandedPlaybackProgressText != null) {
            expandedPlaybackProgressText.setText(playbackProgressText());
        }
        updateExpandedDynamicText();
    }

    private void updateExpandedDynamicText() {
        if (expandedSleepTimerRemainingText != null) {
            expandedSleepTimerRemainingText.setText(sleepTimerInlineText());
        }
    }

    private String expandedPlayerRenderSignature() {
        return playbackTrackId
                + "|" + playbackTitle
                + "|" + playbackArtist
                + "|" + playbackAlbum
                + "|" + playbackAlbumArtUri
                + "|" + playbackThemeColor
                + "|" + playbackQueueIndex
                + "|" + playbackQueueSize
                + "|" + playbackPlaying
                + "|" + playbackWillPlay
                + "|" + playbackPreparing
                + "|" + playbackError
                + "|" + playbackShuffleEnabled
                + "|" + playbackRepeatMode
                + "|" + playbackHasQueue
                + "|" + sleepTimerEndAtMs
                + "|" + sleepTimerPaused
                + "|" + sleepTimerControlsVisible
                + "|" + (sleepTimerControlsVisible ? "editing" : sleepTimerMinutes)
                + "|" + activeQueuePreview.size();
    }

    private View buildExpandedPlayerContent() {
        expandedSleepTimerRemainingText = null;
        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        DragDismissLayout root = new DragDismissLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        updatePlaybackThemeColor(false);
        frame.setBackgroundColor(Color.TRANSPARENT);
        root.setBackground(expandedPlayerBackground(false));
        root.setPlayerSurfaceStyle(true);
        frame.addView(root);

        int statusInset = expandedPlayerStatusInset();
        int navigationInset = expandedPlayerNavigationInset();
        View statusScrim = new View(this);
        statusScrim.setBackgroundColor(playerStatusScrimColor());
        statusScrim.setClickable(false);
        frame.addView(statusScrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                statusInset
        ));

        ExpandedPlayerLayout layout = expandedPlayerLayout(statusInset, navigationInset);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
                layout.horizontalPadding,
                layout.topPadding + statusInset,
                layout.horizontalPadding,
                layout.bottomPadding + navigationInset
        );
        root.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        applyExpandedPlayerInsets(root, statusScrim, content, layout);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton close = playerIconButton(R.drawable.ic_keyboard_arrow_down, "플레이어 닫기", false, true);
        close.setOnClickListener(view -> {
            if (playerDialog != null) {
                playerDialog.dismiss();
            }
        });
        top.addView(close, new LinearLayout.LayoutParams(dp(44), dp(42)));
        TextView mix = text("재생 중", 13, R.color.ytet_muted, true);
        mix.setGravity(Gravity.CENTER);
        top.addView(mix, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(dp(44), dp(42)));
        content.addView(top, marginBottomPx(layout.topGap));

        content.addView(coverArtView(), coverParams(layout.coverHeight, layout.coverBottom));
        content.addView(sleepTimerStatusSlot(), marginBottomPx(layout.sleepStatusBottom));
        content.addView(marqueeText(playbackTitle, layout.titleTextSp, R.color.ytet_text, true), marginBottomPx(layout.titleBottom));
        content.addView(marqueeText(playbackArtist, 14, R.color.ytet_muted, false), marginBottomPx(layout.artistBottom));
        content.addView(marqueeText(albumQueuePositionText(), 12, R.color.ytet_muted, false), marginBottomPx(layout.albumBottom));

        PlaybackSeekBarView progress = new PlaybackSeekBarView(this);
        progress.setProgress(playbackDurationMs, playbackSeeking ? playbackSeekPreviewMs : playbackPositionMs);
        progress.setOnSeekChangeListener(new SeekChangeListener() {
            @Override
            public void onSeekStarted(long positionMs) {
                playbackSeeking = true;
                resumePlaybackAfterSeek = playbackPlaying || playbackWillPlay;
                playbackSeekPreviewMs = positionMs;
                if (resumePlaybackAfterSeek) {
                    startPlayback(PlaybackService.commandIntent(MainActivity.this, PlaybackService.ACTION_PAUSE));
                }
            }

            @Override
            public void onSeekPreview(long positionMs) {
                playbackSeekPreviewMs = positionMs;
            }

            @Override
            public void onSeekCommitted(long positionMs) {
                playbackSeeking = false;
                playbackPositionMs = positionMs;
                playbackSeekPreviewMs = positionMs;
                boolean shouldResume = resumePlaybackAfterSeek;
                resumePlaybackAfterSeek = false;
                startPlayback(PlaybackService.seekIntent(MainActivity.this, positionMs));
                if (shouldResume) {
                    startPlayback(PlaybackService.commandIntent(MainActivity.this, PlaybackService.ACTION_PLAY));
                }
                updateExpandedPlayer();
            }

            @Override
            public void onSeekCanceled() {
                playbackSeeking = false;
                boolean shouldResume = resumePlaybackAfterSeek;
                resumePlaybackAfterSeek = false;
                if (shouldResume) {
                    startPlayback(PlaybackService.commandIntent(MainActivity.this, PlaybackService.ACTION_PLAY));
                }
                updateExpandedPlayer();
            }
        });
        expandedPlaybackSeekBar = progress;
        content.addView(progress, fixedHeightParams(layout.progressHeight));
        TextView progressText = muted(playbackProgressText(), 12);
        expandedPlaybackProgressText = progressText;
        content.addView(progressText, marginBottomPx(layout.progressTextBottom));
        content.addView(new View(this), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton shuffle = playerIconButton(R.drawable.ic_shuffle, "셔플", playbackShuffleEnabled, canShuffleQueue());
        shuffle.setOnClickListener(view -> toggleShuffle());
        ImageButton previous = playerIconButton(R.drawable.ic_skip_previous, "이전 곡", false, hasPreviousTrack());
        previous.setOnClickListener(view -> previousTrack());
        ImageButton play = iconButton(playbackPlaying || playbackWillPlay ? R.drawable.ic_pause : R.drawable.ic_play_arrow,
                playbackPlaying || playbackWillPlay ? "일시정지" : "재생",
                true);
        play.setOnClickListener(view -> toggleStreamPlayback());
        ImageButton next = playerIconButton(R.drawable.ic_skip_next, "다음 곡", false, hasNextTrack());
        next.setOnClickListener(view -> nextTrack());
        ImageButton repeat = playerIconButton(
                playbackRepeatMode == PlaybackService.REPEAT_ONE ? R.drawable.ic_repeat_one : R.drawable.ic_repeat,
                repeatDescription(),
                playbackRepeatMode != PlaybackService.REPEAT_OFF,
                playbackHasQueue
        );
        repeat.setOnClickListener(view -> toggleRepeat());
        controls.addView(shuffle, playerControlParams(5, layout.controlButtonHeight));
        controls.addView(previous, playerControlParams(5, layout.controlButtonHeight));
        controls.addView(play, playerControlParams(5, layout.controlButtonHeight));
        controls.addView(next, playerControlParams(5, layout.controlButtonHeight));
        controls.addView(repeat, playerControlParams(0, layout.controlButtonHeight));
        content.addView(controls, marginBottomPx(layout.controlsBottom));

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        boolean timerSelected = hasSleepTimer();
        ImageButton timer = playerIconButton(R.drawable.ic_timer, "슬립 타이머", timerSelected, true);
        timer.setOnClickListener(view -> {
            if (sleepTimerControlsVisible) {
                sleepTimerMinutes = hasSleepTimer() ? sleepTimerDraftInitialMinutes : 0;
                sleepTimerControlsVisible = false;
            } else {
                sleepTimerMinutes = clampSleepTimerTotalMinutes(hasSleepTimer()
                        ? remainingSleepTimerMinutes()
                        : 0);
                sleepTimerDraftInitialMinutes = sleepTimerMinutes;
                sleepTimerControlsVisible = true;
            }
            updateExpandedPlayer();
        });
        tools.addView(timer, new LinearLayout.LayoutParams(layout.toolIconSize, layout.toolIconSize));
        View timerControls = sleepTimerControlsVisible ? sleepTimerControlsPanel() : nextTrackInfoPanel();
        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        );
        timerParams.setMargins(dp(8), 0, 0, 0);
        tools.addView(timerControls, timerParams);
        ImageButton queue = playerIconButton(R.drawable.ic_queue_music, "재생목록", false, playbackHasQueue);
        queue.setOnClickListener(view -> showQueueDialog());
        tools.addView(queue, new LinearLayout.LayoutParams(layout.queueIconSize, layout.toolIconSize));
        content.addView(tools, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                layout.toolsHeight
        ));
        content.addView(playerQueuePullHandle(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                layout.queueHandleHeight
        ));
        return frame;
    }

    private View playerQueuePullHandle() {
        LinearLayout handleArea = new LinearLayout(this);
        handleArea.setOrientation(LinearLayout.VERTICAL);
        handleArea.setGravity(Gravity.CENTER);
        handleArea.setPadding(0, dp(4), 0, dp(4));
        handleArea.setClickable(true);
        handleArea.setFocusable(true);

        View handle = new View(this);
        handle.setBackground(rounded(0x66FFFFFF, 3));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(42), dp(4));
        handleParams.setMargins(0, 0, 0, dp(12));
        handleArea.addView(handle, handleParams);

        TextView label = text("재생목록", 13, R.color.ytet_text, true);
        label.setGravity(Gravity.CENTER);
        handleArea.addView(label, matchWrap());

        handleArea.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                playerQueueDragStartY = event.getRawY();
                playerQueueDragTriggered = false;
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (!playerQueueDragTriggered && playerQueueDragStartY - event.getRawY() > dp(24)) {
                    playerQueueDragTriggered = true;
                    showQueueDialog();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                if (!playerQueueDragTriggered) {
                    showQueueDialog();
                }
                playerQueueDragTriggered = false;
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                playerQueueDragTriggered = false;
                return true;
            }
            return true;
        });
        return handleArea;
    }

    private View coverArtView() {
        if (playbackAlbumArtUri != null && !playbackAlbumArtUri.trim().isEmpty()) {
            if (isHttpUrl(playbackAlbumArtUri)) {
                return onlineImageView(
                        playbackAlbumArtUri,
                        playbackTitle,
                        284,
                        8,
                        1440,
                        ImageView.ScaleType.FIT_CENTER
                );
            }
            ImageView image = new ImageView(this);
            image.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setImageURI(Uri.parse(playbackAlbumArtUri));
            return image;
        }
        TextView placeholder = text(coverInitials(), 56, android.R.color.white, true);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackground(rounded(color(R.color.ytet_accent_dark), 8));
        return placeholder;
    }

    private View sleepTimerStatusSlot() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(42));

        if (!hasSleepTimer()) {
            expandedSleepTimerRemainingText = null;
            return row;
        }

        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

        TextView remaining = muted(sleepTimerInlineText(), 14);
        expandedSleepTimerRemainingText = remaining;
        remaining.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(remaining, marginRight(8, LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)));

        ImageButton toggle = playerIconButton(
                sleepTimerPaused ? R.drawable.ic_play_arrow : R.drawable.ic_pause,
                sleepTimerPaused ? "슬립 타이머 재개" : "슬립 타이머 일시정지",
                false,
                true
        );
        toggle.setPadding(dp(9), dp(9), dp(9), dp(9));
        toggle.setOnClickListener(view -> toggleSleepTimerPause());
        row.addView(toggle, marginRight(8, dp(42), dp(42)));

        ImageButton cancel = playerIconButton(R.drawable.ic_close, "슬립 타이머 취소", false, true);
        cancel.setPadding(dp(10), dp(10), dp(10), dp(10));
        cancel.setOnClickListener(view -> cancelSleepTimer());
        row.addView(cancel, new LinearLayout.LayoutParams(dp(42), dp(42)));
        return row;
    }

    private View nextTrackInfoPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(0, 0, 0, 0);

        DeviceAudioTrack nextTrack = nextQueueTrack();
        TextView label = muted("다음 곡", 11);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        panel.addView(label, marginBottom(3));

        if (nextTrack == null) {
            TextView empty = muted("재생목록 끝", 13);
            empty.setSingleLine(true);
            empty.setEllipsize(TextUtils.TruncateAt.END);
            panel.addView(empty, matchWrap());
            return panel;
        }

        TextView title = text(nextTrack.title(), 13, R.color.ytet_text, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        panel.addView(title, marginBottom(2));

        TextView meta = muted(nextTrack.artist() + " · " + MusicLibrary.formatDuration(nextTrack.durationMs()), 12);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        panel.addView(meta, matchWrap());
        return panel;
    }

    private DeviceAudioTrack nextQueueTrack() {
        if (activeQueuePreview.isEmpty() || !hasNextTrack()) {
            return null;
        }
        int nextIndex = playbackQueueIndex + 1;
        if (nextIndex >= activeQueuePreview.size() && playbackRepeatMode == PlaybackService.REPEAT_ALL) {
            nextIndex = 0;
        }
        if (nextIndex < 0 || nextIndex >= activeQueuePreview.size()) {
            return null;
        }
        return activeQueuePreview.get(nextIndex);
    }

    private String sleepTimerInlineText() {
        return MusicLibrary.formatDuration(remainingSleepTimerMs());
    }

    private String coverInitials() {
        String title = playbackTitle == null ? "" : playbackTitle.trim();
        if (title.isEmpty() || "로컬 재생 대기".equals(title)) {
            return "RY";
        }
        return title.substring(0, Math.min(2, title.length())).toUpperCase();
    }

    private View queuePreviewPanel() {
        LinearLayout panel = panel();
        panel.addView(label("재생목록"), marginBottom(10));
        if (activeQueuePreview.isEmpty()) {
            panel.addView(muted("현재 재생목록 정보는 이 화면에서 새로 재생을 시작하면 표시됩니다.", 13), matchWrap());
            return panel;
        }
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int limit = Math.min(activeQueuePreview.size(), 30);
        for (int index = 0; index < limit; index++) {
            list.addView(queueRow(activeQueuePreview.get(index), index), marginBottom(6));
        }
        if (activeQueuePreview.size() > limit) {
            list.addView(muted("외 " + (activeQueuePreview.size() - limit) + "곡", 12), matchWrap());
        }
        scroll.addView(list, matchWrap());
        panel.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        return panel;
    }

    private View queueRow(DeviceAudioTrack track, int index) {
        return queueTrackRow(track, index, index, false, true, null);
    }

    private View queueTrackRow(
            DeviceAudioTrack track,
            int index,
            int sourceIndex,
            boolean currentOverride,
            boolean clickable,
            QueueRecyclerViewHolder dragHolder
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        boolean current = currentOverride || isCurrentQueueItem(track, index, sourceIndex);
        row.setBackground(rounded(current ? selectedTrackBackgroundColor() : color(R.color.ytet_panel), 10));

        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        coverParams.setMargins(0, 0, dp(12), 0);
        row.addView(trackCoverView(track), coverParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(track.title(), 14, current ? R.color.ytet_accent : R.color.ytet_text, current);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(title, marginBottom(3));
        TextView meta = muted(track.artist() + " · " + MusicLibrary.formatDuration(track.durationMs()), 12);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(meta, matchWrap());
        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (dragHolder != null) {
            ImageButton drag = playerIconButton(R.drawable.ic_drag_handle, "재생목록 순서 변경", false, true);
            drag.setColorFilter(0x99FFFFFF);
            drag.setPadding(dp(10), dp(10), dp(10), dp(10));
            drag.setOnTouchListener((button, event) -> {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN
                        && queueItemTouchHelper != null
                        && dragHolder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    button.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    queueItemTouchHelper.startDrag(dragHolder);
                }
                return false;
            });
            row.addView(drag, new LinearLayout.LayoutParams(dp(44), dp(48)));
        }
        if (clickable) {
            row.setOnClickListener(view -> {
                int targetIndex = index;
                if (dragHolder != null && queueRecyclerAdapter != null) {
                    int adapterPosition = dragHolder.getBindingAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        targetIndex = queueRecyclerAdapter.queueIndexForAdapterPosition(adapterPosition);
                    }
                }
                playQueueTrack(track, targetIndex);
            });
        }
        return row;
    }

    private boolean isCurrentQueueItem(DeviceAudioTrack track, int queueIndex, int sourceIndex) {
        if (playbackQueueIndex >= 0) {
            return sourceIndex == playbackQueueIndex || (sourceIndex < 0 && queueIndex == playbackQueueIndex);
        }
        return track != null && track.id() == playbackTrackId;
    }

    private String queuePositionText() {
        if (playbackQueueIndex < 0 || playbackQueueSize <= 0) {
            return "";
        }
        return (playbackQueueIndex + 1) + "/" + playbackQueueSize;
    }

    private String albumQueuePositionText() {
        String album = valueOrDefault(playbackAlbum, "앨범 정보 없음");
        String position = queuePositionText();
        if (position.isEmpty()) {
            return album;
        }
        return album + " · " + position;
    }

    private String playbackProgressText() {
        return MusicLibrary.formatDuration(playbackPositionMs) + " / " + MusicLibrary.formatDuration(playbackDurationMs);
    }

    private String repeatLabel() {
        if (playbackRepeatMode == PlaybackService.REPEAT_ONE) {
            return "한곡";
        }
        if (playbackRepeatMode == PlaybackService.REPEAT_ALL) {
            return "반복";
        }
        return "반복";
    }

    private String repeatDescription() {
        if (playbackRepeatMode == PlaybackService.REPEAT_ONE) {
            return "한 곡 반복";
        }
        if (playbackRepeatMode == PlaybackService.REPEAT_ALL) {
            return "전체 반복";
        }
        return "반복 꺼짐";
    }

    private View sleepTimerControlsPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(0, 0, 0, 0);

        SleepTimerDialView hours = new SleepTimerDialView(this);
        hours.configure(MAX_SLEEP_TIMER_HOURS, 1, true);
        hours.setSelectedValue(sleepTimerHours());
        hours.setTimerActive(hasSleepTimer());
        hours.setOnTimerChangeListener((value, committed) -> setSleepTimerDraft(value, sleepTimerMinutePart()));
        panel.addView(hours, new LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.MATCH_PARENT));

        TextView colon = text(":", 26, R.color.ytet_text, true);
        colon.setGravity(Gravity.CENTER);
        colon.setIncludeFontPadding(false);
        panel.addView(colon, new LinearLayout.LayoutParams(dp(18), LinearLayout.LayoutParams.MATCH_PARENT));

        SleepTimerDialView minutes = new SleepTimerDialView(this);
        minutes.configure(59, 1, true);
        minutes.setSelectedValue(sleepTimerMinutePart());
        minutes.setTimerActive(hasSleepTimer());
        minutes.setOnTimerChangeListener((value, committed) -> setSleepTimerDraft(sleepTimerHours(), value));
        panel.addView(minutes, new LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.MATCH_PARENT));

        View leftSpacer = new View(this);
        panel.addView(leftSpacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TimerConfirmButton confirm = new TimerConfirmButton(this);
        confirm.setOnClickListener(view -> confirmSleepTimerSelection());
        panel.addView(confirm, new LinearLayout.LayoutParams(dp(58), dp(42)));

        View rightSpacer = new View(this);
        panel.addView(rightSpacer, new LinearLayout.LayoutParams(0, 1, 1f));
        return panel;
    }

    private int sleepTimerHours() {
        return clampSleepTimerTotalMinutes(sleepTimerMinutes) / 60;
    }

    private int sleepTimerMinutePart() {
        return clampSleepTimerTotalMinutes(sleepTimerMinutes) % 60;
    }

    private void setSleepTimerDraft(int hours, int minutes) {
        int totalMinutes = Math.max(0, hours) * 60 + Math.max(0, minutes);
        sleepTimerMinutes = clampSleepTimerTotalMinutes(totalMinutes);
    }

    private void confirmSleepTimerSelection() {
        applySleepTimer(sleepTimerMinutes);
        sleepTimerControlsVisible = false;
        updateExpandedPlayer();
    }

    private void applySleepTimer(int minutes) {
        minutes = clampSleepTimerTotalMinutes(minutes);
        startPlayback(PlaybackService.sleepTimerIntent(this, minutes));
        sleepTimerEndAtMs = minutes <= 0 ? 0L : System.currentTimeMillis() + minutes * 60_000L;
        sleepTimerRemainingMs = minutes <= 0 ? 0L : minutes * 60_000L;
        sleepTimerPaused = false;
    }

    private void toggleSleepTimerPause() {
        if (!hasSleepTimer()) {
            return;
        }
        startPlayback(PlaybackService.toggleSleepTimerPauseIntent(this));
        if (sleepTimerPaused) {
            sleepTimerEndAtMs = System.currentTimeMillis() + Math.max(1_000L, sleepTimerRemainingMs);
            sleepTimerPaused = false;
        } else {
            sleepTimerRemainingMs = remainingSleepTimerMs();
            sleepTimerEndAtMs = 0L;
            sleepTimerPaused = true;
        }
        updateExpandedPlayer();
    }

    private void cancelSleepTimer() {
        startPlayback(PlaybackService.sleepTimerIntent(this, 0));
        sleepTimerEndAtMs = 0L;
        sleepTimerRemainingMs = 0L;
        sleepTimerPaused = false;
        updateExpandedPlayer();
    }

    private int clampSleepTimerTotalMinutes(int minutes) {
        return Math.max(0, Math.min(MAX_SLEEP_TIMER_MINUTES, minutes));
    }

    private boolean hasSleepTimer() {
        return sleepTimerPaused || isSleepTimerActive();
    }

    private boolean isSleepTimerActive() {
        return sleepTimerEndAtMs > System.currentTimeMillis();
    }

    private long remainingSleepTimerMs() {
        if (sleepTimerPaused) {
            return Math.max(0L, sleepTimerRemainingMs);
        }
        return Math.max(0L, sleepTimerEndAtMs - System.currentTimeMillis());
    }

    private int remainingSleepTimerMinutes() {
        long remainingMs = remainingSleepTimerMs();
        if (remainingMs <= 0L) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(remainingMs / 60_000.0));
    }

    private void showQueueDialog() {
        if (queueDialog != null && queueDialog.isShowing()) {
            if (queueSheetLayout != null) {
                queueSheetLayout.setExpanded(false, true);
            }
            return;
        }
        if (queueDialog == null) {
            queueDialog = new Dialog(this);
            queueDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            queueDialog.setCancelable(false);
            queueDialog.setCanceledOnTouchOutside(false);
            queueDialog.setOnKeyListener((dialog, keyCode, event) -> {
                if (keyCode != KeyEvent.KEYCODE_BACK) {
                    return false;
                }
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    collapseOrDismissQueueDialog();
                }
                return true;
            });
            queueDialog.setOnDismissListener(dialog -> {
                queueSheetLayout = null;
                queueRecyclerAdapter = null;
                queueRecyclerView = null;
                queueItemTouchHelper = null;
                queuePlayerCover = null;
                queuePlayerTitle = null;
                queuePlayerMeta = null;
                queuePlayerShuffleButton = null;
                queuePlayerPreviousButton = null;
                queuePlayerPlayButton = null;
                queuePlayerNextButton = null;
                queuePlayerRepeatButton = null;
                queuePlayerRenderedTrackId = Long.MIN_VALUE;
                queuePlayerRenderedAlbumArtUri = "";
                queueDialogRenderedIndex = Integer.MIN_VALUE;
                queueDialogRenderedQueueSize = Integer.MIN_VALUE;
                queueDialogRenderedTrackId = Long.MIN_VALUE;
                queueDialogRenderedFingerprint = Long.MIN_VALUE;
            });
        }
        queueDialog.setContentView(buildQueueDialogContent());
        queueDialog.show();
        applyQueueWindow(queueDialog.getWindow());
        if (queueSheetLayout != null) {
            queueSheetLayout.showCollapsed();
        }
    }

    private void dismissQueueDialogAnimated() {
        if (queueDialog == null || !queueDialog.isShowing()) {
            return;
        }
        if (queueSheetLayout == null) {
            queueDialog.dismiss();
            return;
        }
        queueSheetLayout.dismissAnimated(() -> {
            if (queueDialog != null && queueDialog.isShowing()) {
                queueDialog.dismiss();
            }
        });
    }

    private void collapseOrDismissQueueDialog() {
        if (queueDialog == null || !queueDialog.isShowing()) {
            return;
        }
        if (queueSheetLayout == null) {
            queueDialog.dismiss();
            return;
        }
        queueSheetLayout.collapseOrDismissAnimated(() -> {
            if (queueDialog != null && queueDialog.isShowing()) {
                queueDialog.dismiss();
            }
        });
    }

    private void updateQueueDialog() {
        if (queueDialog != null && queueDialog.isShowing()) {
            updateQueuePlayerControls();
            bindQueueDialogState(false);
        }
    }

    private View buildQueueDialogContent() {
        queueSheetLayout = new QueueSheetLayout(this);
        queueSheetLayout.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(20), dp(8), dp(20), dp(18));
        sheet.setBackground(roundedTop(queueSheetBackgroundColor(), 20));
        sheet.setTranslationY(getResources().getDisplayMetrics().heightPixels);
        queueSheetLayout.setSheet(sheet);

        LinearLayout handleArea = new LinearLayout(this);
        handleArea.setGravity(Gravity.CENTER);
        handleArea.setPadding(0, dp(4), 0, dp(12));
        handleArea.setClickable(true);
        View handle = new View(this);
        handle.setBackground(rounded(0x55FFFFFF, 3));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(42), dp(4));
        handleArea.addView(handle, handleParams);
        sheet.addView(handleArea, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28)
        ));
        registerQueueSheetGestureTarget(handleArea);

        sheet.addView(queuePlayerControlPanel(), marginBottom(10));
        sheet.addView(queueHeaderRow(), marginBottom(12));

        RecyclerView list = new RecyclerView(this);
        queueRecyclerView = list;
        list.setClipToPadding(false);
        DefaultItemAnimator queueAnimator = new DefaultItemAnimator();
        queueAnimator.setSupportsChangeAnimations(false);
        queueAnimator.setMoveDuration(180L);
        queueAnimator.setAddDuration(120L);
        queueAnimator.setRemoveDuration(120L);
        list.setItemAnimator(queueAnimator);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setLayoutManager(new QueueLinearLayoutManager(this));
        queueRecyclerAdapter = new QueueRecyclerAdapter();
        list.setAdapter(queueRecyclerAdapter);
        queueItemTouchHelper = new ItemTouchHelper(new QueueDragCallback());
        queueItemTouchHelper.attachToRecyclerView(list);
        sheet.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        queueSheetLayout.addView(sheet, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.BOTTOM
        ));
        bindQueueDialogState(true);
        return queueSheetLayout;
    }

    private void registerQueueSheetGestureTarget(View target) {
        if (target == null || queueSheetLayout == null) {
            return;
        }
        target.setClickable(true);
        target.setFocusable(true);
        target.setOnClickListener(view -> {
            if (queueSheetLayout != null) {
                queueSheetLayout.toggleExpandedAnimated();
            }
        });
        queueSheetLayout.addDragHandle(target);
    }

    private int queueSheetBackgroundColor() {
        int base = playbackThemeColor == 0 ? color(R.color.ytet_background) : playbackThemeColor;
        return blendColors(base, color(R.color.ytet_background), 0.58f);
    }

    private View queuePlayerControlPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, dp(2), 0, dp(2));

        LinearLayout current = new LinearLayout(this);
        current.setOrientation(LinearLayout.HORIZONTAL);
        current.setGravity(Gravity.CENTER_VERTICAL);

        queuePlayerCover = new FrameLayout(this);
        current.addView(queuePlayerCover, marginRight(12, dp(46), dp(46)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);
        queuePlayerTitle = text("", 15, R.color.ytet_text, true);
        queuePlayerTitle.setSingleLine(true);
        queuePlayerTitle.setEllipsize(TextUtils.TruncateAt.END);
        queuePlayerMeta = muted("", 12);
        queuePlayerMeta.setSingleLine(true);
        queuePlayerMeta.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(queuePlayerTitle, marginBottom(2));
        copy.addView(queuePlayerMeta, matchWrap());
        current.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        registerQueueSheetGestureTarget(current);
        panel.addView(current, marginBottom(6));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        queuePlayerShuffleButton = playerIconButton(R.drawable.ic_shuffle, "셔플", playbackShuffleEnabled, canShuffleQueue());
        queuePlayerShuffleButton.setOnClickListener(view -> toggleShuffle());
        queuePlayerPreviousButton = playerIconButton(R.drawable.ic_skip_previous, "이전 곡", false, hasPreviousTrack());
        queuePlayerPreviousButton.setOnClickListener(view -> previousTrack());
        queuePlayerPlayButton = playerIconButton(
                playbackPlaying || playbackWillPlay ? R.drawable.ic_pause : R.drawable.ic_play_arrow,
                playbackPlaying || playbackWillPlay ? "일시정지" : "재생",
                true,
                true
        );
        queuePlayerPlayButton.setOnClickListener(view -> toggleStreamPlayback());
        queuePlayerNextButton = playerIconButton(R.drawable.ic_skip_next, "다음 곡", false, hasNextTrack());
        queuePlayerNextButton.setOnClickListener(view -> nextTrack());
        queuePlayerRepeatButton = playerIconButton(
                playbackRepeatMode == PlaybackService.REPEAT_ONE ? R.drawable.ic_repeat_one : R.drawable.ic_repeat,
                repeatDescription(),
                playbackRepeatMode != PlaybackService.REPEAT_OFF,
                playbackHasQueue
        );
        queuePlayerRepeatButton.setOnClickListener(view -> toggleRepeat());
        controls.addView(queuePlayerShuffleButton, playerControlParams(5, dp(44)));
        controls.addView(queuePlayerPreviousButton, playerControlParams(5, dp(44)));
        controls.addView(queuePlayerPlayButton, playerControlParams(5, dp(44)));
        controls.addView(queuePlayerNextButton, playerControlParams(5, dp(44)));
        controls.addView(queuePlayerRepeatButton, playerControlParams(0, dp(44)));
        panel.addView(controls, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
        ));
        updateQueuePlayerControls();
        return panel;
    }

    private void updateQueuePlayerControls() {
        if (queuePlayerTitle == null || queuePlayerMeta == null) {
            return;
        }
        String title = valueOrDefault(playbackTitle, "로컬 재생 대기");
        String meta = playbackPreparing || playbackError
                ? streamStatus
                : valueOrDefault(playbackArtist, miniPlaybackMeta());
        queuePlayerTitle.setText(title);
        queuePlayerMeta.setText(meta);

        String albumArtUri = valueOrDefault(playbackAlbumArtUri, "");
        if (queuePlayerCover != null
                && (queuePlayerRenderedTrackId != playbackTrackId
                || !TextUtils.equals(queuePlayerRenderedAlbumArtUri, albumArtUri))) {
            queuePlayerCover.removeAllViews();
            DeviceAudioTrack current = currentQueueTrack();
            View cover = current == null ? coverArtView() : trackCoverView(current);
            queuePlayerCover.addView(cover, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            queuePlayerRenderedTrackId = playbackTrackId;
            queuePlayerRenderedAlbumArtUri = albumArtUri;
        }

        updatePlayerIconButton(queuePlayerShuffleButton, R.drawable.ic_shuffle, "셔플", playbackShuffleEnabled, canShuffleQueue());
        updatePlayerIconButton(queuePlayerPreviousButton, R.drawable.ic_skip_previous, "이전 곡", false, hasPreviousTrack());
        updatePlayerIconButton(
                queuePlayerPlayButton,
                playbackPlaying || playbackWillPlay ? R.drawable.ic_pause : R.drawable.ic_play_arrow,
                playbackPlaying || playbackWillPlay ? "일시정지" : "재생",
                true,
                true
        );
        updatePlayerIconButton(queuePlayerNextButton, R.drawable.ic_skip_next, "다음 곡", false, hasNextTrack());
        updatePlayerIconButton(
                queuePlayerRepeatButton,
                playbackRepeatMode == PlaybackService.REPEAT_ONE ? R.drawable.ic_repeat_one : R.drawable.ic_repeat,
                repeatDescription(),
                playbackRepeatMode != PlaybackService.REPEAT_OFF,
                playbackHasQueue
        );
    }

    private DeviceAudioTrack currentQueueTrack() {
        if (activeQueuePreview.isEmpty()) {
            return null;
        }
        int currentIndex = safeQueueIndex(activeQueuePreview);
        if (currentIndex >= 0 && currentIndex < activeQueuePreview.size()) {
            return activeQueuePreview.get(currentIndex);
        }
        for (DeviceAudioTrack track : activeQueuePreview) {
            if (track != null && track.id() == playbackTrackId) {
                return track;
            }
        }
        return null;
    }

    private void updatePlayerIconButton(
            ImageButton button,
            int iconRes,
            String description,
            boolean active,
            boolean enabled
    ) {
        if (button == null) {
            return;
        }
        button.setImageResource(iconRes);
        button.setContentDescription(description);
        button.setColorFilter(enabled
                ? (active ? color(R.color.ytet_accent) : color(R.color.ytet_text))
                : color(R.color.ytet_muted));
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.62f);
    }

    private View queueHeaderRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, dp(12), 0);

        TextView title = text("재생목록", 17, R.color.ytet_text, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        FrameLayout titleTarget = new FrameLayout(this);
        titleTarget.addView(title, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL
        ));
        row.addView(titleTarget, new LinearLayout.LayoutParams(0, dp(48), 1f));
        registerQueueSheetGestureTarget(titleTarget);

        View save = queueSaveButton();
        row.addView(save, new LinearLayout.LayoutParams(dp(44), dp(48)));
        return row;
    }

    private View queueSaveButton() {
        ImageButton button = playerIconButton(R.drawable.ic_playlist_save, "현재 재생목록 저장", false, true);
        button.setContentDescription("현재 재생목록 저장");
        boolean enabled = !activeQueuePreview.isEmpty();
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.52f);
        button.setOnClickListener(view -> {
            List<DeviceAudioTrack> tracks = cleanTrackList(activeQueuePreview);
            if (tracks.isEmpty()) {
                toast("저장할 재생목록이 없습니다.");
                return;
            }
            showCreatePlaylistDialog(tracks);
        });
        return button;
    }

    private void bindQueueDialogState(boolean forceScrollToCurrent) {
        if (queueRecyclerAdapter == null) {
            return;
        }
        List<DeviceAudioTrack> queueSnapshot = new ArrayList<>(activeQueuePreview);
        int currentIndex = safeQueueIndex(queueSnapshot);
        DeviceAudioTrack current = currentIndex >= 0 && currentIndex < queueSnapshot.size()
                ? queueSnapshot.get(currentIndex)
                : null;
        long currentId = current == null ? Long.MIN_VALUE : current.id();
        long queueFingerprint = queueFingerprint(queueSnapshot);
        boolean indexChanged = queueDialogRenderedIndex != currentIndex
                || queueDialogRenderedQueueSize != queueSnapshot.size()
                || queueDialogRenderedTrackId != currentId
                || queueDialogRenderedFingerprint != queueFingerprint;
        if (!forceScrollToCurrent && !indexChanged) {
            return;
        }
        queueRecyclerAdapter.submitItems(buildQueueRecyclerItems(queueSnapshot));
        queueDialogRenderedIndex = currentIndex;
        queueDialogRenderedQueueSize = queueSnapshot.size();
        queueDialogRenderedTrackId = currentId;
        queueDialogRenderedFingerprint = queueFingerprint;
        if ((forceScrollToCurrent || indexChanged) && queueRecyclerView != null) {
            int target = queueInitialScrollPosition(queueSnapshot, currentIndex);
            RecyclerView list = queueRecyclerView;
            list.post(() -> {
                if (queueRecyclerView != list) {
                    return;
                }
                RecyclerView.LayoutManager manager = list.getLayoutManager();
                if (manager instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) manager).scrollToPositionWithOffset(target, dp(2));
                } else {
                    list.scrollToPosition(target);
                }
            });
        }
    }

    private int safeQueueIndex(List<DeviceAudioTrack> queue) {
        if (queue == null || queue.isEmpty()) {
            return -1;
        }
        if (playbackQueueIndex >= 0 && playbackQueueIndex < queue.size()) {
            return playbackQueueIndex;
        }
        for (int index = 0; index < queue.size(); index++) {
            if (queue.get(index).id() == playbackTrackId) {
                return index;
            }
        }
        return 0;
    }

    private long queueFingerprint(List<DeviceAudioTrack> queue) {
        long fingerprint = 1125899906842597L;
        if (queue == null) {
            return fingerprint;
        }
        for (DeviceAudioTrack track : queue) {
            fingerprint = fingerprint * 31L + (track == null ? 0L : track.id());
        }
        return fingerprint;
    }

    private int queueInitialScrollPosition(List<DeviceAudioTrack> queue, int currentIndex) {
        if (queue == null || queue.size() <= 1 || currentIndex < 0) {
            return 0;
        }
        return currentIndex;
    }

    private List<QueueListItem> buildQueueRecyclerItems(List<DeviceAudioTrack> queue) {
        List<QueueListItem> items = new ArrayList<>();
        if (queue == null || queue.isEmpty()) {
            items.add(QueueListItem.staticView(muted("재생목록을 불러오는 중입니다.", 14)));
            return items;
        }
        for (int index = 0; index < queue.size(); index++) {
            items.add(QueueListItem.track(queue.get(index), index));
        }
        return items;
    }

    private void commitQueueReorderFromAdapter() {
        if (queueRecyclerAdapter == null || activeQueuePreview.isEmpty()) {
            return;
        }
        int currentIndex = safeQueueIndex(activeQueuePreview);
        DeviceAudioTrack current = currentIndex >= 0 && currentIndex < activeQueuePreview.size()
                ? activeQueuePreview.get(currentIndex)
                : null;
        if (current == null) {
            return;
        }
        List<DeviceAudioTrack> reordered = queueRecyclerAdapter.orderedQueue();
        int[] orderedSourceIndices = queueRecyclerAdapter.orderedSourceIndices();
        if (reordered.size() != activeQueuePreview.size()
                || orderedSourceIndices.length != activeQueuePreview.size()
                || !queueRecyclerAdapter.hasSourceOrderChanged()) {
            return;
        }
        int nextCurrentIndex = queueRecyclerAdapter.queueIndexForSourceIndex(currentIndex);
        activeQueuePreview = new ArrayList<>(reordered);
        playbackQueueIndex = nextCurrentIndex >= 0 ? nextCurrentIndex : indexOfTrack(activeQueuePreview, current);
        playbackQueueSize = activeQueuePreview.size();
        queueRecyclerAdapter.submitItems(buildQueueRecyclerItems(activeQueuePreview));
        queueDialogRenderedIndex = playbackQueueIndex;
        queueDialogRenderedQueueSize = activeQueuePreview.size();
        queueDialogRenderedTrackId = current.id();
        queueDialogRenderedFingerprint = queueFingerprint(activeQueuePreview);
        startPlayback(PlaybackService.reorderQueueIntent(this, activeQueuePreview, orderedSourceIndices, currentIndex));
    }

    private void updateQueuePreviewFromIds(long[] ids) {
        if (ids == null) {
            return;
        }
        playbackQueueTrackIds = ids.clone();
        if (ids.length == 0) {
            activeQueuePreview = new ArrayList<>();
            return;
        }

        Map<Long, DeviceAudioTrack> byId = new HashMap<>();
        for (DeviceAudioTrack track : activeQueuePreview) {
            byId.put(track.id(), track);
        }
        List<DeviceAudioTrack> ordered = new ArrayList<>();
        boolean hasAllTracks = true;
        for (long id : ids) {
            DeviceAudioTrack track = byId.get(id);
            if (track == null) {
                hasAllTracks = false;
                break;
            }
            ordered.add(track);
        }
        if (hasAllTracks) {
            activeQueuePreview = ordered;
            return;
        }
        loadQueuePreview(ids);
    }

    private void loadQueuePreview(long[] ids) {
        if (queuePreviewLoading || ids.length == 0) {
            return;
        }
        queuePreviewLoading = true;
        long[] requestedIds = ids.clone();
        libraryExecutor.execute(() -> {
            try {
                List<DeviceAudioTrack> tracks = deviceMusicLibrary.loadTracksByIds(this, requestedIds);
                runOnUiThread(() -> {
                    queuePreviewLoading = false;
                    if (!Arrays.equals(playbackQueueTrackIds, requestedIds)) {
                        return;
                    }
                    activeQueuePreview = tracks;
                    updateExpandedPlayer();
                    updateQueueDialog();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> queuePreviewLoading = false);
            }
        });
    }

    private void closeAfterSleepTimer() {
        if (queueDialog != null) {
            queueDialog.dismiss();
        }
        if (playerDialog != null) {
            playerDialog.dismiss();
        }
        toast("슬립 타이머로 재생을 종료했습니다.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    private void startHomeRefresh(boolean renderImmediately) {
        if (!hasAudioPermission()) {
            requestAudioPermission();
            return;
        }
        if (homeLoading) {
            return;
        }
        homeLoading = true;
        homeStatus = "";
        if (renderImmediately && currentTab == Tab.HOME) {
            renderCurrentTab();
        }
        libraryExecutor.execute(() -> {
            try {
                migrateLegacyDefaultMediaFolders();
                List<DeviceAudioTrack> tracks = deviceMusicLibrary.loadTracks(
                        this,
                        DefaultMediaPaths.musicRelativePaths()
                );
                runOnUiThread(() -> {
                    homeTracks = tracks;
                    homeLoaded = true;
                    homeLoading = false;
                    homeStatus = tracks.isEmpty()
                            ? "보관함에 음악이 없습니다. 추출한 음원은 기본적으로 "
                            + DefaultMediaPaths.displayPath(MediaType.AUDIO)
                            + "에 저장됩니다."
                            : "";
                    renderLibraryDependentTabs();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    homeLoaded = true;
                    homeLoading = false;
                    homeTracks = new ArrayList<>();
                    homeStatus = "보관함 스캔 실패: " + exception.getMessage();
                    renderLibraryDependentTabs();
                });
            }
        });
    }

    private void startStreamRefresh(boolean renderImmediately) {
        if (streamLoading) {
            return;
        }
        streamLoading = true;
        streamLoadStatus = "";
        if (renderImmediately && currentTab == Tab.HOME) {
            renderCurrentTab();
        }
        streamExecutor.execute(() -> {
            try {
                List<OnlineStreamSection> sections = OnlineStreamClient.loadSections(
                        this,
                        OnlineStreamCatalog.defaultChannels(),
                        40
                );
                runOnUiThread(() -> {
                    streamSections = sections;
                    focusedStreamSection = updatedFocusedStreamSection(sections);
                    streamLoaded = true;
                    streamLoading = false;
                    streamLoadStatus = sections.isEmpty()
                            ? "온라인 추천을 불러오지 못했습니다."
                            : "";
                    prefetchOnlineStreamSections(sections);
                    if (currentTab == Tab.HOME) {
                        renderCurrentTab();
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    streamLoaded = true;
                    streamLoading = false;
                    streamLoadStatus = "온라인 추천을 불러오지 못했습니다: " + safeMessage(exception);
                    if (currentTab == Tab.HOME) {
                        renderCurrentTab();
                    }
                });
            }
        });
    }

    private void prefetchOnlineStreamSections(List<OnlineStreamSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        int scheduled = 0;
        for (OnlineStreamSection section : sections) {
            List<OnlineStreamVideo> videos = section == null ? new ArrayList<>() : section.videos();
            int sectionCount = 0;
            for (OnlineStreamVideo video : videos) {
                if (scheduleOnlineStreamPrefetch(video)) {
                    scheduled++;
                    sectionCount++;
                }
                if (scheduled >= 10 || sectionCount >= 2) {
                    break;
                }
            }
            if (scheduled >= 10) {
                return;
            }
        }
    }

    private void prefetchOnlineStreamVideos(List<OnlineStreamVideo> videos, int limit) {
        if (videos == null || videos.isEmpty() || limit <= 0) {
            return;
        }
        int scheduled = 0;
        for (OnlineStreamVideo video : videos) {
            if (scheduleOnlineStreamPrefetch(video)) {
                scheduled++;
            }
            if (scheduled >= limit) {
                return;
            }
        }
    }

    private boolean scheduleOnlineStreamPrefetch(OnlineStreamVideo video) {
        if (video == null) {
            return false;
        }
        String url = video.watchUrl() == null ? "" : video.watchUrl().trim();
        if (url.isEmpty() || !streamResolvePrefetchUrls.add(url)) {
            return false;
        }
        Context appContext = getApplicationContext();
        streamResolveExecutor.execute(() -> {
            try {
                OnlineStreamResolver.resolve(appContext, url);
            } catch (Exception ignored) {
                streamResolvePrefetchUrls.remove(url);
            }
        });
        return true;
    }

    private OnlineStreamSection updatedFocusedStreamSection(List<OnlineStreamSection> sections) {
        if (focusedStreamSection == null || sections == null) {
            return focusedStreamSection;
        }
        for (OnlineStreamSection section : sections) {
            if (TextUtils.equals(section.channelId(), focusedStreamSection.channelId())) {
                return section;
            }
        }
        return null;
    }

    private void startLibraryRefresh(boolean renderImmediately) {
        if (!hasAudioPermission()) {
            requestAudioPermission();
            return;
        }
        libraryLoading = true;
        libraryStatus = "";
        beginLibraryPullRefreshVisual();
        if (renderImmediately && (currentTab == Tab.HOME || currentTab == Tab.LIBRARY)) {
            renderCurrentTab();
        }
        libraryExecutor.execute(() -> {
            try {
                migrateLegacyDefaultMediaFolders();
                List<DeviceAudioTrack> tracks = deviceMusicLibrary.loadTracks(this, libraryScanRelativePaths());
                runOnUiThread(() -> {
                    libraryTracks = tracks;
                    librarySearchIndex.clear();
                    markLibraryDataChanged();
                    libraryLoaded = true;
                    libraryLoading = false;
                    finishLibraryPullRefreshVisual();
                    libraryStatus = tracks.isEmpty()
                            ? emptyLibraryStatus()
                            : "";
                    renderLibraryDependentTabs();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    libraryLoaded = true;
                    libraryLoading = false;
                    libraryTracks = new ArrayList<>();
                    librarySearchIndex.clear();
                    markLibraryDataChanged();
                    selectedTrack = null;
                    finishLibraryPullRefreshVisual();
                    libraryStatus = "스캔 실패: " + exception.getMessage();
                    renderLibraryDependentTabs();
                });
            }
        });
    }

    private List<String> libraryScanRelativePaths() {
        if (isDeviceFileSource()) {
            return null;
        }
        return DefaultMediaPaths.musicRelativePaths();
    }

    private void migrateLegacyDefaultMediaFolders() {
        try {
            new StorageWriter().migrateLegacyDefaultFolders(this);
        } catch (Exception ignored) {
            // Legacy paths stay in the scan list if the device does not allow moving those files.
        }
    }

    private String emptyLibraryStatus() {
        if (isDeviceFileSource()) {
            return "기기 파일에서 음악 파일을 찾지 못했습니다.";
        }
        return "보관함에 음악이 없습니다. 추출한 음원은 기본적으로 "
                + DefaultMediaPaths.displayPath(MediaType.AUDIO)
                + "에 저장됩니다.";
    }

    private String emptyLibraryHint() {
        if (isDeviceFileSource()) {
            return "기기에 음악을 추가하거나 화면 맨 위에서 아래로 당겨 다시 스캔하세요.";
        }
        return "추출기에서 음원을 저장하면 "
                + DefaultMediaPaths.displayPath(MediaType.AUDIO)
                + " 보관함에 표시됩니다. 상단에서 기기 파일로 전환하면 전체 음악을 볼 수 있습니다.";
    }

    private void renderLibraryDependentTabs() {
        if (currentTab == Tab.HOME || currentTab == Tab.LIBRARY) {
            renderCurrentTab();
        }
    }

    private String homeSummary() {
        int folderCount = Math.max(0, MusicLibrary.folderNames(homeTracks).size() - 1);
        return folderCount + "개 보관함 폴더 · " + homeTracks.size() + "개 파일";
    }

    private boolean hasAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_AUDIO_LIBRARY);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_AUDIO_LIBRARY);
        }
    }

    private void openSelectedTrack() {
        if (selectedTrack == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(selectedTrack.contentUri()), "audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "음악 열기"));
    }

    private void shareSelectedTrack() {
        if (selectedTrack == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(selectedTrack.contentUri()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "음악 공유"));
    }

    private void deleteSelectedTrack() {
        if (selectedTrack == null) {
            return;
        }
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        tracks.add(selectedTrack);
        deleteTracks(tracks);
    }

    private void deleteTracks(List<DeviceAudioTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return;
        }
        List<DeviceAudioTrack> targets = new ArrayList<>();
        List<Uri> uris = new ArrayList<>();
        for (DeviceAudioTrack track : tracks) {
            if (track == null || track.contentUri().trim().isEmpty()) {
                continue;
            }
            targets.add(track);
            uris.add(Uri.parse(track.contentUri()));
        }
        if (targets.isEmpty()) {
            toast("삭제할 파일이 없습니다.");
            return;
        }
        pendingDeleteTracks = targets;
        pendingDeleteTrack = targets.get(0);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), uris);
                startIntentSenderForResult(
                        pendingIntent.getIntentSender(),
                        REQUEST_DELETE_AUDIO,
                        null,
                        0,
                        0,
                        0
                );
                return;
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_LIBRARY);
                return;
            }
            deleteTracksDirectly(targets);
        } catch (IntentSender.SendIntentException exception) {
            toast("삭제 확인 화면을 열 수 없습니다.");
        } catch (SecurityException exception) {
            toast("이 파일을 삭제할 권한이 없습니다.");
        }
    }

    private void handleWritePermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && !pendingDeleteTracks.isEmpty()) {
            deleteTracksDirectly(pendingDeleteTracks);
        } else {
            toast("파일 삭제에는 저장소 쓰기 권한이 필요합니다.");
        }
    }

    private void deleteTrackDirectly(DeviceAudioTrack track) {
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        tracks.add(track);
        deleteTracksDirectly(tracks);
    }

    private void deleteTracksDirectly(List<DeviceAudioTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return;
        }
        int deletedCount = 0;
        try {
            for (DeviceAudioTrack track : tracks) {
                if (track == null || track.contentUri().trim().isEmpty()) {
                    continue;
                }
                pendingDeleteTrack = track;
                Uri uri = Uri.parse(track.contentUri());
                int deleted = getContentResolver().delete(uri, null, null);
                if (deleted > 0) {
                    deletedCount += deleted;
                }
            }
            toast(deletedCount > 0
                    ? deletedCount + "개 파일을 삭제했습니다."
                    : "삭제할 수 없는 파일입니다.");
            selectedTrack = null;
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
            focusedParentArtistGroup = null;
            pendingDeleteTrack = null;
            pendingDeleteTracks = new ArrayList<>();
            startLibraryRefresh(true);
        } catch (SecurityException exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && exception instanceof RecoverableSecurityException) {
                requestRecoverableDelete((RecoverableSecurityException) exception);
                return;
            }
            toast("이 파일을 삭제할 권한이 없습니다.");
        }
    }

    private void requestRecoverableDelete(RecoverableSecurityException exception) {
        try {
            startIntentSenderForResult(
                    exception.getUserAction().getActionIntent().getIntentSender(),
                    REQUEST_DELETE_AUDIO,
                    null,
                    0,
                    0,
                    0
            );
        } catch (IntentSender.SendIntentException intentException) {
            toast("삭제 확인 화면을 열 수 없습니다.");
        }
    }

    private void handleOutputTreeResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        int flags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
            // Some providers grant temporary access only; extraction can still use it in this session.
        }
        String selectedTreeUri = uri.toString();
        if (DefaultMediaPaths.isDefaultTreeUriFor(selectedMediaType(), selectedTreeUri)) {
            outputTreeUri = null;
            getPreferences().edit().remove(PREF_OUTPUT_TREE).apply();
            toast("기본 저장 폴더를 사용합니다.");
        } else {
            outputTreeUri = selectedTreeUri;
            getPreferences().edit().putString(PREF_OUTPUT_TREE, outputTreeUri).apply();
        }
        updateFolderLabel();
    }

    private void resetOutputFolder() {
        outputTreeUri = null;
        getPreferences().edit().remove(PREF_OUTPUT_TREE).apply();
        updateFolderLabel();
        renderCurrentTab();
    }

    private void normalizeSavedOutputTree() {
        if (outputTreeUri == null || outputTreeUri.trim().isEmpty()) {
            return;
        }
        if (DefaultMediaPaths.isDefaultTreeUriFor(MediaType.AUDIO, outputTreeUri)
                || DefaultMediaPaths.isDefaultTreeUriFor(MediaType.VIDEO, outputTreeUri)) {
            outputTreeUri = null;
            getPreferences().edit().remove(PREF_OUTPUT_TREE).apply();
        }
    }

    private String progressStatus(String stage, String message) {
        String safeStage = stage == null || stage.trim().isEmpty() ? "진행 중" : stage.trim();
        if (message == null || message.trim().isEmpty()) {
            return safeStage;
        }
        return safeStage + " · " + message.trim();
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String safeMessage(Exception exception) {
        String message = exception == null ? null : exception.getMessage();
        return message == null || message.trim().isEmpty() ? "알 수 없는 오류" : message.trim();
    }

    private void updateModeOptions() {
        if (optionSpinner == null || subtitlesCheck == null || playlistCheck == null || metadataEnhanceCheck == null) {
            return;
        }
        boolean isVideo = selectedMediaType() == MediaType.VIDEO;
        String[] labels = isVideo
                ? VideoQuality.labels()
                : new String[]{AudioFormat.M4A.label(), AudioFormat.ORIGINAL.label()};
        ArrayAdapter<String> adapter = darkSpinnerAdapter(labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        optionSpinner.setAdapter(adapter);
        subtitlesCheck.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        playlistCheck.setVisibility(isVideo ? View.GONE : View.VISIBLE);
        playlistCheck.setEnabled(!extractionBusy && !isVideo);
        metadataEnhanceCheck.setVisibility(isVideo ? View.GONE : View.VISIBLE);
        metadataEnhanceCheck.setEnabled(!extractionBusy && !isVideo);
        updateFolderLabel();
    }

    private ArrayAdapter<String> darkSpinnerAdapter(String[] labels) {
        return new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                styleSpinnerText(view);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                styleSpinnerText(view);
                view.setBackgroundColor(color(R.color.ytet_panel_alt));
                return view;
            }
        };
    }

    private void styleSpinnerText(TextView view) {
        view.setTextColor(color(R.color.ytet_text));
        view.setTextSize(15);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(14), 0, dp(14), 0);
    }

    private void chooseOutputFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_OUTPUT_TREE);
    }

    private void startExtraction() {
        saveExtractorInputs();

        MediaType mediaType = selectedMediaType();
        String option = selectedOption(mediaType);
        ExtractionRequest request;
        try {
            request = new ExtractionRequest(
                    urlInput.getText().toString(),
                    extractionOutputUri(),
                    mediaType,
                    option,
                    mediaType == MediaType.VIDEO && subtitlesCheck.isChecked(),
                    mediaType == MediaType.AUDIO && playlistCheck.isChecked(),
                    mediaType == MediaType.AUDIO && metadataEnhanceCheck.isChecked()
            );
        } catch (IllegalArgumentException exception) {
            toast(exception.getMessage());
            return;
        }

        if (!hasNotificationPermission()) {
            extractionPendingNotificationPermission = true;
            showExtractionNotificationPermissionRationale();
            return;
        }

        extractionPercent = 0;
        extractionStatus = "작업을 준비하는 중";
        extractionResult = "-";
        extractionBusy = true;
        extractionCancelRequested = false;
        applyExtractionStateToViews();
        setBusy(true);

        Intent intent = new Intent(this, ExtractionService.class);
        request.writeTo(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void cancelExtraction() {
        if (!extractionBusy || extractionCancelRequested) {
            return;
        }
        extractionCancelRequested = true;
        extractionStatus = "취소 요청 중";
        extractionResult = "진행 중인 추출을 중단하는 중입니다.";
        applyExtractionStateToViews();
        try {
            startService(ExtractionService.cancelIntent(this));
        } catch (Exception exception) {
            extractionCancelRequested = false;
            toast("취소 요청을 보낼 수 없습니다.");
            applyExtractionStateToViews();
        }
    }

    private String extractionOutputUri() {
        return outputTreeUri == null || outputTreeUri.trim().isEmpty()
                ? DefaultMediaPaths.DEFAULT_OUTPUT_URI
                : outputTreeUri;
    }

    private MediaType selectedMediaType() {
        if (mediaGroup == null || videoRadio == null) {
            return MediaType.AUDIO;
        }
        return mediaGroup.getCheckedRadioButtonId() == videoRadio.getId() ? MediaType.VIDEO : MediaType.AUDIO;
    }

    private String selectedOption(MediaType mediaType) {
        Object selected = optionSpinner == null ? null : optionSpinner.getSelectedItem();
        String label = selected == null ? "" : selected.toString();
        if (mediaType == MediaType.VIDEO) {
            return VideoQuality.fromLabel(label).value();
        }
        return AudioFormat.fromLabel(label).value();
    }

    private void setBusy(boolean busy) {
        if (urlInput == null) {
            return;
        }
        urlInput.setEnabled(!busy);
        urlInput.setFocusable(!busy);
        urlInput.setFocusableInTouchMode(!busy);
        urlInput.setLongClickable(!busy);
        if (busy) {
            urlInput.clearFocus();
            urlInput.setCursorVisible(false);
        }
        mediaGroup.setEnabled(!busy);
        audioRadio.setEnabled(!busy);
        videoRadio.setEnabled(!busy);
        optionSpinner.setEnabled(!busy);
        subtitlesCheck.setEnabled(!busy);
        playlistCheck.setEnabled(!busy && selectedMediaType() == MediaType.AUDIO);
        metadataEnhanceCheck.setEnabled(!busy && selectedMediaType() == MediaType.AUDIO);
        chooseFolderButton.setEnabled(!busy);
        resetOutputButton.setEnabled(!busy);
        extractButton.setEnabled(!busy);
        if (cancelExtractButton != null) {
            cancelExtractButton.setVisibility(busy ? View.VISIBLE : View.GONE);
            cancelExtractButton.setEnabled(busy && !extractionCancelRequested);
        }
    }

    private void saveCurrentTabInputs() {
        if (currentTab == Tab.EXTRACTOR) {
            saveExtractorInputs();
        } else if (currentTab == Tab.LIBRARY) {
            flushLibrarySearchInput();
            hideKeyboard(librarySearchInput);
        }
    }

    private void saveExtractorInputs() {
        if (urlInput != null) {
            extractorUrl = urlInput.getText().toString();
        }
        if (mediaGroup != null) {
            extractorMediaType = selectedMediaType();
        }
        if (optionSpinner != null) {
            extractorOption = selectedOption(extractorMediaType);
        }
        if (subtitlesCheck != null) {
            extractorIncludeSubtitles = subtitlesCheck.isChecked();
        }
        if (playlistCheck != null) {
            extractorIncludePlaylist = playlistCheck.isChecked();
        }
        if (metadataEnhanceCheck != null) {
            extractorEnhanceMetadata = metadataEnhanceCheck.isChecked();
        }
    }

    private boolean applySharedUrlIntent(Intent intent, boolean renderImmediately) {
        String sharedUrl = extractSharedVideoUrl(intent);
        if (sharedUrl == null || sharedUrl.trim().isEmpty()) {
            return false;
        }
        showExtractorWithUrl(sharedUrl, renderImmediately);
        return true;
    }

    private String extractSharedVideoUrl(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) {
            return null;
        }
        String type = intent.getType();
        if (type != null && !type.startsWith("text/")) {
            return null;
        }
        CharSequence sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (sharedText == null) {
            sharedText = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT);
        }
        return extractFirstSupportedVideoUrl(sharedText == null ? "" : sharedText.toString());
    }

    private String extractFirstSupportedVideoUrl(String textValue) {
        if (textValue == null || textValue.trim().isEmpty()) {
            return null;
        }
        String[] tokens = textValue.split("\\s+");
        for (String token : tokens) {
            String candidate = cleanSharedUrlToken(token);
            if (isSupportedVideoUrl(candidate)) {
                return candidate;
            }
        }
        String cleaned = cleanSharedUrlToken(textValue);
        return isSupportedVideoUrl(cleaned) ? cleaned : null;
    }

    private String cleanSharedUrlToken(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        while (!cleaned.isEmpty()) {
            char last = cleaned.charAt(cleaned.length() - 1);
            if (last == ')' || last == ']' || last == '}' || last == ',' || last == '.') {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            } else {
                break;
            }
        }
        return cleaned;
    }

    private boolean isSupportedVideoUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.US);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        for (String marker : SUPPORTED_VIDEO_URL_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private void selectOption(MediaType mediaType, String optionValue) {
        if (optionSpinner == null) {
            return;
        }
        String label = mediaType == MediaType.VIDEO
                ? VideoQuality.fromValue(optionValue).label()
                : AudioFormat.fromValue(optionValue).label();
        for (int index = 0; index < optionSpinner.getCount(); index++) {
            Object item = optionSpinner.getItemAtPosition(index);
            if (item != null && label.equals(item.toString())) {
                optionSpinner.setSelection(index);
                return;
            }
        }
    }

    private void applyExtractionStateToViews() {
        if (progressBar != null) {
            progressBar.setProgress(extractionPercent);
        }
        if (statusText != null) {
            statusText.setText(extractionStatus);
        }
        if (resultText != null) {
            resultText.setText(extractionResult);
        }
        setBusy(extractionBusy);
        updateExtractorScrollMode();
    }

    private void updateExtractorScrollMode() {
        if (appContentScrollView == null) {
            return;
        }
        if (currentTab != Tab.EXTRACTOR) {
            appContentScrollView.setTabScrollEnabled(true);
            return;
        }
        appContentScrollView.post(() -> {
            if (currentTab != Tab.EXTRACTOR || appContentScrollView == null) {
                return;
            }
            View child = appContentScrollView.getChildCount() == 0
                    ? null
                    : appContentScrollView.getChildAt(0);
            int childHeight = child == null ? 0 : Math.max(0, child.getHeight() - child.getPaddingBottom());
            int viewportHeight = Math.max(0,
                    appContentScrollView.getHeight()
                            - appContentScrollView.getPaddingTop()
                            - appContentScrollView.getPaddingBottom());
            appContentScrollView.setTabScrollEnabled(childHeight > viewportHeight + dp(8));
        });
    }

    private void updateFolderLabel() {
        if (folderText == null) {
            return;
        }
        folderText.setText(outputPathLabel(selectedMediaType()));
    }

    private String outputPathLabel(MediaType mediaType) {
        if (outputTreeUri == null || outputTreeUri.trim().isEmpty()) {
            return "기본 저장: " + DefaultMediaPaths.displayPath(mediaType);
        }
        return "사용자 지정 저장: " + DefaultMediaPaths.displayTreePath(outputTreeUri);
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void showExtractionNotificationPermissionRationale() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("추출 알림 허용")
                .setMessage("추출은 앱을 나가도 백그라운드에서 계속 진행됩니다. 진행률과 성공/실패 결과를 알림바에서 확인하려면 알림 권한이 필요합니다.")
                .setPositiveButton("권한 허용", (dialog, which) ->
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS))
                .setNegativeButton("취소", (dialog, which) -> {
                    extractionPendingNotificationPermission = false;
                    extractionStatus = "대기 중";
                    extractionResult = "추출을 시작하려면 알림 권한을 허용하세요.";
                    applyExtractionStateToViews();
                })
                .show();
    }

    private void showUpdateNotificationPermissionRationale() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("업데이트 알림 허용")
                .setMessage("업데이트 APK 다운로드는 앱을 나가도 계속 진행됩니다. 진행률과 다운로드 완료 후 설치 요청을 알림바에서 확인하려면 알림 권한이 필요합니다.")
                .setPositiveButton("권한 허용", (dialog, which) ->
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS))
                .setNegativeButton("취소", (dialog, which) -> {
                    updatePendingNotificationPermission = false;
                    updateStatus = "업데이트를 다운로드하려면 알림 권한을 허용하세요.";
                    renderUpdateState();
                })
                .show();
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private LinearLayout screenRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(34));
        return root;
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(rounded(color(R.color.ytet_panel), 8));
        return panel;
    }

    private LinearLayout topAlignedPanel() {
        LinearLayout panel = panel();
        panel.setPadding(dp(16), dp(10), dp(16), dp(16));
        return panel;
    }

    private void addTopVisualAlignmentSpacer(LinearLayout root) {
        root.addView(new View(this), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10)
        ));
    }

    private TextView title(String text) {
        return text(text, 30, R.color.ytet_text, true);
    }

    private TextView sectionTitle(String text) {
        return text(text, 19, R.color.ytet_text, true);
    }

    private TextView label(String text) {
        return text(text, 15, R.color.ytet_text, true);
    }

    private TextView muted(String text, int sizeSp) {
        return text(text, sizeSp, R.color.ytet_muted, false);
    }

    private TextView marqueeText(String text, int sizeSp, int colorRes, boolean bold) {
        TextView view = text(text, sizeSp, colorRes, bold);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        view.setMarqueeRepeatLimit(-1);
        view.setHorizontallyScrolling(true);
        view.setHorizontalFadingEdgeEnabled(true);
        view.setFadingEdgeLength(dp(18));
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setSelected(true);
        return view;
    }

    private TextView text(String text, int sizeSp, int colorRes, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color(colorRes));
        view.setTextSize(sizeSp);
        view.setIncludeFontPadding(true);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private Button primaryButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(0xFFFFFFFF);
        button.setBackground(rounded(color(R.color.ytet_accent), 8));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(color(R.color.ytet_text));
        button.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
        return button;
    }

    private View actionButtonWithIcon(String text, int iconRes, boolean primary) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setClipChildren(false);
        button.setClipToPadding(false);
        button.setClickable(true);
        button.setFocusable(true);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rounded(primary ? color(R.color.ytet_accent) : color(R.color.ytet_panel_alt), 8));
        int textColor = primary ? 0xFFFFFFFF : color(R.color.ytet_text);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER);
        content.setClipChildren(false);
        content.setClipToPadding(false);

        Drawable icon = getDrawable(iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(textColor);
            ImageView iconView = new ImageView(this);
            iconView.setImageDrawable(icon);
            iconView.setTranslationX(-dp(6));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
            iconParams.setMargins(0, 0, dp(8), 0);
            content.addView(iconView, iconParams);
        }

        TextView label = text(text, 15, primary ? android.R.color.white : R.color.ytet_text, true);
        label.setGravity(Gravity.CENTER);
        content.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        button.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return button;
    }

    private Button detailActionButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(color(R.color.ytet_text));
        button.setBackground(roundedStroke(0x55000000, 0x33FFFFFF, 12, 1));
        return button;
    }

    private Button dangerButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(0xFFFFFFFF);
        button.setBackground(rounded(color(R.color.ytet_accent_dark), 8));
        return button;
    }

    private Button compactButton(String text) {
        Button button = baseButton(text);
        button.setTextSize(12);
        button.setTextColor(color(R.color.ytet_text));
        button.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
        return button;
    }

    private Button playerControlButton(String text, boolean selected) {
        Button button = baseButton(text);
        button.setTextSize(12);
        button.setTextColor(selected ? 0xFFFFFFFF : color(R.color.ytet_text));
        button.setBackground(rounded(selected ? color(R.color.ytet_accent) : color(R.color.ytet_panel_alt), 8));
        return button;
    }

    private ImageButton iconButton(int iconRes, String description, boolean selected) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setContentDescription(description);
        button.setColorFilter(selected ? Color.WHITE : color(R.color.ytet_text));
        button.setBackground(rounded(selected ? color(R.color.ytet_accent) : color(R.color.ytet_panel_alt), 8));
        button.setPadding(dp(11), dp(11), dp(11), dp(11));
        button.setScaleType(ImageView.ScaleType.CENTER);
        return button;
    }

    private ImageButton playerIconButton(int iconRes, String description, boolean active, boolean enabled) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setContentDescription(description);
        button.setColorFilter(enabled
                ? (active ? color(R.color.ytet_accent) : color(R.color.ytet_text))
                : color(R.color.ytet_muted));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(11), dp(11), dp(11), dp(11));
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.62f);
        return button;
    }

    private ImageButton toolbarIconButton(int iconRes, String description, boolean selected) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setContentDescription(description);
        button.setColorFilter(selected ? color(R.color.ytet_accent) : color(R.color.ytet_text));
        button.setBackground(rounded(color(R.color.ytet_background), 8));
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setScaleType(ImageView.ScaleType.CENTER);
        return button;
    }

    private Button toolbarTextButton(String text) {
        Button button = baseButton(text);
        button.setTextColor(color(R.color.ytet_text));
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setBackground(rounded(color(R.color.ytet_background), 8));
        return button;
    }

    private Button baseButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        return button;
    }

    private void styleInput(EditText input) {
        input.setTextColor(color(R.color.ytet_text));
        input.setHintTextColor(color(R.color.ytet_muted));
        input.setSingleLine(true);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
    }

    private void styleUrlInput(EditText input) {
        styleInput(input);
        input.setTextSize(14);
        input.setSelectAllOnFocus(false);
        input.setHorizontallyScrolling(true);
        input.setHorizontalFadingEdgeEnabled(true);
        input.setFadingEdgeLength(dp(24));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setPadding(dp(10), 0, dp(10), 0);
        input.setCursorVisible(false);
        input.setOnClickListener(view -> {
            input.requestFocus();
            input.setCursorVisible(true);
            showKeyboard(input);
        });
        input.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                input.setCursorVisible(false);
            }
        });
    }

    private GradientDrawable rounded(int fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable roundedTop(int fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        float radius = dp(radiusDp);
        drawable.setCornerRadii(new float[]{
                radius, radius,
                radius, radius,
                0f, 0f,
                0f, 0f
        });
        return drawable;
    }

    private GradientDrawable roundedStroke(int fillColor, int strokeColor, int radiusDp, int strokeDp) {
        GradientDrawable drawable = rounded(fillColor, radiusDp);
        drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private void updateTabStyles() {
        styleTab(homeTabButton, currentTab == Tab.HOME);
        styleTab(libraryTabButton, currentTab == Tab.LIBRARY);
        styleTab(extractorTabButton, currentTab == Tab.EXTRACTOR);
    }

    private void styleTab(TabItem tab, boolean selected) {
        if (tab == null) {
            return;
        }
        tab.icon.setImageResource(selected ? tab.filledIcon : tab.outlineIcon);
        tab.label.setTextColor(selected ? 0xFFFFFFFF : color(R.color.ytet_muted));
        tab.root.setBackgroundColor(Color.TRANSPARENT);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams marginBottom(int bottomDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, dp(bottomDp));
        return params;
    }

    private LinearLayout.LayoutParams marginBottomPx(int bottomPx) {
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, Math.max(0, bottomPx));
        return params;
    }

    private LinearLayout.LayoutParams marginRight(int rightDp, int width, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(0, 0, dp(rightDp), 0);
        return params;
    }

    private LinearLayout.LayoutParams weightedButtonParams(int rightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
        params.setMargins(0, 0, dp(rightDp), 0);
        return params;
    }

    private LinearLayout.LayoutParams fixedButtonParams(int widthDp, int heightDp, int rightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(widthDp), dp(heightDp));
        params.setMargins(0, 0, dp(rightDp), 0);
        return params;
    }

    private LinearLayout.LayoutParams weightedControlParams(int weight, int rightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), weight);
        params.setMargins(0, 0, dp(rightDp), 0);
        return params;
    }

    private LinearLayout.LayoutParams cardColumnParams(int rightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.setMargins(0, 0, dp(rightDp), 0);
        return params;
    }

    private LinearLayout.LayoutParams playerControlParams(int rightDp) {
        return playerControlParams(rightDp, dp(50));
    }

    private LinearLayout.LayoutParams playerControlParams(int rightDp, int heightPx) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Math.max(dp(40), heightPx), 1f);
        params.setMargins(0, 0, dp(rightDp), 0);
        return params;
    }

    private LinearLayout.LayoutParams coverParams(int heightPx, int bottomPx) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(dp(96), heightPx)
        );
        params.setMargins(0, 0, 0, Math.max(0, bottomPx));
        return params;
    }

    private LinearLayout.LayoutParams controlParams(int heightDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(heightDp)
        );
        params.setMargins(0, 0, 0, dp(bottomDp));
        return params;
    }

    private LinearLayout.LayoutParams fixedHeightParams(int heightPx) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, heightPx)
        );
    }

    private LinearLayout.LayoutParams tabParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private RadioGroup.LayoutParams radioParams() {
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                0,
                RadioGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.gravity = Gravity.CENTER_VERTICAL;
        return params;
    }

    private int color(int resId) {
        return getColor(resId);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private final class LibraryRecyclerAdapter extends RecyclerView.Adapter<LibraryRecyclerViewHolder> {
        private final List<LibraryListItem> items = new ArrayList<>();

        void submitItems(List<LibraryListItem> nextItems) {
            items.clear();
            if (nextItems != null) {
                items.addAll(nextItems);
            }
            notifyDataSetChanged();
        }

        LibraryListItem firstItem() {
            return items.isEmpty() ? null : items.get(0);
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= items.size()) {
                return LibraryListItem.TYPE_STATIC;
            }
            return items.get(position).type;
        }

        @Override
        public LibraryRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            FrameLayout container = new FrameLayout(parent.getContext());
            container.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            ));
            return new LibraryRecyclerViewHolder(container);
        }

        @Override
        public void onBindViewHolder(LibraryRecyclerViewHolder holder, int position) {
            LibraryListItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public void onViewRecycled(LibraryRecyclerViewHolder holder) {
            holder.clear();
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private final class LibraryRecyclerViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout container;
        private long boundTrackId = Long.MIN_VALUE;

        LibraryRecyclerViewHolder(FrameLayout container) {
            super(container);
            this.container = container;
        }

        void bind(LibraryListItem item) {
            clear();
            applyLibraryItemMargins(item);
            View view;
            switch (item.type) {
                case LibraryListItem.TYPE_TRACK_ROW:
                    view = trackRow(item.track);
                    boundTrackId = item.track == null ? Long.MIN_VALUE : item.track.id();
                    break;
                case LibraryListItem.TYPE_TRACK_CARD:
                    view = trackCard(item.track);
                    boundTrackId = item.track == null ? Long.MIN_VALUE : item.track.id();
                    break;
                case LibraryListItem.TYPE_GROUP_ROW:
                    view = libraryGroupRow(item.group);
                    break;
                case LibraryListItem.TYPE_GROUP_CARD:
                    view = libraryGroupCard(item.group);
                    break;
                case LibraryListItem.TYPE_STATIC:
                default:
                    view = item.view == null ? new View(MainActivity.this) : item.view;
                    break;
            }
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            container.addView(view, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            ));
        }

        void clear() {
            if (boundTrackId != Long.MIN_VALUE) {
                libraryTrackItemViews.remove(boundTrackId);
                boundTrackId = Long.MIN_VALUE;
            }
            container.removeAllViews();
        }

        private void applyLibraryItemMargins(LibraryListItem item) {
            RecyclerView.LayoutParams params = itemView.getLayoutParams() instanceof RecyclerView.LayoutParams
                    ? (RecyclerView.LayoutParams) itemView.getLayoutParams()
                    : new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            );
            int left = 0;
            int right = 0;
            int bottom = 0;
            if (item != null) {
                if (item.type == LibraryListItem.TYPE_TRACK_ROW || item.type == LibraryListItem.TYPE_GROUP_ROW) {
                    left = dp(20);
                    right = dp(20);
                    bottom = dp(8);
                } else if (item.type == LibraryListItem.TYPE_TRACK_CARD || item.type == LibraryListItem.TYPE_GROUP_CARD) {
                    left = dp(6);
                    right = dp(6);
                    bottom = dp(10);
                }
            }
            params.setMargins(left, 0, right, bottom);
            itemView.setLayoutParams(params);
        }
    }

    private static final class LibraryListItem {
        static final int TYPE_STATIC = 1;
        static final int TYPE_TRACK_ROW = 2;
        static final int TYPE_TRACK_CARD = 3;
        static final int TYPE_GROUP_ROW = 4;
        static final int TYPE_GROUP_CARD = 5;

        final int type;
        final View view;
        final DeviceAudioTrack track;
        final LibraryGroup group;

        private LibraryListItem(int type, View view, DeviceAudioTrack track, LibraryGroup group) {
            this.type = type;
            this.view = view;
            this.track = track;
            this.group = group;
        }

        static LibraryListItem staticView(View view) {
            return new LibraryListItem(TYPE_STATIC, view, null, null);
        }

        static LibraryListItem trackRow(DeviceAudioTrack track) {
            return new LibraryListItem(TYPE_TRACK_ROW, null, track, null);
        }

        static LibraryListItem trackCard(DeviceAudioTrack track) {
            return new LibraryListItem(TYPE_TRACK_CARD, null, track, null);
        }

        static LibraryListItem groupRow(LibraryGroup group) {
            return new LibraryListItem(TYPE_GROUP_ROW, null, null, group);
        }

        static LibraryListItem groupCard(LibraryGroup group) {
            return new LibraryListItem(TYPE_GROUP_CARD, null, null, group);
        }
    }

    private final class QueueRecyclerAdapter extends RecyclerView.Adapter<QueueRecyclerViewHolder> {
        private final List<QueueListItem> items = new ArrayList<>();

        QueueRecyclerAdapter() {
            setHasStableIds(true);
        }

        void submitItems(List<QueueListItem> nextItems) {
            items.clear();
            if (nextItems != null) {
                items.addAll(nextItems);
            }
            notifyDataSetChanged();
        }

        boolean isTrackPosition(int position) {
            return position >= 0
                    && position < items.size()
                    && items.get(position).type == QueueListItem.TYPE_TRACK
                    && items.get(position).track != null;
        }

        boolean moveItem(int fromPosition, int toPosition) {
            if (!isTrackPosition(fromPosition) || !isTrackPosition(toPosition) || fromPosition == toPosition) {
                return false;
            }
            QueueListItem moved = items.remove(fromPosition);
            items.add(toPosition, moved);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        int queueIndexForAdapterPosition(int adapterPosition) {
            if (adapterPosition < 0) {
                return -1;
            }
            int trackOffset = -1;
            for (int position = 0; position <= Math.min(adapterPosition, items.size() - 1); position++) {
                if (items.get(position).type == QueueListItem.TYPE_TRACK) {
                    trackOffset++;
                }
            }
            return trackOffset;
        }

        List<DeviceAudioTrack> orderedQueue() {
            List<DeviceAudioTrack> ordered = new ArrayList<>();
            for (QueueListItem item : items) {
                if (item.type == QueueListItem.TYPE_TRACK && item.track != null) {
                    ordered.add(item.track);
                }
            }
            return ordered;
        }

        int[] orderedSourceIndices() {
            int[] ordered = new int[trackItemCount()];
            int outputIndex = 0;
            for (QueueListItem item : items) {
                if (item.type == QueueListItem.TYPE_TRACK && item.track != null) {
                    ordered[outputIndex++] = item.sourceIndex;
                }
            }
            return ordered;
        }

        int queueIndexForSourceIndex(int sourceIndex) {
            int queueIndex = -1;
            for (QueueListItem item : items) {
                if (item.type != QueueListItem.TYPE_TRACK || item.track == null) {
                    continue;
                }
                queueIndex++;
                if (item.sourceIndex == sourceIndex) {
                    return queueIndex;
                }
            }
            return -1;
        }

        boolean hasSourceOrderChanged() {
            int expectedSourceIndex = 0;
            for (QueueListItem item : items) {
                if (item.type != QueueListItem.TYPE_TRACK || item.track == null) {
                    continue;
                }
                if (item.sourceIndex != expectedSourceIndex) {
                    return true;
                }
                expectedSourceIndex++;
            }
            return false;
        }

        private int trackItemCount() {
            int count = 0;
            for (QueueListItem item : items) {
                if (item.type == QueueListItem.TYPE_TRACK && item.track != null) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= items.size()) {
                return QueueListItem.TYPE_STATIC;
            }
            return items.get(position).type;
        }

        @Override
        public long getItemId(int position) {
            if (position < 0 || position >= items.size()) {
                return RecyclerView.NO_ID;
            }
            QueueListItem item = items.get(position);
            if (item.type != QueueListItem.TYPE_TRACK || item.track == null) {
                return Long.MIN_VALUE + position;
            }
            long trackPart = item.track.id() & 0x00000000FFFFFFFFL;
            return (((long) item.sourceIndex) << 32) ^ trackPart;
        }

        @Override
        public QueueRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            FrameLayout container = new FrameLayout(parent.getContext());
            container.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            ));
            return new QueueRecyclerViewHolder(container);
        }

        @Override
        public void onBindViewHolder(QueueRecyclerViewHolder holder, int position) {
            holder.bind(items.get(position), queueIndexForAdapterPosition(position));
        }

        @Override
        public void onViewRecycled(QueueRecyclerViewHolder holder) {
            holder.clear();
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private final class QueueLinearLayoutManager extends LinearLayoutManager {
        QueueLinearLayoutManager(Context context) {
            super(context);
        }

        @Override
        public boolean canScrollVertically() {
            return (queueSheetLayout == null || !queueSheetLayout.isDraggingSheet())
                    && super.canScrollVertically();
        }
    }

    private final class QueueDragCallback extends ItemTouchHelper.SimpleCallback {
        QueueDragCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int position = viewHolder.getBindingAdapterPosition();
            if (queueRecyclerAdapter == null || !queueRecyclerAdapter.isTrackPosition(position)) {
                return 0;
            }
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean canDropOver(
                RecyclerView recyclerView,
                RecyclerView.ViewHolder current,
                RecyclerView.ViewHolder target
        ) {
            int position = target.getBindingAdapterPosition();
            return queueRecyclerAdapter != null && queueRecyclerAdapter.isTrackPosition(position);
        }

        @Override
        public boolean onMove(
                RecyclerView recyclerView,
                RecyclerView.ViewHolder viewHolder,
                RecyclerView.ViewHolder target
        ) {
            if (queueRecyclerAdapter == null) {
                return false;
            }
            return queueRecyclerAdapter.moveItem(
                    viewHolder.getBindingAdapterPosition(),
                    target.getBindingAdapterPosition()
            );
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Queue rows are never swipe-dismissed.
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                if (queueSheetLayout != null) {
                    queueSheetLayout.setQueueReordering(true);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    viewHolder.itemView.setTranslationZ(dp(14));
                }
                viewHolder.itemView.animate()
                        .scaleX(1.035f)
                        .scaleY(1.035f)
                        .alpha(0.96f)
                        .setDuration(120L)
                        .start();
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(160L)
                    .withEndAction(() -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            viewHolder.itemView.setTranslationZ(0f);
                        }
                    })
                    .start();
            if (queueSheetLayout != null) {
                queueSheetLayout.setQueueReordering(false);
                queueSheetLayout.requestDisallowInterceptTouchEvent(false);
            }
            if (queueRecyclerView != null) {
                queueRecyclerView.requestDisallowInterceptTouchEvent(false);
            }
            commitQueueReorderFromAdapter();
        }
    }

    private final class QueueSheetLayout extends FrameLayout {
        private View sheet;
        private View dragHandle;
        private final List<View> dragHandles = new ArrayList<>();
        private float downY;
        private float downX;
        private float startTranslationY;
        private boolean dragging;
        private boolean expanded;
        private boolean queueReordering;
        private boolean downOutsideSheet;
        private boolean downInDragHandle;
        private boolean startedExpanded;
        private boolean dismissing;
        private final int touchSlop;

        QueueSheetLayout(Context context) {
            super(context);
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            setClickable(true);
        }

        void setSheet(View sheet) {
            this.sheet = sheet;
        }

        void setDragHandle(View dragHandle) {
            this.dragHandle = dragHandle;
        }

        void addDragHandle(View dragTarget) {
            if (dragTarget != null && !dragHandles.contains(dragTarget)) {
                dragHandles.add(dragTarget);
            }
        }

        void toggleExpandedAnimated() {
            if (sheet == null || dismissing || queueReordering) {
                return;
            }
            setExpanded(!expanded, true);
        }

        boolean isDraggingSheet() {
            return dragging || dismissing;
        }

        void setQueueReordering(boolean reordering) {
            queueReordering = reordering;
            if (!reordering) {
                dragging = false;
            }
        }

        void showCollapsed() {
            dismissing = false;
            expanded = false;
            if (sheet == null) {
                return;
            }
            sheet.animate().cancel();
            if (getHeight() > 0) {
                sheet.setTranslationY(dismissedTranslationY());
                animateSheetTo(collapsedTranslationY(), 240L, null);
                updateQueueScrollState();
                return;
            }
            ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    ViewTreeObserver currentObserver = getViewTreeObserver();
                    if (currentObserver.isAlive()) {
                        currentObserver.removeOnPreDrawListener(this);
                    }
                    if (sheet != null) {
                        sheet.setTranslationY(dismissedTranslationY());
                        animateSheetTo(collapsedTranslationY(), 240L, null);
                    }
                    updateQueueScrollState();
                    return true;
                }
            });
        }

        void setExpanded(boolean nextExpanded, boolean animate) {
            if (sheet == null) {
                expanded = nextExpanded;
                return;
            }
            if (dismissing) {
                return;
            }
            expanded = nextExpanded;
            float target = nextExpanded ? expandedTranslationY() : collapsedTranslationY();
            long duration = nextExpanded ? 250L : 220L;
            if (animate) {
                animateSheetTo(target, duration, null);
            } else {
                sheet.animate().cancel();
                sheet.setTranslationY(target);
            }
            updateQueueScrollState();
            if (queueRecyclerAdapter != null) {
                queueRecyclerAdapter.notifyDataSetChanged();
            }
        }

        void dismissAnimated(Runnable endAction) {
            if (dismissing) {
                return;
            }
            dismissing = true;
            dragging = false;
            expanded = false;
            if (sheet == null) {
                if (endAction != null) {
                    endAction.run();
                }
                return;
            }
            updateQueueScrollState();
            animateSheetTo(dismissedTranslationY(), 230L, endAction);
        }

        void collapseOrDismissAnimated(Runnable dismissAction) {
            if (dismissing) {
                return;
            }
            if (expanded) {
                setExpanded(false, true);
            } else {
                dismissAnimated(dismissAction);
            }
        }

        private void animateSheetTo(float target, long durationMs, Runnable endAction) {
            if (sheet == null) {
                if (endAction != null) {
                    endAction.run();
                }
                return;
            }
            sheet.animate().cancel();
            sheet.animate()
                    .translationY(target)
                    .setDuration(durationMs)
                    .withEndAction(() -> {
                        if (endAction != null) {
                            endAction.run();
                        }
                    })
                    .start();
        }

        private void updateQueueScrollState() {
            if (queueRecyclerView != null) {
                queueRecyclerView.setNestedScrollingEnabled(!dismissing);
            }
        }

        private float collapsedTranslationY() {
            int height = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
            return Math.max(0f, height - queueSheetCollapsedHeight());
        }

        private float expandedTranslationY() {
            int height = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
            int topOffset = expandedPlayerStatusInset() + dp(72);
            return Math.min(collapsedTranslationY(), Math.max(dp(64), Math.min(topOffset, height - dp(260))));
        }

        private float dismissedTranslationY() {
            int height = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
            return Math.max(collapsedTranslationY() + dp(96), height);
        }

        private void captureDown(MotionEvent event) {
            downY = event.getRawY();
            downX = event.getRawX();
            startTranslationY = sheet == null ? 0f : sheet.getTranslationY();
            dragging = false;
            downOutsideSheet = sheet != null && !isRawPointInsideView(sheet, downX, downY);
            downInDragHandle = isRawPointInsideDragHandle(downX, downY);
            startedExpanded = expanded;
        }

        private boolean isRawPointInsideDragHandle(float rawX, float rawY) {
            if (dragHandle != null && isRawPointInsideView(dragHandle, rawX, rawY)) {
                return true;
            }
            for (View handle : dragHandles) {
                if (isRawPointInsideView(handle, rawX, rawY)) {
                    return true;
                }
            }
            return false;
        }

        private boolean shouldStartSheetDrag(MotionEvent event) {
            if (!downInDragHandle) {
                return false;
            }
            float dy = event.getRawY() - downY;
            float dx = event.getRawX() - downX;
            if (Math.abs(dy) <= touchSlop || Math.abs(dy) <= Math.abs(dx) * 1.2f) {
                return false;
            }
            if (startedExpanded) {
                return dy > 0f;
            }
            return dy != 0f;
        }

        private void settleAfterDrag() {
            if (sheet == null) {
                return;
            }
            float collapsed = collapsedTranslationY();
            float expandedTop = expandedTranslationY();
            float current = sheet.getTranslationY();
            if (startedExpanded) {
                if (current > expandedTop + dp(72)) {
                    setExpanded(false, true);
                } else {
                    setExpanded(true, true);
                }
                return;
            }
            if (current < collapsed - dp(72)) {
                setExpanded(true, true);
                return;
            }
            if (current > collapsed + dp(44)) {
                dismissQueueDialogAnimated();
                return;
            }
            setExpanded(false, true);
        }

        private boolean isRawPointInsideView(View view, float rawX, float rawY) {
            if (view == null) {
                return false;
            }
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            return rawX >= location[0]
                    && rawX <= location[0] + view.getWidth()
                    && rawY >= location[1]
                    && rawY <= location[1] + view.getHeight();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            if (sheet == null || queueReordering || dismissing) {
                return super.onInterceptTouchEvent(event);
            }
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                captureDown(event);
                return downOutsideSheet;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (downOutsideSheet) {
                    return true;
                }
                if (shouldStartSheetDrag(event)) {
                    dragging = true;
                    return true;
                }
            }
            return super.onInterceptTouchEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (sheet == null || queueReordering || dismissing) {
                return super.onTouchEvent(event);
            }
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                captureDown(event);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (downOutsideSheet) {
                    return true;
                }
                if (!dragging && shouldStartSheetDrag(event)) {
                    dragging = true;
                }
                if (dragging) {
                    float dy = event.getRawY() - downY;
                    float maxTranslation = startedExpanded ? collapsedTranslationY() : dismissedTranslationY();
                    float minTranslation = expandedTranslationY();
                    float next = Math.max(minTranslation, Math.min(maxTranslation, startTranslationY + dy));
                    sheet.setTranslationY(next);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (downOutsideSheet && action == MotionEvent.ACTION_UP) {
                    collapseOrDismissQueueDialog();
                    return true;
                }
                if (dragging) {
                    settleAfterDrag();
                }
                dragging = false;
                return true;
            }
            return true;
        }
    }

    private final class QueueRecyclerViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout container;

        QueueRecyclerViewHolder(FrameLayout container) {
            super(container);
            this.container = container;
        }

        void bind(QueueListItem item, int queueIndex) {
            clear();
            applyQueueItemMargins(item);
            View view = item.type == QueueListItem.TYPE_TRACK
                    ? queueTrackRow(item.track, queueIndex, item.sourceIndex, false, true, this)
                    : item.view == null ? new View(MainActivity.this) : item.view;
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            container.addView(view, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            ));
        }

        void clear() {
            container.removeAllViews();
        }

        private void applyQueueItemMargins(QueueListItem item) {
            RecyclerView.LayoutParams params = itemView.getLayoutParams() instanceof RecyclerView.LayoutParams
                    ? (RecyclerView.LayoutParams) itemView.getLayoutParams()
                    : new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
            );
            int bottom = item != null && item.type == QueueListItem.TYPE_TRACK ? dp(8) : dp(10);
            params.setMargins(0, 0, 0, bottom);
            itemView.setLayoutParams(params);
        }
    }

    private static final class QueueListItem {
        static final int TYPE_STATIC = 1;
        static final int TYPE_TRACK = 2;

        final int type;
        final View view;
        final DeviceAudioTrack track;
        final int sourceIndex;

        private QueueListItem(int type, View view, DeviceAudioTrack track, int sourceIndex) {
            this.type = type;
            this.view = view;
            this.track = track;
            this.sourceIndex = sourceIndex;
        }

        static QueueListItem staticView(View view) {
            return new QueueListItem(TYPE_STATIC, view, null, -1);
        }

        static QueueListItem track(DeviceAudioTrack track, int sourceIndex) {
            return new QueueListItem(TYPE_TRACK, null, track, sourceIndex);
        }
    }

    private final class StreamLoadingSpinnerView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arcBounds = new RectF();
        private ObjectAnimator spinAnimator;
        private float spinDegrees;

        StreamLoadingSpinnerView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        public void setSpinDegrees(float degrees) {
            spinDegrees = degrees;
            invalidate();
        }

        public float getSpinDegrees() {
            return spinDegrees;
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startSpin();
        }

        @Override
        protected void onDetachedFromWindow() {
            stopSpin();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            if (width <= 0f || height <= 0f) {
                return;
            }

            float cx = width / 2f;
            float cy = height / 2f;
            float radius = Math.min(width, height) * 0.34f;
            float strokeWidth = Math.max(dp(3), Math.min(width, height) * 0.09f);
            arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(color(R.color.ytet_muted));
            paint.setAlpha(92);
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setColor(color(R.color.ytet_text));
            paint.setAlpha(255);
            canvas.drawArc(arcBounds, spinDegrees - 90f, 270f, false, paint);
        }

        private void startSpin() {
            if (spinAnimator != null && spinAnimator.isStarted()) {
                return;
            }
            spinAnimator = ObjectAnimator.ofFloat(this, "spinDegrees", spinDegrees, spinDegrees + 360f);
            spinAnimator.setDuration(820L);
            spinAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            spinAnimator.setInterpolator(new LinearInterpolator());
            spinAnimator.start();
        }

        private void stopSpin() {
            if (spinAnimator != null) {
                spinAnimator.cancel();
                spinAnimator = null;
            }
        }
    }

    private final class PullRefreshIndicatorView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arcBounds = new RectF();
        private float pullProgress;
        private float spinDegrees;
        private boolean refreshing;

        PullRefreshIndicatorView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        public void setSpinDegrees(float degrees) {
            spinDegrees = degrees;
            invalidate();
        }

        public float getSpinDegrees() {
            return spinDegrees;
        }

        void setPullProgress(float progress) {
            pullProgress = Math.max(0f, Math.min(1f, progress));
            if (!refreshing) {
                spinDegrees = pullProgress * 520f;
            }
            invalidate();
        }

        void setRefreshing(boolean active) {
            if (refreshing == active) {
                return;
            }
            refreshing = active;
            if (active) {
                pullProgress = 1f;
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            if (width <= 0f || height <= 0f) {
                return;
            }
            float cx = width / 2f;
            float cy = height / 2f;
            float outerRadius = Math.min(width, height) * 0.5f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, outerRadius, paint);

            float progress = refreshing ? 1f : pullProgress;
            if (progress <= 0f) {
                return;
            }

            float iconRadius = outerRadius * 0.42f;
            float strokeWidth = Math.max(dp(3), outerRadius * 0.13f);
            arcBounds.set(cx - iconRadius, cy - iconRadius, cx + iconRadius, cy + iconRadius);

            float sweep = refreshing ? 292f : 54f + progress * 248f;
            float start = -126f + spinDegrees;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(Color.BLACK);
            canvas.drawArc(arcBounds, start, sweep, false, paint);
        }
    }

    private final class PullRefreshRecyclerView extends RecyclerView {
        private boolean pullCanceledChildGesture;

        PullRefreshRecyclerView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (handleLibraryPullToRefresh(event)) {
                if (!pullCanceledChildGesture && action != MotionEvent.ACTION_DOWN) {
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    pullCanceledChildGesture = true;
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    pullCanceledChildGesture = false;
                }
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                pullCanceledChildGesture = false;
            }
            return super.dispatchTouchEvent(event);
        }
    }

    private final class PullRefreshScrollView extends ScrollView {
        private boolean pullCanceledChildGesture;
        private boolean tabScrollEnabled = true;

        PullRefreshScrollView(Context context) {
            super(context);
        }

        void setTabScrollEnabled(boolean enabled) {
            tabScrollEnabled = enabled;
            if (!enabled && getScrollY() != 0) {
                scrollTo(0, 0);
            }
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            if (!tabScrollEnabled) {
                return false;
            }
            return super.onInterceptTouchEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!tabScrollEnabled) {
                return false;
            }
            return super.onTouchEvent(event);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (handleLibraryPullToRefresh(event)) {
                if (!pullCanceledChildGesture && action != MotionEvent.ACTION_DOWN) {
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    pullCanceledChildGesture = true;
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    pullCanceledChildGesture = false;
                }
                return true;
            }
            if (action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL) {
                pullCanceledChildGesture = false;
            }
            return super.dispatchTouchEvent(event);
        }
    }

    private final class SquareFrameLayout extends FrameLayout {
        SquareFrameLayout(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int squareSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, squareSpec);
        }
    }

    private final class MiniPlaybackProgressView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private long durationMs = 1L;
        private long positionMs;
        private boolean progressVisible;

        MiniPlaybackProgressView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void setProgress(long duration, long position, boolean visible) {
            long nextDuration = Math.max(1L, duration);
            long nextPosition = Math.max(0L, Math.min(position, nextDuration));
            boolean changed = durationMs != nextDuration
                    || positionMs != nextPosition
                    || progressVisible != visible;
            durationMs = nextDuration;
            positionMs = nextPosition;
            progressVisible = visible;
            if (changed) {
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!progressVisible || getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            float progress = Math.max(0f, Math.min(1f, positionMs / (float) durationMs));
            float trackHeight = Math.max(1f, getHeight());
            float radius = trackHeight / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(58, 255, 255, 255));
            rect.set(0f, 0f, getWidth(), trackHeight);
            canvas.drawRoundRect(rect, radius, radius, paint);

            paint.setColor(Color.WHITE);
            rect.set(0f, 0f, Math.max(0f, getWidth() * progress), trackHeight);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
    }

    private final class PlaybackSeekBarView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect textBounds = new Rect();
        private final RectF rect = new RectF();
        private final Path arrow = new Path();
        private SeekChangeListener listener;
        private long durationMs = 1L;
        private long positionMs;
        private long previewPositionMs;
        private boolean dragging;

        PlaybackSeekBarView(Context context) {
            super(context);
            setWillNotDraw(false);
            setFocusable(true);
        }

        void setProgress(long duration, long position) {
            durationMs = Math.max(1L, duration);
            positionMs = clampPlaybackPosition(position);
            if (!dragging) {
                previewPositionMs = positionMs;
            }
            invalidate();
        }

        void setOnSeekChangeListener(SeekChangeListener seekChangeListener) {
            listener = seekChangeListener;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float left = seekTrackLeft();
            float right = seekTrackRight();
            float centerY = seekTrackCenterY();
            float trackHeight = dp(4);
            float thumbX = thumbX(dragging ? previewPositionMs : positionMs);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(64, 255, 255, 255));
            rect.set(left, centerY - trackHeight / 2f, right, centerY + trackHeight / 2f);
            canvas.drawRoundRect(rect, trackHeight, trackHeight, paint);

            paint.setColor(Color.WHITE);
            rect.set(left, centerY - trackHeight / 2f, Math.max(left, thumbX), centerY + trackHeight / 2f);
            canvas.drawRoundRect(rect, trackHeight, trackHeight, paint);

            if (dragging) {
                drawSeekBubble(canvas, thumbX, centerY);
            }

            paint.setColor(Color.WHITE);
            canvas.drawCircle(thumbX, centerY, dragging ? dp(8) : dp(5), paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                if (!isInsideThumbTouchArea(event.getX(), event.getY())) {
                    return false;
                }
                suppressPlayerDragDismiss = true;
                dragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                updatePreviewFromX(event.getX());
                setPressed(true);
                if (listener != null) {
                    listener.onSeekStarted(previewPositionMs);
                }
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE && dragging) {
                updatePreviewFromX(event.getX());
                if (listener != null) {
                    listener.onSeekPreview(previewPositionMs);
                }
                invalidate();
                return true;
            }
            if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && dragging) {
                updatePreviewFromX(event.getX());
                dragging = false;
                suppressPlayerDragDismiss = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                setPressed(false);
                if (listener != null) {
                    if (action == MotionEvent.ACTION_UP) {
                        listener.onSeekCommitted(previewPositionMs);
                    } else {
                        listener.onSeekCanceled();
                    }
                }
                invalidate();
                return true;
            }
            return super.onTouchEvent(event);
        }

        private void drawSeekBubble(Canvas canvas, float thumbX, float trackY) {
            String text = MusicLibrary.formatDuration(previewPositionMs);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(12f * getResources().getDisplayMetrics().scaledDensity);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.getTextBounds(text, 0, text.length(), textBounds);

            float bubbleWidth = Math.max(dp(58), textBounds.width() + dp(24));
            float bubbleHeight = dp(26);
            float centerX = Math.max(bubbleWidth / 2f + dp(2), Math.min(getWidth() - bubbleWidth / 2f - dp(2), thumbX));
            float top = Math.max(0f, trackY - dp(33));
            float bottom = top + bubbleHeight;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(218, 12, 12, 14));
            rect.set(centerX - bubbleWidth / 2f, top, centerX + bubbleWidth / 2f, bottom);
            canvas.drawRoundRect(rect, dp(9), dp(9), paint);

            arrow.reset();
            arrow.moveTo(thumbX - dp(6), bottom - dp(1));
            arrow.lineTo(thumbX + dp(6), bottom - dp(1));
            arrow.lineTo(thumbX, Math.min(trackY - dp(4), bottom + dp(7)));
            arrow.close();
            canvas.drawPath(arrow, paint);

            paint.setColor(Color.WHITE);
            canvas.drawText(text, centerX, top + bubbleHeight / 2f + textBounds.height() / 2f - dp(1), paint);
        }

        private boolean isInsideThumbTouchArea(float x, float y) {
            float dx = Math.abs(x - thumbX(positionMs));
            float dy = Math.abs(y - seekTrackCenterY());
            return dx <= dp(30) && dy <= dp(22);
        }

        private void updatePreviewFromX(float x) {
            float left = seekTrackLeft();
            float right = seekTrackRight();
            float clampedX = Math.max(left, Math.min(right, x));
            float progress = (clampedX - left) / Math.max(1f, right - left);
            previewPositionMs = clampPlaybackPosition(Math.round(progress * durationMs));
        }

        private long clampPlaybackPosition(long position) {
            return Math.max(0L, Math.min(durationMs, position));
        }

        private float thumbX(long position) {
            float left = seekTrackLeft();
            float right = seekTrackRight();
            float progress = durationMs <= 0L ? 0f : position / (float) durationMs;
            return left + Math.max(0f, Math.min(1f, progress)) * (right - left);
        }

        private float seekTrackLeft() {
            return dp(8);
        }

        private float seekTrackRight() {
            return Math.max(seekTrackLeft(), getWidth() - dp(8));
        }

        private float seekTrackCenterY() {
            return getHeight() - dp(11);
        }
    }

    private final class DragDismissLayout extends LinearLayout {
        private float startX;
        private float startY;
        private final int touchSlop;
        private VelocityTracker velocityTracker;
        private boolean dragCanStart;
        private boolean draggingDown;
        private boolean draggingQueueUp;
        private boolean playerSurfaceStyle;

        DragDismissLayout(Context context) {
            super(context);
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            setClickable(true);
        }

        void setPlayerSurfaceStyle(boolean enabled) {
            playerSurfaceStyle = enabled;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                beginDragTracking(event);
                super.onInterceptTouchEvent(event);
                return false;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                trackDragMovement(event);
                if (!draggingDown && !draggingQueueUp && shouldStartDrag(event)) {
                    startDragging(event);
                    return true;
                }
                if (!draggingDown && !draggingQueueUp && shouldStartQueueDrag(event)) {
                    startQueueDragging();
                    return true;
                }
                return draggingDown || draggingQueueUp;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (!draggingDown && !draggingQueueUp) {
                    releaseDragTracker();
                }
                return false;
            }
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                beginDragTracking(event);
                return true;
            }
            trackDragMovement(event);
            if (action == MotionEvent.ACTION_MOVE) {
                if (!draggingDown && !draggingQueueUp && shouldStartDrag(event)) {
                    startDragging(event);
                }
                if (!draggingDown && !draggingQueueUp && shouldStartQueueDrag(event)) {
                    startQueueDragging();
                }
                if (draggingDown) {
                    updateDragPosition(event);
                    return true;
                }
                if (draggingQueueUp) {
                    return true;
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (draggingQueueUp) {
                    finishQueueDragging();
                } else {
                    finishDragging(event, action == MotionEvent.ACTION_CANCEL);
                }
                return true;
            }
            return draggingDown || draggingQueueUp || super.onTouchEvent(event);
        }

        private void beginDragTracking(MotionEvent event) {
            releaseDragTracker();
            velocityTracker = VelocityTracker.obtain();
            velocityTracker.addMovement(event);
            startX = event.getRawX();
            startY = event.getRawY();
            dragCanStart = canStartDragFrom(event);
            draggingDown = false;
            draggingQueueUp = false;
            playerDragDismissActive = false;
            animate().cancel();
            setAlpha(1f);
            setTranslationY(0f);
            setDragRounded(false);
        }

        private void trackDragMovement(MotionEvent event) {
            if (velocityTracker != null) {
                velocityTracker.addMovement(event);
            }
        }

        private boolean shouldStartDrag(MotionEvent event) {
            if (!dragCanStart || suppressPlayerDragDismiss) {
                return false;
            }
            float dx = event.getRawX() - startX;
            float dy = event.getRawY() - startY;
            float threshold = Math.max(touchSlop, dp(8));
            return dy > threshold && dy > Math.abs(dx) * 0.85f;
        }

        private boolean shouldStartQueueDrag(MotionEvent event) {
            if (!dragCanStart || suppressPlayerDragDismiss) {
                return false;
            }
            float dx = event.getRawX() - startX;
            float dy = event.getRawY() - startY;
            float threshold = Math.max(touchSlop, dp(12));
            return dy < -threshold && -dy > Math.abs(dx) * 0.85f;
        }

        private void startDragging(MotionEvent event) {
            draggingDown = true;
            playerDragDismissActive = true;
            suppressPlayerDragDismiss = true;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            setDragRounded(true);
            updateDragPosition(event);
        }

        private void startQueueDragging() {
            draggingQueueUp = true;
            suppressPlayerDragDismiss = true;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            showQueueDialog();
        }

        private void updateDragPosition(MotionEvent event) {
            float dy = event.getRawY() - startY;
            float translation = Math.max(0f, Math.min(getHeight(), dy));
            setTranslationY(translation);
            setAlpha(1f - Math.min(0.18f, translation / Math.max(1f, getHeight()) * 0.18f));
            setDragRounded(translation > 0f);
        }

        private void finishDragging(MotionEvent event, boolean canceled) {
            if (!draggingDown) {
                releaseDragTracker();
                return;
            }
            float velocityY = 0f;
            if (velocityTracker != null) {
                velocityTracker.addMovement(event);
                velocityTracker.computeCurrentVelocity(1000);
                velocityY = velocityTracker.getYVelocity();
            }
            float translation = Math.max(0f, getTranslationY());
            boolean shouldDismiss = !canceled
                    && (translation >= dp(72) || (velocityY >= 1100f && translation >= dp(24)));
            dragCanStart = false;
            draggingDown = false;
            releaseDragTracker();
            if (shouldDismiss) {
                animateDismissTopPlayerSurface();
            } else {
                snapBackTopPlayerSurface();
            }
        }

        private void finishQueueDragging() {
            draggingQueueUp = false;
            dragCanStart = false;
            suppressPlayerDragDismiss = false;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            releaseDragTracker();
        }

        private void snapBackTopPlayerSurface() {
            animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(190L)
                    .withEndAction(() -> {
                        playerDragDismissActive = false;
                        suppressPlayerDragDismiss = false;
                        setDragRounded(false);
                    })
                    .start();
        }

        private void releaseDragTracker() {
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
        }

        private void animateDismissTopPlayerSurface() {
            animate()
                    .translationY(Math.max(getHeight(), dp(320)))
                    .alpha(0.82f)
                    .setDuration(220L)
                    .withEndAction(() -> {
                        setTranslationY(0f);
                        setAlpha(1f);
                        setDragRounded(false);
                        playerDragDismissActive = false;
                        suppressPlayerDragDismiss = false;
                        dismissTopPlayerSurface();
                    })
                    .start();
        }

        private boolean canStartDragFrom(MotionEvent event) {
            return !isTouchInsideDragBlocker(this, event.getRawX(), event.getRawY());
        }

        private boolean isTouchInsideDragBlocker(View view, float rawX, float rawY) {
            if (view.getVisibility() != View.VISIBLE) {
                return false;
            }
            if (view != this && isDragBlocker(view) && isRawPointInsideView(view, rawX, rawY)) {
                return true;
            }
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int index = group.getChildCount() - 1; index >= 0; index--) {
                    if (isTouchInsideDragBlocker(group.getChildAt(index), rawX, rawY)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isDragBlocker(View view) {
            return view instanceof Button
                    || view instanceof ImageButton
                    || view instanceof EditText
                    || view instanceof Spinner
                    || view instanceof CheckBox
                    || view instanceof RadioButton
                    || view instanceof RadioGroup
                    || view instanceof ProgressBar
                    || view instanceof PlaybackSeekBarView
                    || view instanceof SleepTimerDialView
                    || view instanceof TimerConfirmButton;
        }

        private boolean isRawPointInsideView(View view, float rawX, float rawY) {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            return rawX >= location[0]
                    && rawX <= location[0] + view.getWidth()
                    && rawY >= location[1]
                    && rawY <= location[1] + view.getHeight();
        }

        private void setDragRounded(boolean rounded) {
            if (playerSurfaceStyle) {
                setBackground(expandedPlayerBackground(rounded));
                setClipToOutline(rounded);
            }
        }

        private void dismissTopPlayerSurface() {
            if (queueDialog != null && queueDialog.isShowing()) {
                collapseOrDismissQueueDialog();
            } else if (playerDialog != null && playerDialog.isShowing()) {
                playerDialog.dismiss();
            }
        }
    }

    private final class TimerConfirmButton extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF buttonBounds = new RectF();

        TimerConfirmButton(Context context) {
            super(context);
            setWillNotDraw(false);
            setClickable(true);
            setFocusable(true);
            setContentDescription("슬립 타이머 설정");
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            buttonBounds.set(dp(2), dp(4), getWidth() - dp(2), getHeight() - dp(4));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(isPressed()
                    ? blendColors(color(R.color.ytet_accent), Color.BLACK, 0.18f)
                    : color(R.color.ytet_accent));
            canvas.drawRoundRect(buttonBounds, dp(14), dp(14), paint);

            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(13f * getResources().getDisplayMetrics().scaledDensity);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float textY = buttonBounds.centerY() - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText("설정", buttonBounds.centerX(), textY, paint);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            invalidate();
        }
    }

    private final class SleepTimerDialView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF wheel = new RectF();
        private TimerChangeListener listener;
        private int maxValue = 120;
        private int stepValue = 5;
        private int selectedValue = 30;
        private int dragStartValue = 30;
        private float dialCenterIndex = 30f / 5f;
        private float dragStepOffset;
        private float dragStartY;
        private VelocityTracker velocityTracker;
        private boolean flinging;
        private float flingVelocityStepsPerSecond;
        private long lastFlingFrameMs;
        private boolean timerActive;
        private boolean dragging;
        private boolean twoDigit;
        private final Runnable flingRunnable = new Runnable() {
            @Override
            public void run() {
                continueDialFling();
            }
        };

        SleepTimerDialView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void configure(int max, int step, boolean showTwoDigit) {
            maxValue = Math.max(1, max);
            stepValue = Math.max(1, step);
            twoDigit = showTwoDigit;
            selectedValue = clampTimerValue(selectedValue);
            if (!dragging && !flinging) {
                dialCenterIndex = selectedValue / (float) stepValue;
            }
            invalidate();
        }

        void setSelectedValue(int value) {
            selectedValue = clampTimerValue(value);
            if (!dragging && !flinging) {
                dragStepOffset = 0f;
                dialCenterIndex = selectedValue / (float) stepValue;
            }
            invalidate();
        }

        void setTimerActive(boolean active) {
            timerActive = active;
            invalidate();
        }

        void setOnTimerChangeListener(TimerChangeListener timerChangeListener) {
            listener = timerChangeListener;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            wheel.set(dp(4), dp(8), width - dp(4), height - dp(8));

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.TRANSPARENT);
            canvas.drawRoundRect(wheel, dp(16), dp(16), paint);

            paint.setColor(timerActive
                    ? blendColors(playbackThemeColor, color(R.color.ytet_background), 0.42f)
                    : Color.argb(92, 0, 0, 0));
            RectF centerBand = new RectF(dp(8), height * 0.26f, width - dp(8), height * 0.74f);
            canvas.drawRoundRect(centerBand, dp(13), dp(13), paint);

            float centerIndex = visibleCenterIndex();
            int anchor = Math.round(centerIndex);
            for (int slot = -2; slot <= 2; slot++) {
                int visualIndex = anchor + slot;
                float distance = visualIndex - centerIndex;
                if (Math.abs(distance) > 1.65f) {
                    continue;
                }
                drawDialValue(canvas, timerTextForIndex(visualIndex), width / 2f, dialValueY(height, distance),
                        dialTextSize(distance), dialTextColor(distance));
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                stopDialFling();
                recycleTimerVelocityTracker();
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                suppressPlayerDragDismiss = true;
                dragging = true;
                dragStepOffset = 0f;
                dragStartY = event.getRawY();
                dragStartValue = selectedValue;
                dialCenterIndex = selectedValue / (float) stepValue;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                }
                float startIndex = dragStartValue / (float) stepValue;
                dialCenterIndex = startIndex + (dragStartY - event.getRawY()) / dp(20);
                dragStepOffset = dialCenterIndex - startIndex;
                updateSelectedValueFromCenter(false);
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                float velocityStepsPerSecond = 0f;
                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                    velocityTracker.computeCurrentVelocity(1000);
                    velocityStepsPerSecond = -velocityTracker.getYVelocity() / Math.max(1f, dp(20));
                }
                recycleTimerVelocityTracker();
                dragging = false;
                dragStepOffset = 0f;
                suppressPlayerDragDismiss = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (action == MotionEvent.ACTION_UP && Math.abs(velocityStepsPerSecond) >= 3.2f) {
                    startDialFling(velocityStepsPerSecond);
                } else {
                    settleDial(action == MotionEvent.ACTION_UP);
                }
                invalidate();
                return true;
            }
            return super.onTouchEvent(event);
        }

        private float visibleCenterIndex() {
            return dialCenterIndex;
        }

        private float dialValueY(int height, float distance) {
            return height * 0.5f + distance * height * 0.33f;
        }

        private int dialTextSize(float distance) {
            float absolute = Math.min(1.4f, Math.abs(distance));
            return Math.round(24f - absolute * 7f);
        }

        private int dialTextColor(float distance) {
            int alpha = Math.round(255f - Math.min(1.4f, Math.abs(distance)) * 102f);
            return Color.argb(Math.max(96, alpha), 255, 255, 255);
        }

        private void drawDialValue(Canvas canvas, String text, float x, float y, int sizeSp, int textColor) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(textColor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(sizeSp * getResources().getDisplayMetrics().scaledDensity);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            canvas.drawText(text, x, y - (metrics.ascent + metrics.descent) / 2f, paint);
        }

        private String timerText(int minutes) {
            int value = clampTimerValue(minutes);
            return twoDigit ? String.format(Locale.US, "%02d", value) : Integer.toString(value);
        }

        private String timerTextForIndex(int index) {
            return timerText(timerValueForIndex(index));
        }

        private int maxTimerIndex() {
            return maxValue / stepValue;
        }

        private int timerValueCount() {
            return maxTimerIndex() + 1;
        }

        private int timerValueForIndex(int index) {
            int normalizedIndex = Math.floorMod(index, timerValueCount());
            return clampTimerValue(normalizedIndex * stepValue);
        }

        private int clampTimerValue(int value) {
            int stepped = Math.round(value / (float) stepValue) * stepValue;
            return Math.max(0, Math.min(maxValue, stepped));
        }

        private void updateSelectedValueFromCenter(boolean committed) {
            int nextValue = timerValueForIndex(Math.round(dialCenterIndex));
            if (nextValue != selectedValue || committed) {
                selectedValue = nextValue;
                if (listener != null) {
                    listener.onChanged(selectedValue, committed);
                }
            }
        }

        private void startDialFling(float velocityStepsPerSecond) {
            flinging = true;
            flingVelocityStepsPerSecond = Math.max(-54f, Math.min(54f, velocityStepsPerSecond));
            lastFlingFrameMs = System.currentTimeMillis();
            postOnAnimation(flingRunnable);
        }

        private void continueDialFling() {
            if (!flinging) {
                return;
            }
            long nowMs = System.currentTimeMillis();
            float deltaSeconds = Math.min(0.034f, Math.max(0.001f, (nowMs - lastFlingFrameMs) / 1000f));
            lastFlingFrameMs = nowMs;
            dialCenterIndex += flingVelocityStepsPerSecond * deltaSeconds;
            updateSelectedValueFromCenter(false);
            float friction = (float) Math.pow(0.90f, deltaSeconds * 60f);
            flingVelocityStepsPerSecond *= friction;
            invalidate();
            if (Math.abs(flingVelocityStepsPerSecond) <= 0.42f) {
                settleDial(true);
                return;
            }
            postOnAnimation(flingRunnable);
        }

        private void settleDial(boolean committed) {
            flinging = false;
            removeCallbacks(flingRunnable);
            int targetIndex = Math.round(dialCenterIndex);
            selectedValue = timerValueForIndex(targetIndex);
            dialCenterIndex = selectedValue / (float) stepValue;
            dragStepOffset = 0f;
            if (listener != null && committed) {
                listener.onChanged(selectedValue, true);
            }
            invalidate();
        }

        private void stopDialFling() {
            flinging = false;
            removeCallbacks(flingRunnable);
        }

        private void recycleTimerVelocityTracker() {
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
        }
    }

    private interface TimerChangeListener {
        void onChanged(int minutes, boolean committed);
    }

    private interface SeekChangeListener {
        void onSeekStarted(long positionMs);

        void onSeekPreview(long positionMs);

        void onSeekCommitted(long positionMs);

        void onSeekCanceled();
    }

    private final class LibrarySearchEditText extends EditText {
        private LibrarySearchEditText(Context context) {
            super(context);
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK
                    && event != null
                    && event.getAction() == KeyEvent.ACTION_UP) {
                finishLibrarySearchInput(this);
            }
            return super.onKeyPreIme(keyCode, event);
        }
    }

    private final class UrlEditText extends EditText {
        private final int touchSlop;
        private float downX;
        private float downY;
        private int downScrollX;
        private boolean horizontalDragging;

        private UrlEditText(Context context) {
            super(context);
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isEnabled()) {
                return super.onTouchEvent(event);
            }
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                disallowParentScroll(true);
                downX = event.getX();
                downY = event.getY();
                downScrollX = getScrollX();
                horizontalDragging = false;
                requestFocus();
                setCursorVisible(true);
                showKeyboard(this);
                super.onTouchEvent(event);
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                float deltaX = downX - event.getX();
                float deltaY = downY - event.getY();
                if (!horizontalDragging && Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY)) {
                    horizontalDragging = true;
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(cancel);
                    cancel.recycle();
                }
                if (horizontalDragging) {
                    scrollTo(clampUrlScroll(downScrollX + Math.round(deltaX)), getScrollY());
                    return true;
                }
                return super.onTouchEvent(event);
            }
            if (action == MotionEvent.ACTION_UP) {
                if (horizontalDragging) {
                    horizontalDragging = false;
                    disallowParentScroll(false);
                    return true;
                }
                boolean handled = super.onTouchEvent(event);
                setCursorVisible(true);
                showKeyboard(this);
                disallowParentScroll(false);
                return handled;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                horizontalDragging = false;
                disallowParentScroll(false);
            }
            return super.onTouchEvent(event);
        }

        @Override
        public boolean performClick() {
            setCursorVisible(true);
            showKeyboard(this);
            return super.performClick();
        }

        private int clampUrlScroll(int value) {
            return Math.max(0, Math.min(value, maxUrlScroll()));
        }

        private int maxUrlScroll() {
            int visibleWidth = Math.max(0, getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight());
            int textWidth = 0;
            if (getLayout() != null && getLayout().getLineCount() > 0) {
                textWidth = (int) Math.ceil(getLayout().getLineWidth(0));
            } else if (getText() != null) {
                textWidth = (int) Math.ceil(getPaint().measureText(getText().toString()));
            }
            return Math.max(0, textWidth - visibleWidth);
        }

        private void disallowParentScroll(boolean disallow) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(disallow);
            }
        }
    }

    private static final class TabItem {
        private final LinearLayout root;
        private final ImageView icon;
        private final TextView label;
        private final int outlineIcon;
        private final int filledIcon;

        private TabItem(LinearLayout root, ImageView icon, TextView label, int outlineIcon, int filledIcon) {
            this.root = root;
            this.icon = icon;
            this.label = label;
            this.outlineIcon = outlineIcon;
            this.filledIcon = filledIcon;
        }
    }

    private static final class LibraryGroup {
        private final String key;
        private final String title;
        private final String subtitle;
        private final DeviceAudioTrack coverTrack;
        private final List<DeviceAudioTrack> tracks;

        private LibraryGroup(
                String key,
                String title,
                String subtitle,
                DeviceAudioTrack coverTrack,
                List<DeviceAudioTrack> tracks
        ) {
            this.key = key == null || key.trim().isEmpty() ? "group:unknown" : key.trim();
            this.title = title == null || title.trim().isEmpty() ? "알 수 없음" : title.trim();
            this.subtitle = subtitle == null || subtitle.trim().isEmpty() ? "-" : subtitle.trim();
            this.coverTrack = coverTrack;
            this.tracks = tracks == null ? new ArrayList<>() : new ArrayList<>(tracks);
        }
    }

    private enum LibraryFilter {
        ALL,
        ALBUM,
        ARTIST,
        PLAYLIST
    }

    private enum LibrarySort {
        NEWEST("newest", "최신순", "최근 추가된 음악부터 표시"),
        OLDEST("oldest", "오래된순", "오래전에 추가된 음악부터 표시"),
        NAME("name", "이름순", "이름을 기준으로 정렬"),
        MOST_PLAYED("most_played", "많이 재생한순", "재생 횟수가 많은 음악부터 표시"),
        LEAST_PLAYED("least_played", "적게 재생한순", "재생 횟수가 적은 음악부터 표시");

        private final String key;
        private final String label;
        private final String description;

        LibrarySort(String key, String label, String description) {
            this.key = key;
            this.label = label;
            this.description = description;
        }

        private String buttonLabel() {
            if (this == MOST_PLAYED) {
                return "많이\n재생한순";
            }
            if (this == LEAST_PLAYED) {
                return "적게\n재생한순";
            }
            return label;
        }

        private int buttonWidthDp() {
            return this == MOST_PLAYED || this == LEAST_PLAYED ? 76 : 62;
        }

        private static LibrarySort fromKey(String key) {
            if ("play_order".equals(key)) {
                return MOST_PLAYED;
            }
            for (LibrarySort sort : values()) {
                if (sort.key.equals(key)) {
                    return sort;
                }
            }
            return NEWEST;
        }
    }

    private enum ArtistDetailMode {
        ALL,
        ALBUMS
    }

    private enum StreamVideoSort {
        POPULAR("인기순"),
        LATEST("최신순"),
        OLDEST("오래된순");

        private final String label;

        StreamVideoSort(String label) {
            this.label = label;
        }
    }

    private enum Tab {
        HOME,
        LIBRARY,
        EXTRACTOR
    }
}
