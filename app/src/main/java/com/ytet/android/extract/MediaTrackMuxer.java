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

final class MediaTrackMuxer {
    private MediaTrackMuxer() {
    }

    static void mergeWorkspace(File workspace, ExtractionProgressListener progressListener) throws ExtractionException {
        File manifest = new File(workspace, "mux.json");
        if (!manifest.isFile()) {
            return;
        }

        MuxPlan plan = readPlan(workspace, manifest);
        progressListener.onProgress(88, "병합", "고화질 영상과 오디오 트랙 병합 중");
        mux(plan.video, plan.audio, plan.output);

        deleteIfExists(plan.video);
        deleteIfExists(plan.audio);
        deleteIfExists(manifest);
    }

    private static MuxPlan readPlan(File workspace, File manifest) throws ExtractionException {
        try {
            String text = new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(text);
            File video = new File(workspace, json.getString("video"));
            File audio = new File(workspace, json.getString("audio"));
            File output = new File(workspace, json.getString("output"));
            if (!video.isFile()) {
                throw new ExtractionException("병합할 영상 트랙을 찾지 못했습니다: " + video.getName());
            }
            if (!audio.isFile()) {
                throw new ExtractionException("병합할 오디오 트랙을 찾지 못했습니다: " + audio.getName());
            }
            return new MuxPlan(video, audio, output);
        } catch (IOException | JSONException exception) {
            throw new ExtractionException("영상 병합 정보를 읽을 수 없습니다.", exception);
        }
    }

    private static void mux(File video, File audio, File output) throws ExtractionException {
        String[] arguments = new String[]{
                "-y",
                "-hide_banner",
                "-i", video.getAbsolutePath(),
                "-i", audio.getAbsolutePath(),
                "-map", "0:v:0",
                "-map", "1:a:0",
                "-c", "copy",
                "-map_metadata", "-1",
                output.getAbsolutePath()
        };

        FFmpegSession session = FFmpegKit.executeWithArguments(arguments);
        ReturnCode returnCode = session.getReturnCode();
        if (!ReturnCode.isSuccess(returnCode)) {
            deleteIfExists(output);
            throw new ExtractionException("고화질 영상 병합에 실패했습니다.\n" + failureMessage(session));
        }
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
