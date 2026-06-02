package com.ytet.android.ui;

import android.Manifest;
import android.app.Activity;
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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
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
import com.ytet.android.core.ExtractionRequest;
import com.ytet.android.core.MediaType;
import com.ytet.android.core.VideoQuality;
import com.ytet.android.extract.ExtractionService;
import com.ytet.android.library.DeviceAudioTrack;
import com.ytet.android.library.DeviceMusicLibrary;
import com.ytet.android.library.MusicLibrary;
import com.ytet.android.playback.PlaybackService;
import com.ytet.android.stream.MusicStation;
import com.ytet.android.stream.StationCatalog;
import com.ytet.android.update.UpdateChecker;
import com.ytet.android.update.UpdateInfo;

import java.util.ArrayList;
import java.util.List;
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
    private static final String PREF_HIDE_SHORT_AUDIO = "hide_short_audio";
    private static final String PREF_HIDE_SYSTEM_AUDIO_FOLDERS = "hide_system_audio_folders";
    private static final long MINIMUM_MUSIC_DURATION_MS = 45_000L;

    private final ExecutorService libraryExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private final DeviceMusicLibrary deviceMusicLibrary = new DeviceMusicLibrary();
    private final UpdateChecker updateChecker = new UpdateChecker();

    private ScrollView contentScrollView;
    private LinearLayout nowPlayingBar;
    private TextView nowPlayingCover;
    private TextView nowPlayingTitle;
    private TextView nowPlayingMeta;
    private Button playPauseButton;
    private Button homeTabButton;
    private Button libraryTabButton;
    private Button extractorTabButton;
    private Button settingsTabButton;
    private TextView updateStatusText;
    private Button updateActionButton;
    private Dialog playerDialog;

    private EditText urlInput;
    private RadioGroup mediaGroup;
    private RadioButton audioRadio;
    private RadioButton videoRadio;
    private Spinner optionSpinner;
    private CheckBox subtitlesCheck;
    private Button chooseFolderButton;
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
    private String playbackMix = "로컬 음악";
    private long playbackDurationMs;
    private long playbackPositionMs;
    private int playbackQueueIndex = -1;
    private int playbackQueueSize;
    private boolean playbackShuffleEnabled;
    private int playbackRepeatMode = PlaybackService.REPEAT_OFF;
    private List<DeviceAudioTrack> activeQueuePreview = new ArrayList<>();
    private String extractorUrl = "";
    private MediaType extractorMediaType = MediaType.AUDIO;
    private String extractorOption = AudioFormat.M4A.value();
    private boolean extractorIncludeSubtitles;

    private List<DeviceAudioTrack> libraryTracks = new ArrayList<>();
    private boolean libraryLoaded;
    private boolean libraryLoading;
    private String libraryStatus = "기기 음악 권한을 허용하면 폴더와 파일을 스캔합니다.";
    private String selectedFolder = MusicLibrary.ALL_FOLDERS;
    private DeviceAudioTrack selectedTrack;
    private DeviceAudioTrack pendingDeleteTrack;

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
    private boolean hideShortAudio = true;
    private boolean hideSystemAudioFolders = true;

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
            if (!playbackHasQueue) {
                activeStation = null;
                activeQueuePreview = new ArrayList<>();
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
        updateDownloadId = getPreferences().getLong(PREF_UPDATE_DOWNLOAD_ID, NO_DOWNLOAD_ID);
        autoUpdateCheck = getPreferences().getBoolean(PREF_AUTO_UPDATE_CHECK, true);
        hideShortAudio = getPreferences().getBoolean(PREF_HIDE_SHORT_AUDIO, true);
        hideSystemAudioFolders = getPreferences().getBoolean(PREF_HIDE_SYSTEM_AUDIO_FOLDERS, true);
        clearInstalledPendingUpdateIfNeeded();
        setContentView(buildContent());
        requestNotificationPermissionIfNeeded();
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
        LinearLayout app = new LinearLayout(this);
        app.setOrientation(LinearLayout.VERTICAL);
        app.setBackgroundColor(color(R.color.ytet_background));

        contentScrollView = new ScrollView(this);
        contentScrollView.setFillViewport(true);
        contentScrollView.setBackgroundColor(color(R.color.ytet_background));
        app.addView(contentScrollView, new LinearLayout.LayoutParams(
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
        bar.setBackgroundColor(color(R.color.ytet_panel));
        bar.setOnClickListener(view -> showExpandedPlayer());

        nowPlayingCover = text("YT", 13, android.R.color.white, true);
        nowPlayingCover.setGravity(Gravity.CENTER);
        nowPlayingCover.setBackground(rounded(color(R.color.ytet_accent_dark), 8));
        bar.addView(nowPlayingCover, marginRight(10, dp(44), dp(44)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        nowPlayingTitle = text("로컬 재생 대기", 14, R.color.ytet_text, true);
        nowPlayingMeta = text("기기 음악을 스캔하면 재생할 수 있습니다.", 12, R.color.ytet_muted, false);
        copy.addView(nowPlayingTitle, matchWrap());
        copy.addView(nowPlayingMeta, matchWrap());
        bar.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        playPauseButton = compactButton("재생");
        playPauseButton.setOnClickListener(view -> toggleStreamPlayback());
        bar.addView(playPauseButton, new LinearLayout.LayoutParams(dp(86), dp(44)));
        return bar;
    }

    private LinearLayout buildBottomTabs() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(dp(10), dp(8), dp(10), dp(10));
        tabs.setBackgroundColor(color(R.color.ytet_background));

        homeTabButton = tabButton("홈", Tab.HOME);
        libraryTabButton = tabButton("내 음악", Tab.LIBRARY);
        extractorTabButton = tabButton("추출기", Tab.EXTRACTOR);
        settingsTabButton = tabButton("설정", Tab.SETTINGS);
        tabs.addView(homeTabButton, tabParams());
        tabs.addView(libraryTabButton, tabParams());
        tabs.addView(extractorTabButton, tabParams());
        tabs.addView(settingsTabButton, tabParams());
        return tabs;
    }

    private Button tabButton(String label, Tab tab) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setOnClickListener(view -> showTab(tab));
        return button;
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
        } else if (currentTab == Tab.SETTINGS) {
            view = buildSettingsTab();
        } else {
            view = buildHomeTab();
        }
        contentScrollView.addView(view, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        updateTabStyles();
        updateNowPlayingBar();
    }

    private View buildHomeTab() {
        LinearLayout root = screenRoot();
        if (!hasAudioPermission()) {
            LinearLayout permission = panel();
            permission.addView(label("오디오 권한 필요"), marginBottom(8));
            permission.addView(muted("홈 추천 믹스는 디바이스 내 음악 메타데이터와 폴더를 읽은 뒤 생성됩니다.", 14), marginBottom(14));
            Button request = primaryButton("권한 허용");
            request.setOnClickListener(view -> requestAudioPermission());
            permission.addView(request, matchWrap());
            root.addView(permission, marginBottom(18));
            return root;
        }

        if (!libraryLoaded && !libraryLoading) {
            startLibraryRefresh(false);
        }

        LinearLayout hero = panel();
        hero.addView(label("내 음악 바로 듣기"), marginBottom(8));
        hero.addView(text(libraryStatus, 15, R.color.ytet_text, false), marginBottom(12));
        hero.addView(muted(librarySummary(), 13), marginBottom(14));
        Button primaryPlay = primaryButton(activeStation == null ? "전체 셔플 재생" : "현재 믹스 다시 재생");
        primaryPlay.setEnabled(!libraryTracks.isEmpty());
        primaryPlay.setOnClickListener(view -> playStation(activeStation == null ? firstStation() : activeStation));
        hero.addView(primaryPlay, matchWrap());
        root.addView(hero, marginBottom(24));

        root.addView(sectionTitle("추천 믹스"), marginBottom(10));
        List<MusicStation> stations = StationCatalog.recommendedStations(libraryTracks);
        if (stations.isEmpty()) {
            LinearLayout empty = panel();
            empty.addView(label("추천할 음악이 없습니다."), marginBottom(8));
            empty.addView(muted("내 음악 탭에서 스캔 상태를 확인하거나 기기에 음악 파일을 추가하세요.", 13), matchWrap());
            root.addView(empty, marginBottom(18));
            return root;
        }
        HorizontalScrollView shelf = new HorizontalScrollView(this);
        shelf.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (MusicStation station : stations) {
            row.addView(stationCard(station), marginRight(12, dp(184), LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        shelf.addView(row, matchWrap());
        root.addView(shelf, marginBottom(24));
        return root;
    }

    private View stationCard(MusicStation station) {
        LinearLayout card = panel();
        card.setBackground(rounded(station.accentColor(), 8));
        card.setOnClickListener(view -> playStation(station));

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
        play.setOnClickListener(view -> playStation(station));
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
        if (currentTab == Tab.SETTINGS && contentScrollView != null) {
            renderCurrentTab();
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

        if (!hasAudioPermission()) {
            LinearLayout permission = panel();
            permission.addView(label("오디오 권한 필요"), marginBottom(8));
            permission.addView(muted("Android 미디어 저장소에서 음악 파일을 읽어 폴더별로 정리합니다.", 14), marginBottom(14));
            Button request = primaryButton("권한 허용");
            request.setOnClickListener(view -> requestAudioPermission());
            permission.addView(request, matchWrap());
            root.addView(permission, marginBottom(16));
            return root;
        }

        if (!libraryLoaded && !libraryLoading) {
            startLibraryRefresh(false);
        }

        LinearLayout summary = panel();
        summary.addView(label("라이브러리"), marginBottom(8));
        summary.addView(muted(libraryStatus, 13), marginBottom(12));
        summary.addView(text(librarySummary(), 22, R.color.ytet_text, true), marginBottom(12));
        Button refresh = secondaryButton(libraryLoading ? "스캔 중" : "새로고침");
        refresh.setEnabled(!libraryLoading);
        refresh.setOnClickListener(view -> startLibraryRefresh(true));
        summary.addView(refresh, matchWrap());
        root.addView(summary, marginBottom(18));

        List<String> folders = MusicLibrary.folderNames(libraryTracks);
        if (!folders.contains(selectedFolder)) {
            selectedFolder = MusicLibrary.ALL_FOLDERS;
        }
        root.addView(sectionTitle("폴더"), marginBottom(10));
        HorizontalScrollView folderShelf = new HorizontalScrollView(this);
        folderShelf.setHorizontalScrollBarEnabled(false);
        LinearLayout folderRow = new LinearLayout(this);
        folderRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String folder : folders) {
            Button chip = compactButton(folder);
            chip.setTextColor(folder.equals(selectedFolder) ? 0xFFFFFFFF : color(R.color.ytet_text));
            chip.setBackground(rounded(folder.equals(selectedFolder) ? color(R.color.ytet_accent) : color(R.color.ytet_panel_alt), 18));
            chip.setOnClickListener(view -> {
                selectedFolder = folder;
                selectedTrack = null;
                renderCurrentTab();
            });
            folderRow.addView(chip, marginRight(8, LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)));
        }
        folderShelf.addView(folderRow, matchWrap());
        root.addView(folderShelf, marginBottom(18));

        if (selectedTrack != null) {
            root.addView(selectedTrackPanel(), marginBottom(18));
        }

        root.addView(sectionTitle("파일"), marginBottom(10));
        List<DeviceAudioTrack> visibleTracks = MusicLibrary.filterByFolder(libraryTracks, selectedFolder);
        if (visibleTracks.isEmpty()) {
            LinearLayout empty = panel();
            empty.addView(label("표시할 음악이 없습니다."), marginBottom(8));
            empty.addView(muted("다른 폴더를 선택하거나 새로고침을 실행하세요.", 13), matchWrap());
            root.addView(empty, matchWrap());
            return root;
        }

        int limit = Math.min(visibleTracks.size(), 80);
        for (int i = 0; i < limit; i++) {
            root.addView(trackRow(visibleTracks.get(i)), marginBottom(8));
        }
        if (visibleTracks.size() > limit) {
            root.addView(muted("상위 " + limit + "개만 표시 중입니다. 폴더를 선택하면 목록을 좁힐 수 있습니다.", 12), matchWrap());
        }
        return root;
    }

    private View selectedTrackPanel() {
        LinearLayout panel = panel();
        panel.addView(label("선택한 파일"), marginBottom(8));
        panel.addView(text(selectedTrack.title(), 18, R.color.ytet_text, true), marginBottom(4));
        panel.addView(muted(selectedTrack.artist() + " · " + selectedTrack.album(), 13), marginBottom(4));
        panel.addView(muted(selectedTrack.folder() + " · " + MusicLibrary.formatDuration(selectedTrack.durationMs())
                + " · " + MusicLibrary.formatBytes(selectedTrack.sizeBytes()), 13), marginBottom(12));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button play = primaryButton("재생");
        play.setOnClickListener(view -> playTrack(selectedTrack));
        Button open = secondaryButton("열기");
        open.setOnClickListener(view -> openSelectedTrack());
        Button share = secondaryButton("공유");
        share.setOnClickListener(view -> shareSelectedTrack());
        Button delete = dangerButton("삭제");
        delete.setOnClickListener(view -> deleteSelectedTrack());
        actions.addView(play, weightedButtonParams(6));
        actions.addView(open, weightedButtonParams(6));
        actions.addView(share, weightedButtonParams(6));
        actions.addView(delete, new LinearLayout.LayoutParams(0, dp(44), 1f));
        panel.addView(actions, matchWrap());
        return panel;
    }

    private View trackRow(DeviceAudioTrack track) {
        LinearLayout row = panel();
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(rounded(selectedTrack != null && selectedTrack.id() == track.id()
                ? color(R.color.ytet_panel_alt)
                : color(R.color.ytet_panel), 8));
        row.setOnClickListener(view -> {
            selectedTrack = track;
            renderCurrentTab();
        });
        row.addView(text(track.title(), 15, R.color.ytet_text, true), marginBottom(4));
        row.addView(muted(track.artist() + " · " + MusicLibrary.formatDuration(track.durationMs()), 12), marginBottom(2));
        row.addView(muted(track.folder() + " · " + MusicLibrary.formatBytes(track.sizeBytes()), 12), matchWrap());
        return row;
    }

    private View buildExtractorTab() {
        LinearLayout root = screenRoot();

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
        });
        root.addView(mediaGroup, marginBottom(16));

        root.addView(label("포맷 / 품질"), marginBottom(8));
        optionSpinner = new Spinner(this);
        optionSpinner.setBackgroundColor(color(R.color.ytet_panel_alt));
        optionSpinner.setPadding(dp(4), 0, dp(4), 0);
        root.addView(optionSpinner, controlParams(56, 14));

        subtitlesCheck = new CheckBox(this);
        subtitlesCheck.setText("한국어/영어 등록 자막 포함");
        subtitlesCheck.setTextColor(color(R.color.ytet_text));
        subtitlesCheck.setChecked(extractorIncludeSubtitles);
        root.addView(subtitlesCheck, marginBottom(18));

        root.addView(label("저장 폴더"), marginBottom(8));
        chooseFolderButton = secondaryButton("폴더 선택");
        chooseFolderButton.setOnClickListener(view -> chooseOutputFolder());
        root.addView(chooseFolderButton, marginBottom(8));

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

    private View buildSettingsTab() {
        LinearLayout root = screenRoot();

        LinearLayout playback = panel();
        playback.addView(label("재생"), marginBottom(10));
        playback.addView(settingCheckBox(
                "짧은 오디오 숨김",
                "45초 미만 파일은 음악 목록에서 제외합니다.",
                hideShortAudio,
                checked -> {
                    hideShortAudio = checked;
                    saveBooleanPref(PREF_HIDE_SHORT_AUDIO, checked);
                    startLibraryRefresh(false);
                }
        ), marginBottom(8));
        playback.addView(settingCheckBox(
                "녹음/알림 폴더 숨김",
                "녹음, 벨소리, 알림음 폴더는 기본 음악 라이브러리에서 제외합니다.",
                hideSystemAudioFolders,
                checked -> {
                    hideSystemAudioFolders = checked;
                    saveBooleanPref(PREF_HIDE_SYSTEM_AUDIO_FOLDERS, checked);
                    startLibraryRefresh(false);
                }
        ), matchWrap());
        root.addView(playback, marginBottom(18));

        LinearLayout extraction = panel();
        extraction.addView(label("추출 저장 폴더"), marginBottom(10));
        extraction.addView(muted(outputTreeUri == null ? "선택 안 됨" : outputTreeUri, 13), marginBottom(12));
        Button chooseFolder = secondaryButton("저장 폴더 변경");
        chooseFolder.setOnClickListener(view -> chooseOutputFolder());
        extraction.addView(chooseFolder, matchWrap());
        root.addView(extraction, marginBottom(18));

        LinearLayout library = panel();
        library.addView(label("라이브러리"), marginBottom(10));
        library.addView(muted(librarySummary(), 13), marginBottom(12));
        Button refresh = secondaryButton(libraryLoading ? "스캔 중" : "음악 다시 스캔");
        refresh.setEnabled(!libraryLoading);
        refresh.setOnClickListener(view -> startLibraryRefresh(true));
        library.addView(refresh, matchWrap());
        root.addView(library, marginBottom(18));

        LinearLayout updates = panel();
        updates.addView(label("업데이트"), marginBottom(10));
        updates.addView(settingCheckBox(
                "자동 확인",
                "앱 시작 시 GitHub 정식 릴리즈만 확인합니다.",
                autoUpdateCheck,
                checked -> {
                    autoUpdateCheck = checked;
                    saveBooleanPref(PREF_AUTO_UPDATE_CHECK, checked);
                    if (checked && !updateChecking && !updateChecked) {
                        startUpdateCheck(true);
                    }
                }
        ), marginBottom(12));
        updateStatusText = text(updateStatus, 14, R.color.ytet_text, false);
        updates.addView(updateStatusText, marginBottom(12));
        updateActionButton = secondaryButton(updateActionLabel());
        updateActionButton.setEnabled(!updateChecking && !updateDownloading);
        updateActionButton.setOnClickListener(view -> handleUpdateAction());
        updates.addView(updateActionButton, matchWrap());
        root.addView(updates, marginBottom(18));

        return root;
    }

    private View settingCheckBox(String title, String description, boolean checked, SettingChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(title);
        checkBox.setTextColor(color(R.color.ytet_text));
        checkBox.setTextSize(14);
        checkBox.setChecked(checked);
        checkBox.setOnCheckedChangeListener((button, isChecked) -> listener.onChanged(isChecked));
        row.addView(checkBox, matchWrap());
        row.addView(muted(description, 12), matchWrap());
        row.setOnClickListener(view -> checkBox.setChecked(!checkBox.isChecked()));
        return row;
    }

    private void saveBooleanPref(String key, boolean value) {
        getPreferences().edit().putBoolean(key, value).apply();
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
        if (station == null) {
            toast("재생할 음악이 없습니다.");
            return;
        }
        List<DeviceAudioTrack> queue = MusicLibrary.tracksForStation(libraryTracks, station);
        if (queue.isEmpty()) {
            toast("이 믹스에 포함할 음악이 없습니다.");
            return;
        }
        activeStation = station;
        activeQueuePreview = new ArrayList<>(queue);
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
        playbackPlaying = false;
        playbackPreparing = true;
        playbackWillPlay = true;
        playbackError = false;
        playbackTitle = track.title();
        playbackMeta = track.artist() + " · " + track.folder();
        setStreamingStatus("준비 중: " + track.title());
        updateNowPlayingBar();
        List<DeviceAudioTrack> queue = new ArrayList<>();
        queue.add(track);
        activeQueuePreview = new ArrayList<>(queue);
        startPlayback(PlaybackService.playQueueIntent(this, station, queue));
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
        if (playbackHasQueue) {
            startPlayback(PlaybackService.commandIntent(this, PlaybackService.ACTION_PREVIOUS));
        }
    }

    private void nextTrack() {
        if (playbackHasQueue) {
            startPlayback(PlaybackService.commandIntent(this, PlaybackService.ACTION_NEXT));
        }
    }

    private void toggleShuffle() {
        if (playbackHasQueue) {
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
        if (nowPlayingTitle == null || nowPlayingMeta == null || playPauseButton == null) {
            return;
        }
        if (!playbackHasQueue && activeStation == null) {
            if (nowPlayingCover != null) {
                nowPlayingCover.setText("YT");
                nowPlayingCover.setBackground(rounded(color(R.color.ytet_panel_alt), 8));
            }
            nowPlayingTitle.setText("로컬 재생 대기");
            nowPlayingMeta.setText("기기 음악을 스캔하면 재생할 수 있습니다.");
            playPauseButton.setText("재생");
            return;
        }
        if (nowPlayingCover != null) {
            nowPlayingCover.setText(coverInitials());
            nowPlayingCover.setBackground(rounded(color(R.color.ytet_accent_dark), 8));
        }
        nowPlayingTitle.setText(playbackTitle);
        nowPlayingMeta.setText(playbackPreparing || playbackError ? streamStatus : playbackMeta);
        playPauseButton.setText(playbackPlaying || playbackWillPlay ? "일시정지" : "재생");
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
        updateExpandedPlayer();
        playerDialog.show();
        Window window = playerDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    private void updateExpandedPlayer() {
        if (playerDialog == null || (playerDialog.isShowing() && playerDialog.getWindow() == null)) {
            return;
        }
        playerDialog.setContentView(buildExpandedPlayerContent());
    }

    private View buildExpandedPlayerContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(color(R.color.ytet_background));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button close = compactButton("닫기");
        close.setOnClickListener(view -> {
            if (playerDialog != null) {
                playerDialog.dismiss();
            }
        });
        top.addView(close, new LinearLayout.LayoutParams(dp(78), dp(42)));
        TextView mix = text(playbackMix, 13, R.color.ytet_muted, true);
        mix.setGravity(Gravity.CENTER);
        top.addView(mix, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(dp(78), dp(42)));
        root.addView(top, marginBottom(16));

        root.addView(coverArtView(), coverParams());
        root.addView(text(playbackTitle, 23, R.color.ytet_text, true), marginBottom(6));
        root.addView(muted(playbackArtist + " · " + playbackAlbum, 14), marginBottom(4));
        root.addView(muted(playbackFolder + queuePositionText(), 12), marginBottom(16));

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax((int) Math.max(1L, Math.min(Integer.MAX_VALUE, playbackDurationMs)));
        progress.setProgress((int) Math.max(0L, Math.min(progress.getMax(), playbackPositionMs)));
        root.addView(progress, marginBottom(8));
        root.addView(muted(playbackProgressText(), 12), marginBottom(18));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        Button shuffle = playerControlButton("셔플", playbackShuffleEnabled);
        shuffle.setOnClickListener(view -> toggleShuffle());
        Button previous = playerControlButton("이전", false);
        previous.setOnClickListener(view -> previousTrack());
        Button play = primaryButton(playbackPlaying || playbackWillPlay ? "일시정지" : "재생");
        play.setOnClickListener(view -> toggleStreamPlayback());
        Button next = playerControlButton("다음", false);
        next.setOnClickListener(view -> nextTrack());
        Button repeat = playerControlButton(repeatLabel(), playbackRepeatMode != PlaybackService.REPEAT_OFF);
        repeat.setOnClickListener(view -> toggleRepeat());
        controls.addView(shuffle, playerControlParams(5));
        controls.addView(previous, playerControlParams(5));
        controls.addView(play, playerControlParams(5));
        controls.addView(next, playerControlParams(5));
        controls.addView(repeat, playerControlParams(0));
        root.addView(controls, marginBottom(20));

        root.addView(queuePreviewPanel(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
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
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        boolean current = track.id() == playbackTrackId
                || (playbackTrackId < 0L && index == playbackQueueIndex);
        row.setBackground(rounded(current ? color(R.color.ytet_panel_alt) : color(R.color.ytet_panel), 8));
        row.addView(text(track.title(), 14, current ? R.color.ytet_text : R.color.ytet_muted, current), marginBottom(2));
        row.addView(muted(track.artist() + " · " + MusicLibrary.formatDuration(track.durationMs()), 12), matchWrap());
        row.setOnClickListener(view -> playTrack(track));
        return row;
    }

    private String queuePositionText() {
        if (playbackQueueIndex < 0 || playbackQueueSize <= 0) {
            return "";
        }
        return " · " + (playbackQueueIndex + 1) + "/" + playbackQueueSize;
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

    private void startLibraryRefresh(boolean renderImmediately) {
        if (!hasAudioPermission()) {
            requestAudioPermission();
            return;
        }
        libraryLoading = true;
        libraryStatus = "기기 음악을 스캔하는 중입니다.";
        if (renderImmediately && (currentTab == Tab.HOME || currentTab == Tab.LIBRARY || currentTab == Tab.SETTINGS)) {
            renderCurrentTab();
        }
        libraryExecutor.execute(() -> {
            try {
                List<DeviceAudioTrack> tracks = filteredLibraryTracks(deviceMusicLibrary.loadTracks(this));
                runOnUiThread(() -> {
                    libraryTracks = tracks;
                    libraryLoaded = true;
                    libraryLoading = false;
                    libraryStatus = tracks.isEmpty()
                            ? "기기에서 음악 파일을 찾지 못했습니다."
                            : "스캔 완료";
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

    private List<DeviceAudioTrack> filteredLibraryTracks(List<DeviceAudioTrack> tracks) {
        List<DeviceAudioTrack> filtered = new ArrayList<>();
        if (tracks == null) {
            return filtered;
        }
        for (DeviceAudioTrack track : tracks) {
            if (hideShortAudio && track.durationMs() > 0L && track.durationMs() < MINIMUM_MUSIC_DURATION_MS) {
                continue;
            }
            if (hideSystemAudioFolders && isSystemAudioFolder(track.folder())) {
                continue;
            }
            filtered.add(track);
        }
        return filtered;
    }

    private boolean isSystemAudioFolder(String folder) {
        if (folder == null) {
            return false;
        }
        String normalized = folder.trim().toLowerCase();
        return normalized.contains("ringtone")
                || normalized.contains("notification")
                || normalized.contains("alarm")
                || normalized.contains("recording")
                || normalized.contains("voice recorder")
                || normalized.contains("voice notes")
                || normalized.contains("call recording")
                || normalized.contains("녹음")
                || normalized.contains("통화")
                || normalized.contains("알림")
                || normalized.contains("벨소리");
    }

    private void renderLibraryDependentTabs() {
        if (currentTab == Tab.HOME || currentTab == Tab.LIBRARY || currentTab == Tab.SETTINGS) {
            renderCurrentTab();
        }
    }

    private String librarySummary() {
        int folderCount = Math.max(0, MusicLibrary.folderNames(libraryTracks).size() - 1);
        return folderCount + "개 폴더 · " + libraryTracks.size() + "개 파일";
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
        outputTreeUri = uri.toString();
        getPreferences().edit().putString(PREF_OUTPUT_TREE, outputTreeUri).apply();
        updateFolderLabel();
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
        if (optionSpinner == null || subtitlesCheck == null) {
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
        if (outputTreeUri == null || outputTreeUri.trim().isEmpty()) {
            toast("저장 폴더를 선택하세요.");
            return;
        }

        MediaType mediaType = selectedMediaType();
        String option = selectedOption(mediaType);
        ExtractionRequest request;
        try {
            request = new ExtractionRequest(
                    urlInput.getText().toString(),
                    outputTreeUri,
                    mediaType,
                    option,
                    mediaType == MediaType.VIDEO && subtitlesCheck.isChecked()
            );
        } catch (IllegalArgumentException exception) {
            toast(exception.getMessage());
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
        chooseFolderButton.setEnabled(!busy);
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
        folderText.setText(outputTreeUri == null ? "선택 안 됨" : outputTreeUri);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private LinearLayout screenRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        return root;
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(rounded(color(R.color.ytet_panel), 8));
        return panel;
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

    private void updateTabStyles() {
        styleTab(homeTabButton, currentTab == Tab.HOME);
        styleTab(libraryTabButton, currentTab == Tab.LIBRARY);
        styleTab(extractorTabButton, currentTab == Tab.EXTRACTOR);
        styleTab(settingsTabButton, currentTab == Tab.SETTINGS);
    }

    private void styleTab(Button button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setTextColor(selected ? 0xFFFFFFFF : color(R.color.ytet_muted));
        button.setBackground(rounded(selected ? color(R.color.ytet_accent) : color(R.color.ytet_panel), 8));
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
        params.setMargins(0, 0, 0, dp(22));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
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

    private interface SettingChangeListener {
        void onChanged(boolean checked);
    }

    private enum Tab {
        HOME,
        LIBRARY,
        EXTRACTOR,
        SETTINGS
    }
}
