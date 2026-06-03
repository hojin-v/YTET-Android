package com.ytet.android.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
import com.ytet.android.playback.PlaybackService;
import com.ytet.android.stream.MusicStation;
import com.ytet.android.stream.StationCatalog;
import com.ytet.android.update.UpdateChecker;
import com.ytet.android.update.UpdateInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public final class MainActivity extends Activity {
    private static final int REQUEST_OUTPUT_TREE = 1207;
    private static final int REQUEST_NOTIFICATIONS = 1208;
    private static final int REQUEST_DELETE_AUDIO = 1209;
    private static final int REQUEST_AUDIO_LIBRARY = 1210;
    private static final int REQUEST_WRITE_LIBRARY = 1211;
    private static final long NO_DOWNLOAD_ID = -1L;
    private static final String PREFS = "ytet_android";
    private static final String PREF_OUTPUT_TREE = "output_tree";
    private static final String PREF_UPDATE_DOWNLOAD_ID = "update_download_id";
    private static final String PREF_UPDATE_TAG = "update_tag";
    private static final String PREF_AUTO_UPDATE_CHECK = "auto_update_check";
    private static final String PREF_LIBRARY_SOURCE = "library_source";
    private static final String LIBRARY_SOURCE_COLLECTION = "collection";
    private static final String LIBRARY_SOURCE_DEVICE = "device";

    private final ExecutorService libraryExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private final DeviceMusicLibrary deviceMusicLibrary = new DeviceMusicLibrary();
    private final UpdateChecker updateChecker = new UpdateChecker();

    private ScrollView contentScrollView;
    private LinearLayout nowPlayingBar;
    private FrameLayout nowPlayingCover;
    private TextView nowPlayingTitle;
    private TextView nowPlayingMeta;
    private ImageButton playPauseButton;
    private TabItem homeTabButton;
    private TabItem libraryTabButton;
    private TabItem extractorTabButton;
    private TextView updateStatusText;
    private Button updateActionButton;
    private Dialog playerDialog;
    private Dialog queueDialog;

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
    private TextView folderText;
    private TextView statusText;
    private TextView resultText;
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
    private boolean sleepTimerPaused;
    private boolean sleepTimerControlsVisible;
    private boolean suppressPlayerDragDismiss;
    private String extractorUrl = "";
    private MediaType extractorMediaType = MediaType.AUDIO;
    private String extractorOption = AudioFormat.M4A.value();
    private boolean extractorIncludeSubtitles;
    private boolean extractorIncludePlaylist;
    private boolean extractorEnhanceMetadata;

    private List<DeviceAudioTrack> libraryTracks = new ArrayList<>();
    private List<DeviceAudioTrack> homeTracks = new ArrayList<>();
    private boolean libraryLoaded;
    private boolean libraryLoading;
    private String libraryStatus = "기기 음악 권한을 허용하면 앨범과 아티스트를 정리합니다.";
    private boolean homeLoaded;
    private boolean homeLoading;
    private String homeStatus = "보관함 음악을 스캔하면 추천 믹스가 표시됩니다.";
    private LibraryFilter libraryFilter = LibraryFilter.ALL;
    private String librarySearchQuery = "";
    private boolean libraryGridView;
    private boolean librarySearchVisible;
    private EditText librarySearchInput;
    private LibraryGroup focusedLibraryGroup;
    private LibraryFilter focusedLibraryGroupFilter;
    private float libraryPullStartY;
    private boolean libraryPullTracking;
    private float libraryPullDistance;
    private boolean libraryPullReady;
    private FrameLayout libraryPullIndicator;
    private ImageView libraryPullIcon;
    private String librarySource = LIBRARY_SOURCE_COLLECTION;
    private DeviceAudioTrack selectedTrack;
    private DeviceAudioTrack pendingDeleteTrack;
    private final Map<Long, View> libraryTrackItemViews = new HashMap<>();

    private String outputTreeUri;
    private int extractionPercent;
    private String extractionStatus = "대기 중";
    private String extractionResult = "-";
    private boolean extractionBusy;
    private boolean receiverRegistered;
    private boolean playbackReceiverRegistered;
    private boolean updateReceiverRegistered;
    private boolean updateChecking;
    private boolean updateChecked;
    private boolean updateDownloading;
    private String updateStatus = "정식 릴리즈 업데이트만 확인합니다.";
    private UpdateInfo availableUpdate;
    private long updateDownloadId = NO_DOWNLOAD_ID;
    private boolean autoUpdateCheck = true;
    private boolean extractionPendingNotificationPermission;

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

            extractionPercent = percent;
            if (error != null) {
                extractionStatus = "오류";
                extractionResult = error;
            } else {
                extractionStatus = progressStatus(stage, message);
                if (result != null) {
                    extractionResult = result;
                }
            }

            if (done) {
                extractionBusy = false;
            } else {
                extractionBusy = true;
            }
            applyExtractionStateToViews();
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
            playbackHasQueue = intent.getBooleanExtra(PlaybackService.EXTRA_HAS_QUEUE, false);
            playbackPlaying = intent.getBooleanExtra(PlaybackService.EXTRA_PLAYING, false);
            playbackPreparing = intent.getBooleanExtra(PlaybackService.EXTRA_PREPARING, false);
            playbackWillPlay = intent.getBooleanExtra(PlaybackService.EXTRA_WILL_PLAY, false);
            playbackError = intent.getBooleanExtra(PlaybackService.EXTRA_ERROR, false);
            playbackTitle = valueOrDefault(
                    intent.getStringExtra(PlaybackService.EXTRA_TITLE),
                    "로컬 재생 대기"
            );
            playbackMeta = valueOrDefault(
                    intent.getStringExtra(PlaybackService.EXTRA_META),
                    "기기 음악을 스캔하면 재생할 수 있습니다."
            );
            streamStatus = valueOrDefault(
                    intent.getStringExtra(PlaybackService.EXTRA_STATUS),
                    playbackMeta
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
            playbackMix = valueOrDefault(intent.getStringExtra(PlaybackService.EXTRA_MIX), "로컬 음악");
            playbackShuffleEnabled = intent.getBooleanExtra(PlaybackService.EXTRA_SHUFFLE_ENABLED, false);
            playbackRepeatMode = intent.getIntExtra(PlaybackService.EXTRA_REPEAT_MODE, PlaybackService.REPEAT_OFF);
            sleepTimerEndAtMs = intent.getLongExtra(PlaybackService.EXTRA_SLEEP_TIMER_END_AT_MS, 0L);
            sleepTimerRemainingMs = intent.getLongExtra(PlaybackService.EXTRA_SLEEP_TIMER_REMAINING_MS, 0L);
            sleepTimerPaused = intent.getBooleanExtra(PlaybackService.EXTRA_SLEEP_TIMER_PAUSED, false);
            updateQueuePreviewFromIds(intent.getLongArrayExtra(PlaybackService.EXTRA_QUEUE_TRACK_IDS));
            if (!playbackHasQueue) {
                activeStation = null;
                activeQueuePreview = new ArrayList<>();
                playbackQueueTrackIds = new long[0];
            }
            updateNowPlayingBar();
            updateExpandedPlayer();
        }
    };

    private final BroadcastReceiver updateDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                return;
            }
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, NO_DOWNLOAD_ID);
            if (downloadId == updateDownloadId && downloadId != NO_DOWNLOAD_ID) {
                handleUpdateDownloadComplete(downloadId);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outputTreeUri = getPreferences().getString(PREF_OUTPUT_TREE, null);
        librarySource = getPreferences().getString(PREF_LIBRARY_SOURCE, LIBRARY_SOURCE_COLLECTION);
        updateDownloadId = getPreferences().getLong(PREF_UPDATE_DOWNLOAD_ID, NO_DOWNLOAD_ID);
        autoUpdateCheck = getPreferences().getBoolean(PREF_AUTO_UPDATE_CHECK, true);
        ensureDefaultMediaFolders();
        clearInstalledPendingUpdateIfNeeded();
        setContentView(buildContent());
        if (autoUpdateCheck) {
            startUpdateCheck(false);
        }
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
            IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(updateDownloadReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(updateDownloadReceiver, filter);
            }
            updateReceiverRegistered = true;
        }
        refreshPendingUpdateDownloadState();
    }

    @Override
    protected void onStop() {
        if (playbackReceiverRegistered) {
            unregisterReceiver(playbackReceiver);
            playbackReceiverRegistered = false;
        }
        if (updateReceiverRegistered) {
            unregisterReceiver(updateDownloadReceiver);
            updateReceiverRegistered = false;
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
        libraryExecutor.shutdownNow();
        updateExecutor.shutdownNow();
        super.onDestroy();
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
            toast("선택한 파일을 삭제했습니다.");
            selectedTrack = null;
            pendingDeleteTrack = null;
            startLibraryRefresh(true);
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
            startHomeRefresh(true);
        } else {
            libraryStatus = "기기 음악을 관리하려면 오디오 읽기 권한이 필요합니다.";
            homeStatus = libraryStatus;
            renderLibraryDependentTabs();
        }
    }

    private View buildContent() {
        LinearLayout app = new LinearLayout(this);
        app.setOrientation(LinearLayout.VERTICAL);
        app.setBackgroundColor(color(R.color.ytet_background));

        FrameLayout contentFrame = new FrameLayout(this);
        contentScrollView = new PullRefreshScrollView(this);
        contentScrollView.setFillViewport(true);
        contentScrollView.setBackgroundColor(color(R.color.ytet_background));
        contentFrame.addView(contentScrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        FrameLayout.LayoutParams refreshParams = new FrameLayout.LayoutParams(
                dp(56),
                dp(56),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        contentFrame.addView(libraryPullRefreshIndicator(), refreshParams);
        app.addView(contentFrame, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        nowPlayingBar = buildNowPlayingBar();
        app.addView(nowPlayingBar, matchWrap());
        app.addView(buildBottomTabs(), matchWrap());

        renderCurrentTab();
        return app;
    }

    private LinearLayout buildNowPlayingBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(10), dp(16), dp(10));
        bar.setBackground(nowPlayingBarBackground(true));
        bar.setOnClickListener(view -> showExpandedPlayer());

        nowPlayingCover = new FrameLayout(this);
        setNowPlayingCover(true);
        bar.addView(nowPlayingCover, marginRight(10, dp(44), dp(44)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        nowPlayingTitle = text("로컬 재생 대기", 14, R.color.ytet_text, true);
        nowPlayingMeta = text("기기 음악을 스캔하면 재생할 수 있습니다.", 12, R.color.ytet_muted, false);
        copy.addView(nowPlayingTitle, matchWrap());
        copy.addView(nowPlayingMeta, matchWrap());
        bar.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        playPauseButton = iconButton(R.drawable.ic_play_arrow, "재생", true);
        playPauseButton.setOnClickListener(view -> toggleStreamPlayback());
        bar.addView(playPauseButton, new LinearLayout.LayoutParams(dp(48), dp(44)));
        return bar;
    }

    private LinearLayout buildBottomTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(10), dp(4), dp(10), dp(12));
        tabs.setBackgroundColor(0x800B0B0D);

        homeTabButton = tabButton("홈", Tab.HOME, R.drawable.ic_tab_home_outline, R.drawable.ic_tab_home_filled);
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
        root.setPadding(0, dp(2), 0, dp(2));
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setOnClickListener(view -> showTab(tab));

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        root.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView text = text(label, 11, R.color.ytet_muted, true);
        text.setGravity(Gravity.CENTER);
        text.setIncludeFontPadding(false);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, dp(3), 0, 0);
        root.addView(text, textParams);
        return new TabItem(root, icon, text, outlineIcon, filledIcon);
    }

    private void showTab(Tab tab) {
        saveCurrentTabInputs();
        currentTab = tab;
        renderCurrentTab();
    }

    private void renderCurrentTab() {
        if (contentScrollView == null) {
            return;
        }
        contentScrollView.removeAllViews();
        View view;
        if (currentTab == Tab.LIBRARY) {
            view = buildLibraryTab();
        } else if (currentTab == Tab.EXTRACTOR) {
            view = buildExtractorTab();
        } else {
            view = buildHomeTab();
        }
        contentScrollView.addView(view, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        updateTabStyles();
        updateNowPlayingBar();
        if (currentTab == Tab.LIBRARY) {
            updateLibraryPullIndicator(libraryLoading ? dp(92) : libraryPullDistance, libraryLoading || libraryPullReady);
        } else {
            resetLibraryPullIndicator();
        }
    }

    private void ensureDefaultMediaFolders() {
        try {
            new StorageWriter().ensureDefaultFolders();
        } catch (Exception ignored) {
            // MediaStore creates the public folders when the first file is saved on scoped-storage devices.
        }
    }

    private boolean handleLibraryPullToRefresh(MotionEvent event) {
        int action = event.getActionMasked();
        if (currentTab != Tab.LIBRARY || contentScrollView == null) {
            resetLibraryPullIndicator();
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            libraryPullTracking = contentScrollView.getScrollY() <= 0 && !libraryLoading;
            libraryPullStartY = event.getRawY();
            libraryPullDistance = 0f;
            libraryPullReady = false;
            updateLibraryPullIndicator(0f, false);
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE && libraryPullTracking) {
            float dragDistance = event.getRawY() - libraryPullStartY;
            if (dragDistance <= 0f || contentScrollView.getScrollY() > 0) {
                libraryPullDistance = 0f;
                libraryPullReady = false;
                updateLibraryPullIndicator(0f, false);
                return false;
            }
            libraryPullDistance = dragDistance;
            libraryPullReady = dragDistance >= dp(92);
            updateLibraryPullIndicator(dragDistance, libraryPullReady);
            return dragDistance > dp(6);
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            boolean shouldRefresh = action == MotionEvent.ACTION_UP && libraryPullTracking && libraryPullReady && !libraryLoading;
            libraryPullTracking = false;
            libraryPullDistance = 0f;
            libraryPullReady = false;
            updateLibraryPullIndicator(shouldRefresh ? dp(92) : 0f, shouldRefresh);
            if (shouldRefresh) {
                startLibraryRefresh(true);
                return true;
            }
        }
        return false;
    }

    private void resetLibraryPullIndicator() {
        libraryPullTracking = false;
        libraryPullDistance = 0f;
        libraryPullReady = false;
        updateLibraryPullIndicator(0f, false);
    }

    private void updateLibraryPullIndicator(float dragDistance, boolean ready) {
        if (libraryPullIndicator == null) {
            return;
        }
        boolean visible = libraryLoading || dragDistance > dp(8);
        float progress = Math.min(1f, Math.max(0f, dragDistance / dp(92)));
        float offset = libraryLoading || ready
                ? dp(28)
                : -dp(54) + Math.min(dp(82), dragDistance * 0.72f);
        libraryPullIndicator.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        libraryPullIndicator.setTranslationY(offset);
        libraryPullIndicator.setAlpha(libraryLoading || ready ? 1f : Math.max(0.35f, progress));
        float scale = 0.86f + progress * 0.14f;
        libraryPullIndicator.setScaleX(scale);
        libraryPullIndicator.setScaleY(scale);
        if (libraryPullIcon != null) {
            libraryPullIcon.setRotation(libraryLoading || ready ? 0f : progress * 180f);
        }
    }

    private View buildHomeTab() {
        LinearLayout root = screenRoot();
        if (!hasAudioPermission()) {
            LinearLayout permission = topAlignedPanel();
            permission.addView(label("오디오 권한 필요"), marginBottom(8));
            permission.addView(muted("홈 추천 믹스는 YTET 보관함 음악 메타데이터와 폴더를 읽은 뒤 생성됩니다.", 14), marginBottom(14));
            Button request = primaryButton("권한 허용");
            request.setOnClickListener(view -> requestAudioPermission());
            permission.addView(request, matchWrap());
            root.addView(permission, marginBottom(18));
            return root;
        }

        if (!homeLoaded && !homeLoading) {
            startHomeRefresh(false);
        }

        LinearLayout hero = topAlignedPanel();
        hero.addView(label("내 음악 바로 듣기"), marginBottom(8));
        if (!homeStatus.trim().isEmpty()) {
            hero.addView(text(homeStatus, 15, R.color.ytet_text, false), marginBottom(12));
        }
        hero.addView(muted(homeSummary(), 13), marginBottom(14));
        Button primaryPlay = primaryButton("보관함 추천 재생");
        primaryPlay.setEnabled(!homeTracks.isEmpty());
        primaryPlay.setOnClickListener(view -> playStation(firstHomeStation(), homeTracks));
        hero.addView(primaryPlay, matchWrap());
        root.addView(hero, marginBottom(24));

        root.addView(sectionTitle("추천 믹스"), marginBottom(10));
        List<MusicStation> stations = StationCatalog.recommendedStations(homeTracks);
        if (stations.isEmpty()) {
            LinearLayout empty = panel();
            empty.addView(label("추천할 음악이 없습니다."), marginBottom(8));
            empty.addView(muted("추출한 음원이 " + DefaultMediaPaths.displayPath(MediaType.AUDIO)
                    + "에 저장되면 홈 추천 스테이션에 반영됩니다.", 13), matchWrap());
            root.addView(empty, marginBottom(18));
            return root;
        }
        HorizontalScrollView shelf = new HorizontalScrollView(this);
        shelf.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (MusicStation station : stations) {
            row.addView(stationCard(station, homeTracks), marginRight(12, dp(184), LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        shelf.addView(row, matchWrap());
        root.addView(shelf, marginBottom(24));
        return root;
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
        panel.addView(muted("GitHub의 정식 버전 릴리즈만 확인합니다. Nightly와 prerelease는 건너뜁니다.", 13), marginBottom(10));
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
            installDownloadedUpdate(updateDownloadId);
            return;
        }
        if (availableUpdate != null) {
            downloadUpdate(availableUpdate);
            return;
        }
        startUpdateCheck(true);
    }

    private void startUpdateCheck(boolean manual) {
        if (updateChecking) {
            return;
        }
        updateChecking = true;
        updateStatus = "정식 릴리즈 업데이트를 확인하는 중입니다.";
        renderUpdateState();
        String currentVersionName = currentAppVersionName();
        updateExecutor.execute(() -> {
            try {
                UpdateInfo update = updateChecker.checkForStableUpdate(currentVersionName);
                runOnUiThread(() -> {
                    updateChecking = false;
                    updateChecked = true;
                    availableUpdate = update;
                    if (update == null) {
                        updateStatus = "현재 설치된 " + currentVersionName + " 버전이 최신 정식 버전입니다.";
                    } else {
                        updateStatus = update.tagName() + " 정식 업데이트를 사용할 수 있습니다.";
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
        try {
            DownloadManager manager = downloadManager();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(update.apkUrl()));
            request.setTitle("YTET " + update.tagName());
            request.setDescription("정식 업데이트 APK 다운로드");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(
                    this,
                    Environment.DIRECTORY_DOWNLOADS,
                    "updates/" + update.tagName() + "-" + System.currentTimeMillis() + ".apk"
            );
            updateDownloadId = manager.enqueue(request);
            updateDownloading = true;
            updateStatus = update.tagName() + " 업데이트 APK를 다운로드하는 중입니다.";
            getPreferences().edit()
                    .putLong(PREF_UPDATE_DOWNLOAD_ID, updateDownloadId)
                    .putString(PREF_UPDATE_TAG, update.tagName())
                    .apply();
            renderUpdateState();
        } catch (Exception exception) {
            updateDownloading = false;
            updateStatus = "업데이트 다운로드를 시작할 수 없습니다: " + safeMessage(exception);
            renderUpdateState();
        }
    }

    private void handleUpdateDownloadComplete(long downloadId) {
        int status = downloadStatus(downloadId);
        updateDownloading = false;
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            updateStatus = "업데이트 다운로드가 완료되었습니다. 설치 화면을 여는 중입니다.";
            renderUpdateState();
            installDownloadedUpdate(downloadId);
            return;
        }
        clearPendingUpdateDownload();
        updateStatus = "업데이트 다운로드에 실패했습니다.";
        renderUpdateState();
    }

    private void installDownloadedUpdate(long downloadId) {
        if (downloadId == NO_DOWNLOAD_ID || downloadStatus(downloadId) != DownloadManager.STATUS_SUCCESSFUL) {
            toast("설치할 업데이트 APK가 아직 준비되지 않았습니다.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            updateStatus = "설치를 계속하려면 YTET의 알 수 없는 앱 설치를 허용한 뒤 설치를 다시 누르세요.";
            renderUpdateState();
            openInstallPermissionSettings();
            return;
        }
        Uri apkUri = downloadManager().getUriForDownloadedFile(downloadId);
        if (apkUri == null) {
            updateStatus = "다운로드한 APK를 열 수 없습니다. 다시 다운로드하세요.";
            renderUpdateState();
            return;
        }

        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(apkUri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(install);
            updateStatus = "Android 설치 화면에서 업데이트를 승인하세요.";
            renderUpdateState();
        } catch (ActivityNotFoundException exception) {
            updateStatus = "APK 설치 화면을 열 수 없습니다.";
            renderUpdateState();
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
        if (updateDownloadId == NO_DOWNLOAD_ID) {
            return;
        }
        int status = downloadStatus(updateDownloadId);
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            updateDownloading = false;
            String tag = getPreferences().getString(PREF_UPDATE_TAG, "다운로드한 업데이트");
            updateStatus = tag + " APK 다운로드가 완료되었습니다. 설치할 수 있습니다.";
            renderUpdateState();
        } else if (status == DownloadManager.STATUS_FAILED) {
            clearPendingUpdateDownload();
            updateDownloading = false;
            updateStatus = "이전 업데이트 다운로드가 실패했습니다.";
            renderUpdateState();
        } else if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING) {
            updateDownloading = true;
            updateStatus = "업데이트 APK를 다운로드하는 중입니다.";
            renderUpdateState();
        }
    }

    private boolean isDownloadedUpdateReady() {
        return updateDownloadId != NO_DOWNLOAD_ID
                && downloadStatus(updateDownloadId) == DownloadManager.STATUS_SUCCESSFUL;
    }

    private void clearInstalledPendingUpdateIfNeeded() {
        String tag = getPreferences().getString(PREF_UPDATE_TAG, "");
        if (!tag.isEmpty() && UpdateChecker.compareStableTagToCurrentVersion(tag, currentAppVersionName()) <= 0) {
            clearPendingUpdateDownload();
            updateDownloading = false;
        }
    }

    private int downloadStatus(long downloadId) {
        if (downloadId == NO_DOWNLOAD_ID) {
            return -1;
        }
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        try (Cursor cursor = downloadManager().query(query)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return -1;
            }
            int column = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            return column < 0 ? -1 : cursor.getInt(column);
        } catch (Exception exception) {
            return -1;
        }
    }

    private void clearPendingUpdateDownload() {
        updateDownloadId = NO_DOWNLOAD_ID;
        getPreferences().edit()
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

    private DownloadManager downloadManager() {
        return (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
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

    private View buildLibraryTab() {
        LinearLayout root = screenRoot();
        libraryTrackItemViews.clear();

        if (!hasAudioPermission()) {
            LinearLayout permission = panel();
            permission.addView(label("오디오 권한 필요"), marginBottom(8));
            permission.addView(muted("Android 미디어 저장소에서 음악 파일을 읽어 앨범과 아티스트로 정리합니다.", 14), marginBottom(14));
            Button request = primaryButton("권한 허용");
            request.setOnClickListener(view -> requestAudioPermission());
            permission.addView(request, matchWrap());
            root.addView(permission, marginBottom(16));
            return root;
        }

        if (!libraryLoaded && !libraryLoading) {
            startLibraryRefresh(false);
        }

        if (focusedLibraryGroup != null && focusedLibraryGroupFilter == LibraryFilter.ALBUM) {
            LibraryGroup group = currentFocusedLibraryGroup();
            if (group != null && !group.tracks.isEmpty()) {
                buildLibraryGroupDetail(root, group);
                return root;
            }
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
        }

        root.addView(librarySearchToolbar(), marginBottom(8));
        root.addView(libraryFilterBar(), marginBottom(shouldShowLibrarySearchInput() ? 8 : 12));
        if (shouldShowLibrarySearchInput()) {
            root.addView(librarySearchInputRow(), marginBottom(10));
        }
        if (!libraryStatus.trim().isEmpty()) {
            root.addView(libraryViewToolbar(), marginBottom(14));
        }

        if (libraryFilter == LibraryFilter.ALL) {
            List<DeviceAudioTrack> visibleTracks = visibleLibraryTracks();
            if (visibleTracks.isEmpty()) {
                root.addView(emptyLibraryView(), matchWrap());
                return root;
            }
            int limit = Math.min(visibleTracks.size(), 80);
            if (libraryGridView) {
                addTrackCardGrid(root, visibleTracks, limit);
            } else {
                for (int i = 0; i < limit; i++) {
                    root.addView(trackRow(visibleTracks.get(i)), marginBottom(8));
                }
            }
            if (visibleTracks.size() > limit) {
                root.addView(muted("상위 " + limit + "곡만 표시 중입니다. 검색하면 목록을 좁힐 수 있습니다.", 12), matchWrap());
            }
            return root;
        }

        List<LibraryGroup> visibleGroups = visibleLibraryGroups();
        if (visibleGroups.isEmpty()) {
            root.addView(emptyLibraryView(), matchWrap());
            return root;
        }
        int limit = Math.min(visibleGroups.size(), 80);
        if (libraryGridView) {
            addLibraryGroupCardGrid(root, visibleGroups, limit);
        } else {
            for (int i = 0; i < limit; i++) {
                root.addView(libraryGroupRow(visibleGroups.get(i)), marginBottom(8));
            }
        }
        if (visibleGroups.size() > limit) {
            root.addView(muted("상위 " + limit + "개만 표시 중입니다. 검색하면 목록을 좁힐 수 있습니다.", 12), matchWrap());
        }
        return root;
    }

    private View emptyLibraryView() {
        LinearLayout empty = panel();
        boolean searching = !librarySearchQuery.trim().isEmpty();
        empty.addView(label(searching ? "검색 결과가 없습니다." : "표시할 음악이 없습니다."), marginBottom(8));
        empty.addView(muted(searching
                ? "다른 검색어를 입력하거나 전체/앨범/아티스트 필터를 바꿔보세요."
                : emptyLibraryHint(), 13), matchWrap());
        return empty;
    }

    private View libraryFilterBar() {
        HorizontalScrollView shelf = new HorizontalScrollView(this);
        shelf.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(libraryFilterChip("전체", LibraryFilter.ALL), marginRight(8, LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        row.addView(libraryFilterChip("앨범", LibraryFilter.ALBUM), marginRight(8, LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        row.addView(libraryFilterChip("아티스트", LibraryFilter.ARTIST), marginRight(0, LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        shelf.addView(row, matchWrap());
        return shelf;
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
            libraryFilter = filter;
            selectedTrack = null;
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
            renderCurrentTab();
        });
        return chip;
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
        FrameLayout container = new FrameLayout(this);
        container.setVisibility(View.INVISIBLE);
        container.setBackground(rounded(Color.WHITE, 28));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_refresh);
        icon.setColorFilter(Color.BLACK);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        container.addView(icon, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        libraryPullIndicator = container;
        libraryPullIcon = icon;
        updateLibraryPullIndicator(0f, false);
        return container;
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

        ImageButton search = toolbarIconButton(R.drawable.ic_search, "검색", shouldShowLibrarySearchInput());
        search.setOnClickListener(view -> {
            if (!shouldShowLibrarySearchInput()) {
                librarySearchVisible = true;
                renderCurrentTab();
                return;
            }
            focusLibrarySearchInput();
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
        librarySearchInput = new EditText(this);
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
        librarySearchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
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
                applyLibrarySearch(text == null ? "" : text.toString());
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
                "YTET/Music에 저장된 음악만 보기",
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
        libraryLoaded = false;
        startLibraryRefresh(true);
    }

    private String librarySourceLabel() {
        return isDeviceFileSource() ? "기기 파일" : "보관함";
    }

    private boolean isDeviceFileSource() {
        return LIBRARY_SOURCE_DEVICE.equals(librarySource);
    }

    private void applyLibrarySearch(String query) {
        String nextQuery = query == null ? "" : query;
        if (nextQuery.equals(librarySearchQuery)) {
            return;
        }
        librarySearchQuery = nextQuery;
        librarySearchVisible = true;
        librarySearchInput = null;
        selectedTrack = null;
        renderCurrentTab();
    }

    private void focusLibrarySearchInput() {
        if (librarySearchInput == null) {
            return;
        }
        librarySearchInput.requestFocus();
        librarySearchInput.setSelection(librarySearchInput.getText().length());
    }

    private boolean shouldShowLibrarySearchInput() {
        return librarySearchVisible || !librarySearchQuery.trim().isEmpty();
    }

    private List<DeviceAudioTrack> visibleLibraryTracks() {
        String query = librarySearchQuery == null ? "" : librarySearchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return new ArrayList<>(libraryTracks);
        }

        List<DeviceAudioTrack> matches = new ArrayList<>();
        for (DeviceAudioTrack track : libraryTracks) {
            if (trackMatchesQuery(track, query)) {
                matches.add(track);
            }
        }
        return matches;
    }

    private List<LibraryGroup> visibleLibraryGroups() {
        Map<String, List<DeviceAudioTrack>> grouped = new LinkedHashMap<>();
        for (DeviceAudioTrack track : visibleLibraryTracks()) {
            String key = libraryGroupKey(track, libraryFilter);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(track);
        }

        List<LibraryGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<DeviceAudioTrack>> entry : grouped.entrySet()) {
            groups.add(libraryGroup(entry.getKey(), entry.getValue(), libraryFilter));
        }
        groups.sort((first, second) -> first.title.compareToIgnoreCase(second.title));
        return groups;
    }

    private String libraryGroupKey(DeviceAudioTrack track, LibraryFilter filter) {
        return filter == LibraryFilter.ALBUM ? track.album() : track.artist();
    }

    private LibraryGroup libraryGroup(String title, List<DeviceAudioTrack> tracks, LibraryFilter filter) {
        String subtitle;
        if (filter == LibraryFilter.ALBUM) {
            subtitle = albumArtistSummary(tracks) + " · " + tracks.size() + "곡 · " + totalDurationLabel(tracks);
        } else {
            subtitle = distinctAlbumCount(tracks) + "개 앨범 · " + tracks.size() + "곡 · " + totalDurationLabel(tracks);
        }
        return new LibraryGroup(title, subtitle, bestCoverTrack(tracks), tracks);
    }

    private LibraryGroup currentFocusedLibraryGroup() {
        if (focusedLibraryGroup == null || focusedLibraryGroupFilter == null) {
            return null;
        }
        List<DeviceAudioTrack> tracks = new ArrayList<>();
        for (DeviceAudioTrack track : libraryTracks) {
            if (focusedLibraryGroup.title.equals(libraryGroupKey(track, focusedLibraryGroupFilter))) {
                tracks.add(track);
            }
        }
        if (tracks.isEmpty()) {
            return focusedLibraryGroup;
        }
        return libraryGroup(focusedLibraryGroup.title, tracks, focusedLibraryGroupFilter);
    }

    private String albumArtistSummary(List<DeviceAudioTrack> tracks) {
        String artist = "";
        for (DeviceAudioTrack track : tracks) {
            if (artist.isEmpty()) {
                artist = track.artist();
                continue;
            }
            if (!artist.equals(track.artist())) {
                return "여러 아티스트";
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

    private void addLibraryGroupCardGrid(LinearLayout root, List<LibraryGroup> groups, int limit) {
        for (int index = 0; index < limit; index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(libraryGroupCard(groups.get(index)), cardColumnParams(8));
            if (index + 1 < limit) {
                row.addView(libraryGroupCard(groups.get(index + 1)), cardColumnParams(0));
            } else {
                row.addView(new View(this), cardColumnParams(0));
            }
            root.addView(row, marginBottom(10));
        }
    }

    private View libraryGroupCard(LibraryGroup group) {
        LinearLayout card = panel();
        card.setPadding(dp(10), dp(10), dp(10), dp(12));
        card.setOnClickListener(view -> handleLibraryGroupClick(group));

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
        if (libraryFilter == LibraryFilter.ALBUM) {
            focusedLibraryGroup = group;
            focusedLibraryGroupFilter = LibraryFilter.ALBUM;
            selectedTrack = null;
            renderCurrentTab();
            return;
        }
        playLibraryGroup(group);
    }

    private void buildLibraryGroupDetail(LinearLayout root, LibraryGroup group) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back = toolbarIconButton(R.drawable.ic_arrow_back, "앨범 목록", false);
        back.setOnClickListener(view -> {
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
            renderCurrentTab();
        });
        top.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, dp(44), 1f));
        ImageButton search = toolbarIconButton(R.drawable.ic_search, "검색", false);
        search.setOnClickListener(view -> {
            focusedLibraryGroup = null;
            focusedLibraryGroupFilter = null;
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
        more.setOnClickListener(view -> showLibraryGroupDetails(group));
        copy.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));
        hero.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(hero, marginBottom(24));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button play = primaryButton("▶  재생");
        play.setOnClickListener(view -> playLibraryGroup(group, 0, false));
        actions.addView(play, weightedControlParams(1, 10));
        Button shuffle = secondaryButton("♢  셔플");
        shuffle.setOnClickListener(view -> playLibraryGroup(group, 0, true));
        actions.addView(shuffle, weightedControlParams(1, 0));
        root.addView(actions, marginBottom(22));

        for (int index = 0; index < group.tracks.size(); index++) {
            root.addView(libraryGroupTrackRow(group, group.tracks.get(index), index), marginBottom(6));
        }
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

    private void showLibraryGroupDetails(LibraryGroup group) {
        LinearLayout body = dialogBody("상세정보");
        body.addView(trackDetailItem("앨범", group.title), marginBottom(10));
        body.addView(trackDetailItem("아티스트", albumArtistSummary(group.tracks)), marginBottom(10));
        body.addView(trackDetailItem("수록곡", group.tracks.size() + "곡"), marginBottom(10));
        body.addView(trackDetailItem("전체 재생시간", totalDurationLabel(group.tracks)), marginBottom(14));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(body)
                .create();
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button close = detailActionButton("닫기");
        close.setOnClickListener(view -> dialog.dismiss());
        actions.addView(close, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
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
        String category = groupFilter == LibraryFilter.ALBUM ? "앨범" : "아티스트";
        MusicStation station = new MusicStation(
                category + "-" + Integer.toHexString(group.title.hashCode()) + (shuffle ? "-shuffle" : "-ordered"),
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
        return containsIgnoreCase(track.title(), query)
                || containsIgnoreCase(track.artist(), query)
                || containsIgnoreCase(track.album(), query)
                || containsIgnoreCase(track.folder(), query)
                || containsIgnoreCase(track.displayName(), query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void addTrackCardGrid(LinearLayout root, List<DeviceAudioTrack> visibleTracks, int limit) {
        for (int index = 0; index < limit; index += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(trackCard(visibleTracks.get(index)), cardColumnParams(8));
            if (index + 1 < limit) {
                row.addView(trackCard(visibleTracks.get(index + 1)), cardColumnParams(0));
            } else {
                row.addView(new View(this), cardColumnParams(0));
            }
            root.addView(row, marginBottom(10));
        }
    }

    private View trackCard(DeviceAudioTrack track) {
        LinearLayout card = panel();
        card.setPadding(dp(10), dp(10), dp(10), dp(12));
        applyTrackSelectionBackground(card, track);
        card.setOnClickListener(view -> {
            selectLibraryTrack(track);
            playTrack(track);
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
            playTrack(track);
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
        selectedTrack = track;
        refreshVisibleTrackSelection();
    }

    private void registerLibraryTrackView(DeviceAudioTrack track, View view) {
        if (track == null || view == null) {
            return;
        }
        libraryTrackItemViews.put(track.id(), view);
    }

    private void refreshVisibleTrackSelection() {
        for (DeviceAudioTrack track : visibleLibraryTracks()) {
            View view = libraryTrackItemViews.get(track.id());
            if (view != null) {
                applyTrackSelectionBackground(view, track);
            }
        }
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
        if (track.albumArtUri() != null && !track.albumArtUri().trim().isEmpty()) {
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

    private String trackInitials(DeviceAudioTrack track) {
        String title = track == null ? "" : track.title().trim();
        if (title.isEmpty()) {
            return "YT";
        }
        return title.substring(0, Math.min(2, title.length())).toUpperCase(Locale.ROOT);
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
        Button delete = detailActionButton("삭제");
        delete.setOnClickListener(view -> {
            selectedTrack = track;
            dialog.dismiss();
            deleteSelectedTrack();
        });
        actions.addView(share, fixedButtonParams(76, 38, 8));
        actions.addView(delete, fixedButtonParams(76, 38, 0));
        body.addView(actions, matchWrap());
        dialog.show();
        styleDetailDialog(dialog);
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
        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setText(extractorUrl);
        urlInput.setHint("https://youtu.be/...");
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        styleInput(urlInput);
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

        extractButton = primaryButton("추출");
        extractButton.setOnClickListener(view -> startExtraction());
        root.addView(extractButton, marginBottom(18));

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
        startPlayback(PlaybackService.playQueueIntent(this, station, activeQueuePreview, index));
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
                && PlaybackService.ACTION_PLAY_QUEUE.equals(intent.getAction())) {
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
        if (!playbackHasQueue && activeStation == null) {
            updatePlaybackThemeColor(true);
            nowPlayingBar.setBackground(nowPlayingBarBackground(true));
            setNowPlayingCover(true);
            nowPlayingTitle.setText("로컬 재생 대기");
            nowPlayingMeta.setText("기기 음악을 스캔하면 재생할 수 있습니다.");
            playPauseButton.setImageResource(R.drawable.ic_play_arrow);
            playPauseButton.setContentDescription("재생");
            return;
        }
        updatePlaybackThemeColor(false);
        nowPlayingBar.setBackground(nowPlayingBarBackground(false));
        setNowPlayingCover(false);
        nowPlayingTitle.setText(playbackTitle);
        nowPlayingMeta.setText(playbackPreparing || playbackError ? streamStatus : miniPlaybackMeta());
        boolean waitingToPlay = playbackPlaying || playbackWillPlay;
        playPauseButton.setImageResource(waitingToPlay ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        playPauseButton.setContentDescription(waitingToPlay ? "일시정지" : "재생");
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
        if (!idle && playbackAlbumArtUri != null && !playbackAlbumArtUri.trim().isEmpty()) {
            ImageView image = new ImageView(this);
            image.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageURI(Uri.parse(playbackAlbumArtUri));
            return image;
        }
        TextView placeholder = text(idle ? "YT" : coverInitials(), 13, android.R.color.white, true);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackground(rounded(idle ? color(R.color.ytet_panel_alt) : color(R.color.ytet_accent_dark), 8));
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
        playbackThemeColor = artworkUri.isEmpty()
                ? fallbackPlayerThemeColor(idle)
                : readArtworkThemeColor(artworkUri);
    }

    private GradientDrawable nowPlayingBarBackground(boolean idle) {
        int base = idle ? color(R.color.ytet_panel) : playbackThemeColor;
        int end = idle
                ? color(R.color.ytet_panel)
                : blendColors(base, color(R.color.ytet_panel), 0.46f);
        return new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{base, end});
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
        applyOpaqueDialogBars(window, statusColor, navigationColor);
    }

    private void applyQueueWindow(Window window) {
        if (window == null) {
            return;
        }
        int background = color(R.color.ytet_background);
        applyOpaqueDialogBars(window, background, background);
    }

    private void applyOpaqueDialogBars(Window window, int statusColor, int navigationColor) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
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

    private int playerStatusBarColor() {
        int base = playbackThemeColor;
        float[] hsv = new float[3];
        Color.colorToHSV(base, hsv);
        hsv[1] = Math.min(1f, Math.max(0.42f, hsv[1] * 1.18f));
        hsv[2] = Math.max(0.08f, Math.min(0.22f, hsv[2] * 0.58f));
        return Color.HSVToColor(hsv);
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
            playerDialog = new Dialog(this);
            playerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        playerDialog.setContentView(buildExpandedPlayerContent());
        playerDialog.show();
        applyExpandedPlayerWindow(playerDialog.getWindow());
    }

    private void updateExpandedPlayer() {
        if (playerDialog == null || !playerDialog.isShowing() || suppressPlayerDragDismiss || playbackSeeking) {
            return;
        }
        playerDialog.setContentView(buildExpandedPlayerContent());
        applyExpandedPlayerWindow(playerDialog.getWindow());
    }

    private View buildExpandedPlayerContent() {
        DragDismissLayout root = new DragDismissLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        root.setPadding(dp(20), dp(22), dp(20), dp(24));
        updatePlaybackThemeColor(false);
        root.setBackground(expandedPlayerBackground(false));
        root.setPlayerSurfaceStyle(true);

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
        root.addView(top, marginBottom(26));

        root.addView(coverArtView(), coverParams());
        if (hasSleepTimer()) {
            root.addView(activeSleepTimerRow(), marginBottom(8));
        }
        root.addView(text(playbackTitle, 23, R.color.ytet_text, true), marginBottom(4));
        root.addView(muted(playbackArtist + " · " + playbackAlbum, 14), marginBottom(2));
        root.addView(muted(queuePositionText(), 12), marginBottom(8));

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
        root.addView(progress, controlParams(42, 0));
        root.addView(muted(playbackProgressText(), 12), marginBottom(10));
        root.addView(new View(this), new LinearLayout.LayoutParams(
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
        controls.addView(shuffle, playerControlParams(5));
        controls.addView(previous, playerControlParams(5));
        controls.addView(play, playerControlParams(5));
        controls.addView(next, playerControlParams(5));
        controls.addView(repeat, playerControlParams(0));
        root.addView(controls, marginBottom(6));

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        boolean timerSelected = hasSleepTimer() || (sleepTimerControlsVisible && sleepTimerMinutes > 0);
        ImageButton timer = playerIconButton(R.drawable.ic_timer, "슬립 타이머", timerSelected, true);
        timer.setOnClickListener(view -> {
            if (sleepTimerControlsVisible) {
                applySleepTimer(sleepTimerMinutes);
                sleepTimerControlsVisible = false;
            } else {
                if (hasSleepTimer()) {
                    sleepTimerMinutes = remainingSleepTimerMinutes();
                }
                sleepTimerControlsVisible = true;
            }
            updateExpandedPlayer();
        });
        tools.addView(timer, new LinearLayout.LayoutParams(dp(58), dp(58)));
        View timerControls = sleepTimerControlsVisible ? sleepTimerControlsPanel() : nextTrackInfoPanel();
        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        );
        timerParams.setMargins(dp(8), 0, dp(8), 0);
        tools.addView(timerControls, timerParams);
        ImageButton queue = playerIconButton(R.drawable.ic_queue_music, "재생목록", false, playbackHasQueue);
        queue.setOnClickListener(view -> showQueueDialog());
        tools.addView(queue, new LinearLayout.LayoutParams(dp(58), dp(58)));
        root.addView(tools, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(98)
        ));
        return root;
    }

    private View coverArtView() {
        if (playbackAlbumArtUri != null && !playbackAlbumArtUri.trim().isEmpty()) {
            ImageView image = new ImageView(this);
            image.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageURI(Uri.parse(playbackAlbumArtUri));
            return image;
        }
        TextView placeholder = text(coverInitials(), 56, android.R.color.white, true);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackground(rounded(color(R.color.ytet_accent_dark), 8));
        return placeholder;
    }

    private View activeSleepTimerRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView remaining = muted(sleepTimerInlineText(), 14);
        remaining.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(remaining, new LinearLayout.LayoutParams(0, dp(42), 1f));

        ImageButton toggle = playerIconButton(
                sleepTimerPaused ? R.drawable.ic_play_arrow : R.drawable.ic_pause,
                sleepTimerPaused ? "슬립 타이머 재개" : "슬립 타이머 일시정지",
                false,
                true
        );
        toggle.setPadding(dp(9), dp(9), dp(9), dp(9));
        toggle.setOnClickListener(view -> toggleSleepTimerPause());
        row.addView(toggle, marginRight(4, dp(42), dp(42)));

        ImageButton cancel = playerIconButton(R.drawable.ic_close, "슬립 타이머 끄기", false, true);
        cancel.setPadding(dp(9), dp(9), dp(9), dp(9));
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
        String prefix = sleepTimerPaused ? "슬립 타이머 일시정지 · " : "슬립 타이머 · ";
        return prefix + MusicLibrary.formatDuration(remainingSleepTimerMs());
    }

    private String coverInitials() {
        String title = playbackTitle == null ? "" : playbackTitle.trim();
        if (title.isEmpty() || "로컬 재생 대기".equals(title)) {
            return "YTET";
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
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        boolean current = track.id() == playbackTrackId
                || (playbackTrackId < 0L && index == playbackQueueIndex);
        row.setBackground(rounded(current ? color(R.color.ytet_panel_alt) : color(R.color.ytet_panel), 8));

        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        coverParams.setMargins(0, 0, dp(12), 0);
        row.addView(trackCoverView(track), coverParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(track.title(), 14, current ? R.color.ytet_text : R.color.ytet_muted, current);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(title, marginBottom(3));
        TextView meta = muted(track.artist() + " · " + MusicLibrary.formatDuration(track.durationMs()), 12);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(meta, matchWrap());
        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.setOnClickListener(view -> playQueueTrack(track, index));
        return row;
    }

    private String queuePositionText() {
        if (playbackQueueIndex < 0 || playbackQueueSize <= 0) {
            return "";
        }
        return (playbackQueueIndex + 1) + "/" + playbackQueueSize;
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

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(0, 0, dp(8), 0);
        copy.addView(label("슬립 타이머"), marginBottom(2));
        copy.addView(muted(sleepTimerStatusText(), 12), matchWrap());
        panel.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        SleepTimerDialView dial = new SleepTimerDialView(this);
        dial.setSelectedMinutes(sleepTimerMinutes);
        dial.setTimerActive(hasSleepTimer());
        dial.setOnTimerChangeListener((minutes, committed) -> {
            sleepTimerMinutes = minutes;
            if (committed) {
                updateExpandedPlayer();
            }
        });
        panel.addView(dial, new LinearLayout.LayoutParams(dp(104), LinearLayout.LayoutParams.MATCH_PARENT));
        return panel;
    }

    private void applySleepTimer(int minutes) {
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

    private String sleepTimerStatusText() {
        if (sleepTimerMinutes <= 0) {
            return "다이얼을 드래그해서 설정";
        }
        return "← 아이콘을 클릭해서 설정 완료";
    }

    private void showQueueDialog() {
        if (queueDialog == null) {
            queueDialog = new Dialog(this);
            queueDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        queueDialog.setContentView(buildQueueDialogContent());
        queueDialog.show();
        applyQueueWindow(queueDialog.getWindow());
    }

    private void updateQueueDialog() {
        if (queueDialog != null && queueDialog.isShowing()) {
            queueDialog.setContentView(buildQueueDialogContent());
            applyQueueWindow(queueDialog.getWindow());
        }
    }

    private View buildQueueDialogContent() {
        DragDismissLayout root = new DragDismissLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(30), dp(20), dp(28));
        root.setBackgroundColor(color(R.color.ytet_background));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton close = playerIconButton(R.drawable.ic_keyboard_arrow_down, "재생목록 닫기", false, true);
        close.setOnClickListener(view -> {
            if (queueDialog != null) {
                queueDialog.dismiss();
            }
        });
        top.addView(close, new LinearLayout.LayoutParams(dp(44), dp(42)));
        TextView title = text("재생목록", 17, R.color.ytet_text, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(dp(44), dp(42)));
        root.addView(top, marginBottom(18));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        if (activeQueuePreview.isEmpty()) {
            list.addView(muted("재생목록을 불러오는 중입니다.", 14), matchWrap());
        } else {
            addQueueSection(list, "이전 곡", 0, Math.max(0, playbackQueueIndex), true);
            addQueueSection(list, "현재 곡", Math.max(0, playbackQueueIndex), Math.min(activeQueuePreview.size(), playbackQueueIndex + 1), false);
            addQueueSection(list, "다음 곡", Math.max(0, playbackQueueIndex + 1), activeQueuePreview.size(), false);
        }
        scroll.addView(list, matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        return root;
    }

    private void addQueueSection(LinearLayout list, String title, int from, int to, boolean compactPrevious) {
        if (from >= to || from >= activeQueuePreview.size()) {
            return;
        }
        list.addView(sectionTitle(title), marginBottom(10));
        int start = compactPrevious ? Math.max(from, to - 5) : from;
        int end = Math.min(to, activeQueuePreview.size());
        for (int index = start; index < end; index++) {
            list.addView(queueRow(activeQueuePreview.get(index), index), marginBottom(8));
        }
        if (compactPrevious && start > from) {
            list.addView(muted("이전 " + (start - from) + "곡은 접혀 있습니다.", 12), marginBottom(12));
        }
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
        homeStatus = "보관함 " + DefaultMediaPaths.displayPath(MediaType.AUDIO) + " 경로를 스캔하는 중입니다.";
        if (renderImmediately && currentTab == Tab.HOME) {
            renderCurrentTab();
        }
        libraryExecutor.execute(() -> {
            try {
                List<DeviceAudioTrack> tracks = deviceMusicLibrary.loadTracks(
                        this,
                        Arrays.asList(DefaultMediaPaths.musicRelativePath())
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

    private void startLibraryRefresh(boolean renderImmediately) {
        if (!hasAudioPermission()) {
            requestAudioPermission();
            return;
        }
        libraryLoading = true;
        libraryStatus = isDeviceFileSource()
                ? "기기 파일의 음악을 스캔하는 중입니다."
                : "보관함 " + DefaultMediaPaths.displayPath(MediaType.AUDIO) + " 경로를 스캔하는 중입니다.";
        if (renderImmediately && (currentTab == Tab.HOME || currentTab == Tab.LIBRARY)) {
            renderCurrentTab();
        }
        libraryExecutor.execute(() -> {
            try {
                List<DeviceAudioTrack> tracks = deviceMusicLibrary.loadTracks(this, libraryScanRelativePaths());
                runOnUiThread(() -> {
                    libraryTracks = tracks;
                    libraryLoaded = true;
                    libraryLoading = false;
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
                    selectedTrack = null;
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
        return Arrays.asList(DefaultMediaPaths.musicRelativePath());
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
        pendingDeleteTrack = selectedTrack;
        Uri uri = Uri.parse(selectedTrack.contentUri());
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                List<Uri> uris = new ArrayList<>();
                uris.add(uri);
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
            deleteTrackDirectly(selectedTrack);
        } catch (IntentSender.SendIntentException exception) {
            toast("삭제 확인 화면을 열 수 없습니다.");
        } catch (SecurityException exception) {
            toast("이 파일을 삭제할 권한이 없습니다.");
        }
    }

    private void handleWritePermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && pendingDeleteTrack != null) {
            deleteTrackDirectly(pendingDeleteTrack);
        } else {
            toast("파일 삭제에는 저장소 쓰기 권한이 필요합니다.");
        }
    }

    private void deleteTrackDirectly(DeviceAudioTrack track) {
        Uri uri = Uri.parse(track.contentUri());
        try {
            int deleted = getContentResolver().delete(uri, null, null);
            toast(deleted > 0 ? "선택한 파일을 삭제했습니다." : "삭제할 수 없는 파일입니다.");
            selectedTrack = null;
            pendingDeleteTrack = null;
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
    }

    private void saveCurrentTabInputs() {
        if (currentTab == Tab.EXTRACTOR) {
            saveExtractorInputs();
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

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private LinearLayout screenRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(32), dp(20), dp(34));
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
        button.setAlpha(enabled ? 1f : 0.48f);
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

    private GradientDrawable rounded(int fillColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(50), 1f);
        params.setMargins(0, 0, dp(rightDp), 0);
        return params;
    }

    private LinearLayout.LayoutParams coverParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(284)
        );
        params.setMargins(0, 0, 0, dp(14));
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

    private LinearLayout.LayoutParams tabParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
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

    private final class PullRefreshScrollView extends ScrollView {
        PullRefreshScrollView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (handleLibraryPullToRefresh(event)) {
                return true;
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
        private boolean dragCanStart;
        private boolean draggingDown;
        private boolean playerSurfaceStyle;

        DragDismissLayout(Context context) {
            super(context);
        }

        void setPlayerSurfaceStyle(boolean enabled) {
            playerSurfaceStyle = enabled;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                startX = event.getRawX();
                startY = event.getRawY();
                dragCanStart = canStartDragFrom(event);
                draggingDown = false;
                animate().cancel();
                setAlpha(1f);
                setTranslationY(0f);
                setDragRounded(false);
            } else if (action == MotionEvent.ACTION_MOVE && dragCanStart && !suppressPlayerDragDismiss) {
                float dx = event.getRawX() - startX;
                float dy = event.getRawY() - startY;
                if (draggingDown || (dy > dp(8) && dy > Math.abs(dx) * 0.85f)) {
                    draggingDown = true;
                    float translation = Math.max(0f, Math.min(getHeight(), dy));
                    setTranslationY(translation);
                    setAlpha(1f - Math.min(0.18f, translation / Math.max(1f, getHeight()) * 0.18f));
                    setDragRounded(true);
                    return true;
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (draggingDown) {
                    boolean shouldDismiss = action == MotionEvent.ACTION_UP && getTranslationY() >= dp(56);
                    dragCanStart = false;
                    draggingDown = false;
                    if (shouldDismiss) {
                        animateDismissTopPlayerSurface();
                    } else {
                        animate()
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(150L)
                                .withEndAction(() -> setDragRounded(false))
                                .start();
                    }
                    return true;
                }
                if (getTranslationY() > 0f) {
                    animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(150L)
                            .withEndAction(() -> setDragRounded(false))
                            .start();
                }
            }
            return super.dispatchTouchEvent(event);
        }

        private void animateDismissTopPlayerSurface() {
            animate()
                    .translationY(Math.max(getHeight(), dp(320)))
                    .alpha(0.82f)
                    .setDuration(190L)
                    .withEndAction(() -> {
                        setTranslationY(0f);
                        setAlpha(1f);
                        setDragRounded(false);
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
                    || (view.isClickable() && view.isEnabled());
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
            }
        }

        private void dismissTopPlayerSurface() {
            if (queueDialog != null && queueDialog.isShowing()) {
                queueDialog.dismiss();
            } else if (playerDialog != null && playerDialog.isShowing()) {
                playerDialog.dismiss();
            }
        }
    }

    private final class SleepTimerDialView extends View {
        private static final int MAX_MINUTES = 120;
        private static final int STEP_MINUTES = 5;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF wheel = new RectF();
        private TimerChangeListener listener;
        private int selectedMinutes = 30;
        private int dragStartMinutes = 30;
        private float dragStepOffset;
        private float dragStartY;
        private boolean timerActive;
        private boolean dragging;

        SleepTimerDialView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void setSelectedMinutes(int minutes) {
            selectedMinutes = clampTimerMinutes(minutes);
            if (!dragging) {
                dragStepOffset = 0f;
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
            int minIndex = Math.max(0, anchor - 2);
            int maxIndex = Math.min(maxTimerIndex(), anchor + 2);
            for (int index = minIndex; index <= maxIndex; index++) {
                float distance = index - centerIndex;
                if (Math.abs(distance) > 1.65f) {
                    continue;
                }
                drawDialValue(canvas, timerText(index * STEP_MINUTES), width / 2f, dialValueY(height, distance),
                        dialTextSize(distance), dialTextColor(distance));
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                suppressPlayerDragDismiss = true;
                dragging = true;
                dragStepOffset = 0f;
                dragStartY = event.getRawY();
                dragStartMinutes = selectedMinutes;
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float startIndex = dragStartMinutes / (float) STEP_MINUTES;
                float rawCenterIndex = startIndex + (dragStartY - event.getRawY()) / dp(20);
                float nextCenterIndex = Math.max(0f, Math.min(maxTimerIndex(), rawCenterIndex));
                dragStepOffset = nextCenterIndex - startIndex;
                int nextMinutes = clampTimerMinutes(Math.round(nextCenterIndex) * STEP_MINUTES);
                if (nextMinutes != selectedMinutes) {
                    selectedMinutes = nextMinutes;
                    if (listener != null) {
                        listener.onChanged(selectedMinutes, false);
                    }
                }
                invalidate();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                dragging = false;
                dragStepOffset = 0f;
                suppressPlayerDragDismiss = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                if (listener != null && event.getAction() == MotionEvent.ACTION_UP) {
                    listener.onChanged(selectedMinutes, true);
                }
                invalidate();
                return true;
            }
            return super.onTouchEvent(event);
        }

        private float visibleCenterIndex() {
            if (dragging) {
                return dragStartMinutes / (float) STEP_MINUTES + dragStepOffset;
            }
            return selectedMinutes / (float) STEP_MINUTES;
        }

        private float dialValueY(int height, float distance) {
            return height * 0.5f + distance * height * 0.33f;
        }

        private int dialTextSize(float distance) {
            float absolute = Math.min(1.4f, Math.abs(distance));
            return Math.round(22f - absolute * 7f);
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
            return minutes <= 0 ? "OFF" : minutes + "m";
        }

        private int maxTimerIndex() {
            return MAX_MINUTES / STEP_MINUTES;
        }

        private int clampTimerMinutes(int minutes) {
            int stepped = Math.round(minutes / (float) STEP_MINUTES) * STEP_MINUTES;
            return Math.max(0, Math.min(MAX_MINUTES, stepped));
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
        private final String title;
        private final String subtitle;
        private final DeviceAudioTrack coverTrack;
        private final List<DeviceAudioTrack> tracks;

        private LibraryGroup(
                String title,
                String subtitle,
                DeviceAudioTrack coverTrack,
                List<DeviceAudioTrack> tracks
        ) {
            this.title = title == null || title.trim().isEmpty() ? "알 수 없음" : title.trim();
            this.subtitle = subtitle == null || subtitle.trim().isEmpty() ? "-" : subtitle.trim();
            this.coverTrack = coverTrack;
            this.tracks = tracks == null ? new ArrayList<>() : new ArrayList<>(tracks);
        }
    }

    private enum LibraryFilter {
        ALL,
        ALBUM,
        ARTIST
    }

    private enum Tab {
        HOME,
        LIBRARY,
        EXTRACTOR
    }
}
