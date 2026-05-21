package com.ytet.android.ui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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

public final class MainActivity extends Activity {
    private static final int REQUEST_OUTPUT_TREE = 1207;
    private static final int REQUEST_NOTIFICATIONS = 1208;
    private static final String PREFS = "ytet_android";
    private static final String PREF_OUTPUT_TREE = "output_tree";

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

    private String outputTreeUri;
    private boolean receiverRegistered;

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

            progressBar.setProgress(percent);
            if (error != null) {
                statusText.setText("오류");
                resultText.setText(error);
            } else {
                statusText.setText(progressStatus(stage, message));
                if (result != null) {
                    resultText.setText(result);
                }
            }

            if (done) {
                setBusy(false);
            }
        }
    };

    private String progressStatus(String stage, String message) {
        String safeStage = stage == null || stage.trim().isEmpty() ? "진행 중" : stage.trim();
        if (message == null || message.trim().isEmpty()) {
            return safeStage;
        }
        return safeStage + " · " + message.trim();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        outputTreeUri = getPreferences().getString(PREF_OUTPUT_TREE, null);
        setContentView(buildContent());
        updateModeOptions();
        updateFolderLabel();
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
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(progressReceiver);
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OUTPUT_TREE || resultCode != RESULT_OK || data == null || data.getData() == null) {
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

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(getColor(R.color.ytet_background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText("YTET Android");
        title.setTextColor(getColor(R.color.ytet_text));
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("YouTube Extractor Toolkit");
        subtitle.setTextColor(getColor(R.color.ytet_muted));
        subtitle.setTextSize(15);
        root.addView(subtitle, marginBottom(20));

        root.addView(label("YouTube URL"), marginBottom(8));
        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://youtu.be/...");
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(urlInput, marginBottom(18));

        root.addView(label("모드"), marginBottom(8));
        mediaGroup = new RadioGroup(this);
        mediaGroup.setOrientation(RadioGroup.HORIZONTAL);
        audioRadio = new RadioButton(this);
        audioRadio.setId(View.generateViewId());
        audioRadio.setText("음원");
        videoRadio = new RadioButton(this);
        videoRadio.setId(View.generateViewId());
        videoRadio.setText("영상");
        mediaGroup.addView(audioRadio, radioParams());
        mediaGroup.addView(videoRadio, radioParams());
        mediaGroup.check(audioRadio.getId());
        mediaGroup.setOnCheckedChangeListener((group, checkedId) -> updateModeOptions());
        root.addView(mediaGroup, marginBottom(16));

        root.addView(label("포맷 / 품질"), marginBottom(8));
        optionSpinner = new Spinner(this);
        root.addView(optionSpinner, marginBottom(14));

        subtitlesCheck = new CheckBox(this);
        subtitlesCheck.setText("한국어/영어 등록 자막 포함");
        root.addView(subtitlesCheck, marginBottom(18));

        root.addView(label("저장 폴더"), marginBottom(8));
        chooseFolderButton = new Button(this);
        chooseFolderButton.setText("폴더 선택");
        chooseFolderButton.setAllCaps(false);
        chooseFolderButton.setOnClickListener(view -> chooseOutputFolder());
        root.addView(chooseFolderButton, marginBottom(8));

        folderText = new TextView(this);
        folderText.setTextColor(getColor(R.color.ytet_muted));
        folderText.setTextSize(13);
        root.addView(folderText, marginBottom(20));

        extractButton = new Button(this);
        extractButton.setText("추출");
        extractButton.setAllCaps(false);
        extractButton.setTextColor(0xFFFFFFFF);
        extractButton.setBackgroundColor(getColor(R.color.ytet_accent));
        extractButton.setOnClickListener(view -> startExtraction());
        root.addView(extractButton, marginBottom(18));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        root.addView(progressBar, marginBottom(12));

        statusText = new TextView(this);
        statusText.setText("대기 중");
        statusText.setTextColor(getColor(R.color.ytet_muted));
        statusText.setTextSize(14);
        root.addView(statusText, marginBottom(12));

        resultText = new TextView(this);
        resultText.setText("-");
        resultText.setTextColor(getColor(R.color.ytet_text));
        resultText.setTextSize(14);
        resultText.setLineSpacing(0, 1.12f);
        root.addView(resultText, matchWrap());

        return scrollView;
    }

    private void updateModeOptions() {
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

        progressBar.setProgress(0);
        statusText.setText("작업을 준비하는 중");
        resultText.setText("-");
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
        return mediaGroup.getCheckedRadioButtonId() == videoRadio.getId() ? MediaType.VIDEO : MediaType.AUDIO;
    }

    private String selectedOption(MediaType mediaType) {
        Object selected = optionSpinner.getSelectedItem();
        String label = selected == null ? "" : selected.toString();
        if (mediaType == MediaType.VIDEO) {
            return VideoQuality.fromLabel(label).value();
        }
        return AudioFormat.fromLabel(label).value();
    }

    private void setBusy(boolean busy) {
        urlInput.setEnabled(!busy);
        mediaGroup.setEnabled(!busy);
        audioRadio.setEnabled(!busy);
        videoRadio.setEnabled(!busy);
        optionSpinner.setEnabled(!busy);
        subtitlesCheck.setEnabled(!busy);
        chooseFolderButton.setEnabled(!busy);
        extractButton.setEnabled(!busy);
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

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(getColor(R.color.ytet_text));
        view.setTextSize(15);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
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

    private RadioGroup.LayoutParams radioParams() {
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                0,
                RadioGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.gravity = Gravity.CENTER_VERTICAL;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
