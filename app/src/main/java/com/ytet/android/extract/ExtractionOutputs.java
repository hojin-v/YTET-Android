package com.ytet.android.extract;

import com.ytet.android.core.AudioFormat;
import com.ytet.android.core.ExtractionRequest;
import com.ytet.android.core.MediaType;
import com.ytet.android.core.VideoQuality;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class ExtractionOutputs {
    private static final String PLAYLIST_REPORT_NAME = "ytet-extraction-report.json";
    private static final int REPORT_ITEM_LIMIT = 12;

    private ExtractionOutputs() {
    }

    static PlaylistReport readPlaylistReport(File workspace) {
        File reportFile = new File(workspace, PLAYLIST_REPORT_NAME);
        if (!reportFile.isFile()) {
            return PlaylistReport.empty();
        }
        try {
            String text = new String(Files.readAllBytes(reportFile.toPath()), StandardCharsets.UTF_8);
            return PlaylistReport.fromJson(new JSONObject(text));
        } catch (Exception exception) {
            return PlaylistReport.empty();
        }
    }

    static List<File> collectOutputFiles(File workspace) throws ExtractionException {
        List<File> allFiles = new ArrayList<>();
        collectFiles(workspace, allFiles);

        List<File> filtered = new ArrayList<>();
        List<File> nonTransient = new ArrayList<>();
        for (File file : allFiles) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (isTransientOutput(name)) {
                continue;
            }
            nonTransient.add(file);
            if (isExpectedOutput(name)) {
                filtered.add(file);
            }
        }

        if (filtered.isEmpty()) {
            if (nonTransient.isEmpty()) {
                throw new ExtractionException("추출 결과 파일을 찾지 못했습니다.");
            }
            throw new ExtractionException(
                    "예상한 형식의 추출 결과 파일을 찾지 못했습니다. 임시 파일이나 부가 파일은 결과로 저장하지 않았습니다.\n"
                            + "발견된 파일: " + describeFiles(nonTransient)
            );
        }

        filtered.sort(Comparator.comparing(File::getName));
        return filtered;
    }

    static String buildSummary(ExtractionRequest request, List<StorageWriter.CopiedFile> copiedFiles, PlaylistReport report) {
        StringBuilder builder = new StringBuilder();
        if (report.hasFailures()) {
            builder.append("부분 완료\n");
            builder.append("성공: ").append(copiedFiles.size()).append("개");
            builder.append(" · 실패/건너뜀: ").append(report.failureCount()).append("개");
            if (report.totalCount() > 0) {
                builder.append(" · 전체: ").append(report.totalCount()).append("개");
            }
            builder.append('\n');
            appendReportItems(builder, "실패/건너뜀 항목", report.failedLabels(), true);
            appendReportItems(builder, "성공 항목", report.succeededLabels(), false);
        } else {
            builder.append("저장 완료\n");
        }
        builder.append("검증: Android 저장소에 복사된 파일 확인\n");
        builder.append("요청: ").append(requestLabel(request)).append('\n');
        if (request.includePlaylist()) {
            builder.append("플레이리스트: 전체 항목 순서대로 추출\n");
        }
        if (request.enhanceMetadata()) {
            builder.append("메타데이터 보정: MusicBrainz 검색 사용\n");
        }
        if (request.mediaType() == MediaType.VIDEO) {
            builder.append("자막 요청: ").append(yesNo(request.includeSubtitles())).append('\n');
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

    private static void appendReportItems(StringBuilder builder, String title, List<String> labels, boolean required) {
        if (labels.isEmpty()) {
            if (required) {
                builder.append(title).append(": 알 수 없음\n");
            }
            return;
        }
        builder.append(title).append(":\n");
        int limit = Math.min(labels.size(), REPORT_ITEM_LIMIT);
        for (int index = 0; index < limit; index++) {
            builder.append("- ").append(labels.get(index)).append('\n');
        }
        if (labels.size() > limit) {
            builder.append("- 외 ").append(labels.size() - limit).append("개\n");
        }
    }

    private static void collectFiles(File dir, List<File> out) {
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

    private static boolean isTransientOutput(String name) {
        return name.endsWith(".part")
                || name.endsWith(".ytdl")
                || name.endsWith(".tmp")
                || PLAYLIST_REPORT_NAME.equals(name)
                || "mux.json".equals(name)
                || name.startsWith("cover.")
                || name.startsWith("video-track.")
                || name.startsWith("audio-track.")
                || name.endsWith(".srt")
                || name.endsWith(".vtt");
    }

    private static boolean isExpectedOutput(String name) {
        return name.endsWith(".m4a")
                || name.endsWith(".aac")
                || name.endsWith(".flac")
                || name.endsWith(".mp3")
                || name.endsWith(".opus")
                || name.endsWith(".ogg")
                || name.endsWith(".wav")
                || name.endsWith(".webm")
                || name.endsWith(".mp4")
                || name.endsWith(".mkv");
    }

    private static String describeFiles(List<File> files) {
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

    private static String requestLabel(ExtractionRequest request) {
        if (request.mediaType() == MediaType.VIDEO) {
            return "영상 / " + VideoQuality.fromValue(request.option()).label();
        }
        return "음원 / " + AudioFormat.fromValue(request.option()).label();
    }

    private static String yesNo(boolean value) {
        return value ? "예" : "아니오";
    }

    private static String formatBytes(long bytes) {
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

    static final class PlaylistReport {
        private final int totalCount;
        private final List<String> succeededLabels;
        private final List<String> failedLabels;

        PlaylistReport(int totalCount, List<String> succeededLabels, List<String> failedLabels) {
            this.totalCount = totalCount;
            this.succeededLabels = Collections.unmodifiableList(new ArrayList<>(succeededLabels));
            this.failedLabels = Collections.unmodifiableList(new ArrayList<>(failedLabels));
        }

        static PlaylistReport empty() {
            return new PlaylistReport(0, Collections.emptyList(), Collections.emptyList());
        }

        static PlaylistReport fromJson(JSONObject object) {
            return new PlaylistReport(
                    object.optInt("total", 0),
                    labelsFromArray(object.optJSONArray("succeeded"), false),
                    labelsFromArray(object.optJSONArray("failed"), true)
            );
        }

        boolean hasFailures() {
            return !failedLabels.isEmpty();
        }

        int totalCount() {
            return totalCount;
        }

        int failureCount() {
            return failedLabels.size();
        }

        List<String> succeededLabels() {
            return succeededLabels;
        }

        List<String> failedLabels() {
            return failedLabels;
        }

        String buildFailureSummary() {
            StringBuilder builder = new StringBuilder();
            builder.append("플레이리스트 추출 실패\n");
            builder.append("저장 가능한 결과 파일을 찾지 못했습니다.");
            if (totalCount > 0) {
                builder.append(" 전체 ").append(totalCount).append("개 중");
            }
            builder.append(" 실패/건너뜀 ").append(failedLabels.size()).append("개\n");
            appendReportItems(builder, "실패/건너뜀 항목", failedLabels, true);
            return builder.toString().trim();
        }

        private static List<String> labelsFromArray(JSONArray array, boolean includeReason) {
            if (array == null) {
                return Collections.emptyList();
            }
            List<String> labels = new ArrayList<>();
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item == null) {
                    continue;
                }
                String label = itemLabel(item, includeReason);
                if (!label.isEmpty()) {
                    labels.add(label);
                }
            }
            return labels;
        }

        private static String itemLabel(JSONObject item, boolean includeReason) {
            int index = item.optInt("index", 0);
            String label = item.optString("label", "").trim();
            if (label.isEmpty()) {
                label = index > 0 ? index + "번째 항목" : "알 수 없는 항목";
            }
            StringBuilder builder = new StringBuilder();
            if (index > 0) {
                builder.append(index).append(". ");
            }
            builder.append(label);
            String reason = item.optString("reason", "").trim();
            if (includeReason && !reason.isEmpty()) {
                builder.append(" (").append(reason).append(')');
            }
            return builder.toString();
        }
    }
}
