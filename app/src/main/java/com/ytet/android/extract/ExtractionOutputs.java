package com.ytet.android.extract;

import com.ytet.android.core.AudioFormat;
import com.ytet.android.core.ExtractionRequest;
import com.ytet.android.core.MediaType;
import com.ytet.android.core.VideoQuality;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class ExtractionOutputs {
    private ExtractionOutputs() {
    }

    static List<File> collectOutputFiles(File workspace) throws ExtractionException {
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

    static String buildSummary(ExtractionRequest request, List<StorageWriter.CopiedFile> copiedFiles) {
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
                || name.endsWith(".tmp");
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
                || name.endsWith(".mkv")
                || name.endsWith(".srt")
                || name.endsWith(".vtt");
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
}
