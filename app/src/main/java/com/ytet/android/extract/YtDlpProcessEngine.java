package com.ytet.android.extract;

import android.content.Context;
import android.net.Uri;

import com.ytet.android.core.AudioFormat;
import com.ytet.android.core.ExtractionRequest;
import com.ytet.android.core.MediaType;
import com.ytet.android.core.VideoQuality;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YtDlpProcessEngine implements ExtractorEngine {
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d{1,3}(?:\\.\\d+)?)%");
    private static final int MAX_LOG_CHARS = 8000;

    private final RuntimeInstaller runtimeInstaller;
    private final StorageWriter storageWriter;

    public YtDlpProcessEngine() {
        this(new RuntimeInstaller(), new StorageWriter());
    }

    YtDlpProcessEngine(RuntimeInstaller runtimeInstaller, StorageWriter storageWriter) {
        this.runtimeInstaller = runtimeInstaller;
        this.storageWriter = storageWriter;
    }

    @Override
    public ExtractionResult extract(
            Context context,
            ExtractionRequest request,
            ExtractionProgressListener progressListener
    ) throws ExtractionException {
        File workspace = new File(context.getCacheDir(), "ytet-" + System.currentTimeMillis());
        if (!workspace.mkdirs()) {
            throw new ExtractionException("작업 폴더를 만들 수 없습니다: " + workspace);
        }

        try {
            progressListener.onProgress(2, "준비", "Android 런타임 확인 중");
            RuntimeInstaller.RuntimePaths runtime = runtimeInstaller.install(context);

            List<String> command = YtDlpCommandBuilder.build(runtime, workspace, request);
            progressListener.onProgress(5, "추출", "yt-dlp 실행 중");
            runProcess(command, workspace, progressListener);

            progressListener.onProgress(92, "저장", "결과 파일 정리 중");
            List<File> outputFiles = collectOutputFiles(workspace);

            progressListener.onProgress(95, "저장", "선택한 Android 폴더로 복사 중");
            List<StorageWriter.CopiedFile> copiedFiles = storageWriter.copyToTree(
                    context,
                    Uri.parse(request.outputTreeUri()),
                    outputFiles
            );
            progressListener.onProgress(100, "완료", "저장 완료");

            return new ExtractionResult(copiedFiles, buildSummary(request, copiedFiles));
        } finally {
            deleteRecursively(workspace);
        }
    }

    private void runProcess(
            List<String> command,
            File workspace,
            ExtractionProgressListener progressListener
    ) throws ExtractionException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workspace);
        builder.redirectErrorStream(true);

        StringBuilder logTail = new StringBuilder();
        Process process;
        try {
            process = builder.start();
        } catch (IOException exception) {
            throw new ExtractionException("yt-dlp 프로세스를 시작하지 못했습니다.", exception);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendLogTail(logTail, line);
                int percent = parsePercent(line);
                if (percent >= 0) {
                    progressListener.onProgress(Math.min(90, Math.max(6, percent)), "다운로드", line);
                } else if (line.contains("[Merger]") || line.contains("[ExtractAudio]")) {
                    progressListener.onProgress(88, "후처리", line);
                } else if (line.contains("Destination") || line.contains("Deleting original file")) {
                    progressListener.onProgress(20, "다운로드", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ExtractionException("yt-dlp가 실패했습니다. 종료 코드: " + exitCode + "\n" + logTail);
            }
        } catch (IOException exception) {
            process.destroyForcibly();
            throw new ExtractionException("yt-dlp 로그를 읽는 중 오류가 발생했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ExtractionException("추출 작업이 중단되었습니다.", exception);
        }
    }

    private void appendLogTail(StringBuilder logTail, String line) {
        logTail.append(line).append('\n');
        if (logTail.length() > MAX_LOG_CHARS) {
            logTail.delete(0, logTail.length() - MAX_LOG_CHARS);
        }
    }

    private int parsePercent(String line) {
        Matcher matcher = PERCENT_PATTERN.matcher(line);
        if (!matcher.find()) {
            return -1;
        }
        try {
            float value = Float.parseFloat(matcher.group(1));
            return Math.round(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private List<File> collectOutputFiles(File workspace) throws ExtractionException {
        List<File> allFiles = new ArrayList<>();
        collectFiles(workspace, allFiles);

        List<File> filtered = new ArrayList<>();
        for (File file : allFiles) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (isTransientOutput(name)) {
                continue;
            }
            if (isExpectedOutput(name)) {
                filtered.add(file);
            }
        }

        if (filtered.isEmpty()) {
            if (allFiles.isEmpty()) {
                throw new ExtractionException("추출 결과 파일을 찾지 못했습니다.");
            }
            throw new ExtractionException(
                    "예상한 형식의 추출 결과 파일을 찾지 못했습니다. 임시 파일이나 부가 파일은 결과로 저장하지 않았습니다.\n"
                            + "발견된 파일: " + describeFiles(allFiles)
            );
        }

        filtered.sort(Comparator.comparing(File::getName));
        return filtered;
    }

    private void collectFiles(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectFiles(file, out);
            } else if (file.isFile()) {
                out.add(file);
            }
        }
    }

    private boolean isTransientOutput(String name) {
        return name.endsWith(".part")
                || name.endsWith(".ytdl")
                || name.endsWith(".tmp");
    }

    private boolean isExpectedOutput(String name) {
        return name.endsWith(".m4a")
                || name.endsWith(".aac")
                || name.endsWith(".flac")
                || name.endsWith(".mp3")
                || name.endsWith(".opus")
                || name.endsWith(".ogg")
                || name.endsWith(".wav")
                || name.endsWith(".webm")
                || name.endsWith(".mp4")
                || name.endsWith(".mkv")
                || name.endsWith(".srt")
                || name.endsWith(".vtt");
    }

    private String describeFiles(List<File> files) {
        List<File> sorted = new ArrayList<>(files);
        sorted.sort(Comparator.comparing(File::getName));

        StringBuilder builder = new StringBuilder();
        int limit = Math.min(sorted.size(), 8);
        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(sorted.get(index).getName());
        }
        if (sorted.size() > limit) {
            builder.append(" 외 ").append(sorted.size() - limit).append("개");
        }
        return builder.toString();
    }

    private String buildSummary(ExtractionRequest request, List<StorageWriter.CopiedFile> copiedFiles) {
        StringBuilder builder = new StringBuilder();
        builder.append("저장 완료\n");
        builder.append("검증: Android 저장소에 복사된 파일 확인\n");
        builder.append("요청: ").append(requestLabel(request)).append('\n');
        if (request.mediaType() == MediaType.VIDEO) {
            builder.append("자막 요청: ").append(yesNo(request.includeSubtitles())).append('\n');
            builder.append("다중 오디오 요청: ").append(yesNo(request.includeMultiAudio())).append('\n');
        }
        for (StorageWriter.CopiedFile file : copiedFiles) {
            builder.append("파일: ")
                    .append(file.name())
                    .append(" (")
                    .append(formatBytes(file.bytes()))
                    .append(")\n");
            builder.append("위치: ").append(file.uri()).append('\n');
        }
        return builder.toString().trim();
    }

    private String requestLabel(ExtractionRequest request) {
        if (request.mediaType() == MediaType.VIDEO) {
            return "영상 / " + VideoQuality.fromValue(request.option()).label();
        }
        return "음원 / " + AudioFormat.fromValue(request.option()).label();
    }

    private String yesNo(boolean value) {
        return value ? "예" : "아니오";
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "알 수 없음";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes / 1024.0;
        String[] units = {"KB", "MB", "GB"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex]);
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
