package com.ytet.android.extract;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class MediaTrackMuxer {
    private MediaTrackMuxer() {
    }

    static void mergeWorkspace(File workspace, ExtractionProgressListener progressListener) throws ExtractionException {
        File manifest = new File(workspace, "mux.json");
        if (!manifest.isFile()) {
            return;
        }

        MuxPlan plan = readPlan(workspace, manifest);
        List<File> subtitles = findSubtitleFiles(workspace);
        progressListener.onProgress(88, "병합", mergeMessage(subtitles));
        mux(plan.video, plan.audio, subtitles, plan.output);

        deleteIfExists(plan.video);
        deleteIfExists(plan.audio);
        for (File subtitle : subtitles) {
            deleteIfExists(subtitle);
        }
        deleteIfExists(manifest);
    }

    private static MuxPlan readPlan(File workspace, File manifest) throws ExtractionException {
        try {
            String text = new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(text);
            File video = new File(workspace, json.getString("video"));
            File audio = json.has("audio") ? new File(workspace, json.getString("audio")) : null;
            File output = new File(workspace, json.getString("output"));
            if (!video.isFile()) {
                throw new ExtractionException("병합할 영상 트랙을 찾지 못했습니다: " + video.getName());
            }
            if (audio != null && !audio.isFile()) {
                throw new ExtractionException("병합할 오디오 트랙을 찾지 못했습니다: " + audio.getName());
            }
            return new MuxPlan(video, audio, output);
        } catch (IOException | JSONException exception) {
            throw new ExtractionException("영상 병합 정보를 읽을 수 없습니다.", exception);
        }
    }

    private static String mergeMessage(List<File> subtitles) {
        if (subtitles.isEmpty()) {
            return "고화질 영상과 오디오 트랙 병합 중";
        }
        return "영상, 오디오, 자막 트랙 병합 중";
    }

    private static void mux(File video, File audio, List<File> subtitles, File output) throws ExtractionException {
        String[] arguments = buildArguments(video, audio, output, subtitles);

        FFmpegSession session = FFmpegKit.executeWithArguments(arguments);
        ReturnCode returnCode = session.getReturnCode();
        if (!ReturnCode.isSuccess(returnCode)) {
            deleteIfExists(output);
            throw new ExtractionException("고화질 영상 병합에 실패했습니다.\n" + failureMessage(session));
        }
    }

    static String[] buildArguments(File video, File audio, File output, List<File> subtitles) {
        List<String> arguments = new ArrayList<>();
        arguments.add("-y");
        arguments.add("-hide_banner");
        arguments.add("-i");
        arguments.add(video.getAbsolutePath());
        if (audio != null) {
            arguments.add("-i");
            arguments.add(audio.getAbsolutePath());
        }
        for (File subtitle : subtitles) {
            arguments.add("-i");
            arguments.add(subtitle.getAbsolutePath());
        }

        arguments.add("-map");
        arguments.add("0:v:0");
        arguments.add("-map");
        arguments.add(audio == null ? "0:a?" : "1:a:0");
        int subtitleInputOffset = audio == null ? 1 : 2;
        for (int index = 0; index < subtitles.size(); index++) {
            arguments.add("-map");
            arguments.add((subtitleInputOffset + index) + ":0");
        }

        arguments.add("-c:v");
        arguments.add("copy");
        arguments.add("-c:a");
        arguments.add("copy");
        if (!subtitles.isEmpty()) {
            arguments.add("-c:s");
            arguments.add(subtitleCodec(output));
        }
        arguments.add("-map_metadata");
        arguments.add("-1");

        for (int index = 0; index < subtitles.size(); index++) {
            File subtitle = subtitles.get(index);
            arguments.add("-metadata:s:s:" + index);
            arguments.add("language=" + subtitleLanguage(subtitle));
            arguments.add("-metadata:s:s:" + index);
            arguments.add("title=" + subtitleTitle(subtitle));
        }
        if (!subtitles.isEmpty()) {
            arguments.add("-disposition:s:0");
            arguments.add("default");
        }

        arguments.add(output.getAbsolutePath());
        return arguments.toArray(new String[0]);
    }

    private static String subtitleCodec(File output) {
        String name = output.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp4") || name.endsWith(".m4v") || name.endsWith(".mov")) {
            return "mov_text";
        }
        return "srt";
    }

    private static String failureMessage(FFmpegSession session) {
        String stackTrace = session.getFailStackTrace();
        if (stackTrace != null && !stackTrace.trim().isEmpty()) {
            return stackTrace.trim();
        }
        String logs = session.getAllLogsAsString();
        if (logs == null || logs.trim().isEmpty()) {
            return "FFmpeg 로그가 비어 있습니다.";
        }
        String[] lines = logs.trim().split("\\R");
        StringBuilder builder = new StringBuilder();
        int start = Math.max(0, lines.length - 6);
        for (int index = start; index < lines.length; index++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(lines[index]);
        }
        return builder.toString();
    }

    private static void deleteIfExists(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private static List<File> findSubtitleFiles(File workspace) {
        List<File> files = new ArrayList<>();
        collectSubtitleFiles(workspace, files);
        files.sort(Comparator.comparingInt(MediaTrackMuxer::subtitleRank).thenComparing(File::getName));
        return files;
    }

    private static void collectSubtitleFiles(File file, List<File> out) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                collectSubtitleFiles(child, out);
            }
            return;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".srt") || name.endsWith(".vtt")) {
            out.add(file);
        }
    }

    private static int subtitleRank(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.contains(".ko.") || name.contains(".ko-kr.")) {
            return 0;
        }
        if (name.contains(".en.") || name.contains(".en-")) {
            return 1;
        }
        return 2;
    }

    private static String subtitleLanguage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.contains(".ko.") || name.contains(".ko-kr.")) {
            return "kor";
        }
        if (name.contains(".en.") || name.contains(".en-")) {
            return "eng";
        }
        return "und";
    }

    private static String subtitleTitle(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.contains(".ko.") || name.contains(".ko-kr.")) {
            return "Korean";
        }
        if (name.contains(".en.") || name.contains(".en-")) {
            return "English";
        }
        return "Subtitle";
    }

    private static final class MuxPlan {
        private final File video;
        private final File audio;
        private final File output;

        private MuxPlan(File video, File audio, File output) {
            this.video = video;
            this.audio = audio;
            this.output = output;
        }
    }
}
