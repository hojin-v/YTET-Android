package com.ytet.android.ui;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
    private static final String PREFS = "ytet_android";
    private static final String PREF_OUTPUT_TREE = "output_tree";

    private final ExecutorService libraryExecutor = Executors.newSingleThreadExecutor();
    private final DeviceMusicLibrary deviceMusicLibrary = new DeviceMusicLibrary();

    private ScrollView contentScrollView;
    private LinearLayout nowPlayingBar;
    private TextView nowPlayingTitle;
    private TextView nowPlayingMeta;
    private Button playPauseButton;
    private Button homeTabButton;
    private Button libraryTabButton;
    private Button extractorTabButton;

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
    private boolean playbackError;
    private String playbackTitle = "로컬 재생 대기";
    private String playbackMeta = "기기 음악을 스캔하면 재생할 수 있습니다.";
    private String streamStatus = "기기 음악을 스캔하면 추천 믹스가 표시됩니다.";
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
            if (!playbackHasQueue) {
                activeStation = null;
            }
            updateNowPlayingBar();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outputTreeUri = getPreferences().getString(PREF_OUTPUT_TREE, null);
        setContentView(buildContent());
        requestNotificationPermissionIfNeeded();
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
    }

    @Override
    protected void onStop() {
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
        libraryExecutor.shutdownNow();
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

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        nowPlayingTitle = text("로컬 재생 대기", 14, R.color.ytet_text, true);
        nowPlayingMeta = text("기기 음악을 스캔하면 재생할 수 있습니다.", 12, R.color.ytet_muted, false);
        copy.addView(nowPlayingTitle, matchWrap());
        copy.addView(nowPlayingMeta, matchWrap());
        bar.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        playPauseButton = compactButton("재생");
        playPauseButton.setOnClickListener(view -> toggleStreamPlayback());
        bar.addView(playPauseButton, new LinearLayout.LayoutParams(dp(82), dp(42)));
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
        tabs.addView(homeTabButton, tabParams());
        tabs.addView(libraryTabButton, tabParams());
        tabs.addView(extractorTabButton, tabParams());
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
        root.addView(title("YTET"), marginBottom(4));
        root.addView(muted("디바이스에 저장된 음악을 기준으로 추천 믹스와 로컬 재생을 시작합니다.", 14), marginBottom(22));

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

    private View buildLibraryTab() {
        LinearLayout root = screenRoot();
        root.addView(title("내 음악"), marginBottom(4));
        root.addView(muted("디바이스의 음악 폴더와 파일을 스캔하고 재생, 공유, 삭제 작업을 실행합니다.", 14), marginBottom(18));

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
        root.addView(title("추출기"), marginBottom(4));
        root.addView(muted("권한이 있는 YouTube 링크를 오디오 또는 영상 파일로 저장합니다.", 14), marginBottom(20));

        root.addView(label("YouTube URL"), marginBottom(8));
        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setText(extractorUrl);
        urlInput.setHint("https://youtu.be/...");
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        styleInput(urlInput);
        root.addView(urlInput, marginBottom(18));

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
        root.addView(optionSpinner, marginBottom(14));

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
        playbackHasQueue = true;
        playbackPlaying = false;
        playbackPreparing = true;
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
        playbackError = false;
        playbackTitle = track.title();
        playbackMeta = track.artist() + " · " + track.folder();
        setStreamingStatus("준비 중: " + track.title());
        updateNowPlayingBar();
        List<DeviceAudioTrack> queue = new ArrayList<>();
        queue.add(track);
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
            nowPlayingTitle.setText("로컬 재생 대기");
            nowPlayingMeta.setText("기기 음악을 스캔하면 재생할 수 있습니다.");
            playPauseButton.setText("재생");
            return;
        }
        nowPlayingTitle.setText(playbackTitle);
        nowPlayingMeta.setText(playbackPreparing || playbackError ? streamStatus : playbackMeta);
        playPauseButton.setText(playbackPlaying ? "일시정지" : "재생");
    }

    private void startLibraryRefresh(boolean renderImmediately) {
        if (!hasAudioPermission()) {
            requestAudioPermission();
            return;
        }
        libraryLoading = true;
        libraryStatus = "기기 음악을 스캔하는 중입니다.";
        if (renderImmediately && currentTab == Tab.LIBRARY) {
            renderCurrentTab();
        }
        libraryExecutor.execute(() -> {
            try {
                List<DeviceAudioTrack> tracks = deviceMusicLibrary.loadTracks(this);
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

    private void renderLibraryDependentTabs() {
        if (currentTab == Tab.HOME || currentTab == Tab.LIBRARY) {
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

    private void updateModeOptions() {
        if (optionSpinner == null || subtitlesCheck == null) {
            return;
        }
        boolean isVideo = selectedMediaType() == MediaType.VIDEO;
        String[] labels = isVideo
                ? VideoQuality.labels()
                : new String[]{AudioFormat.M4A.label(), AudioFormat.ORIGINAL.label()};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        optionSpinner.setAdapter(adapter);
        subtitlesCheck.setVisibility(isVideo ? View.VISIBLE : View.GONE);
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

    private enum Tab {
        HOME,
        LIBRARY,
        EXTRACTOR
    }
}
