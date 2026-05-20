package com.ytet.android.extract;

import com.ytet.android.core.AudioFormat;
import com.ytet.android.core.ExtractionRequest;
import com.ytet.android.core.MediaType;
import com.ytet.android.core.VideoQuality;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class YtDlpCommandBuilder {
    private YtDlpCommandBuilder() {
    }

    public static List<String> build(RuntimeInstaller.RuntimePaths runtime, File workspace, ExtractionRequest request) {
        List<String> command = new ArrayList<>();
        command.add(runtime.ytDlp().getAbsolutePath());
        command.add("--no-cache-dir");
        command.add("--newline");
        command.add("--no-playlist");
        command.add("--paths");
        command.add(workspace.getAbsolutePath());
        command.add("--output");
        command.add("%(uploader)s - %(title)s.%(ext)s");
        command.add("--ffmpeg-location");
        command.add(runtime.ffmpeg().getAbsolutePath());
        command.add("--print");
        command.add("after_move:filepath");

        if (request.mediaType() == MediaType.VIDEO) {
            appendVideoOptions(command, request);
        } else {
            appendAudioOptions(command, request);
        }

        command.add(request.url());
        return command;
    }

    private static void appendAudioOptions(List<String> command, ExtractionRequest request) {
        AudioFormat format = AudioFormat.fromValue(request.option());
        command.add("--extract-audio");
        command.add("--audio-format");
        if (format == AudioFormat.ORIGINAL) {
            command.add("best");
        } else {
            command.add(format.value());
        }
        command.add("--embed-thumbnail");
        command.add("--embed-metadata");
    }

    private static void appendVideoOptions(List<String> command, ExtractionRequest request) {
        VideoQuality quality = VideoQuality.fromValue(request.option());
        command.add("--format");
        command.add(formatSelector(quality));
        command.add("--merge-output-format");
        command.add(quality == VideoQuality.BEST ? "mkv" : "mp4");

        if (request.includeSubtitles()) {
            command.add("--write-subs");
            command.add("--sub-langs");
            command.add("ko,ko-KR,en,en-US,en-GB");
            command.add("--convert-subs");
            command.add("srt");
            command.add("--embed-subs");
        }

        if (request.includeMultiAudio()) {
            command.add("--audio-multistreams");
        }
    }

    private static String formatSelector(VideoQuality quality) {
        if (quality == VideoQuality.P1080) {
            return "bv*[height<=1080][ext=mp4]+ba[ext=m4a]/b[height<=1080][ext=mp4]/best[height<=1080]";
        }
        if (quality == VideoQuality.P720) {
            return "bv*[height<=720][ext=mp4]+ba[ext=m4a]/b[height<=720][ext=mp4]/best[height<=720]";
        }
        if (quality == VideoQuality.P480) {
            return "bv*[height<=480][ext=mp4]+ba[ext=m4a]/b[height<=480][ext=mp4]/best[height<=480]";
        }
        return "bv*+ba/best";
    }
}

