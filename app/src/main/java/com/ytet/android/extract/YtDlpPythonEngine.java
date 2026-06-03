package com.ytet.android.extract;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.ytet.android.core.ExtractionRequest;

import java.io.File;
import java.util.List;

public final class YtDlpPythonEngine implements ExtractorEngine {
    private final StorageWriter storageWriter;

    public YtDlpPythonEngine() {
        this(new StorageWriter());
    }

    YtDlpPythonEngine(StorageWriter storageWriter) {
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
            progressListener.onProgress(3, "준비", "Python yt-dlp 초기화 중");
            ensurePython(context);

            progressListener.onProgress(5, "추출", "yt-dlp 실행 중");
            PyObject module = Python.getInstance().getModule("ytet_ydl");
            module.callAttr(
                    "extract",
                    workspace.getAbsolutePath(),
                    request.url(),
                    request.mediaType().value(),
                    request.option(),
                    request.includeSubtitles(),
                    request.includePlaylist(),
                    request.enhanceMetadata(),
                    progressListener,
                    Build.VERSION.SDK_INT
            );

            MediaTrackMuxer.mergeWorkspace(workspace, progressListener);

            progressListener.onProgress(92, "저장", "결과 파일 정리 중");
            List<File> outputFiles = ExtractionOutputs.collectOutputFiles(workspace);

            progressListener.onProgress(95, "저장", request.usesDefaultOutput()
                    ? "기본 YTET 폴더로 복사 중"
                    : "선택한 Android 폴더로 복사 중");
            List<StorageWriter.CopiedFile> copiedFiles = request.usesDefaultOutput()
                    ? storageWriter.copyToDefaultPublicFolder(context, request.mediaType(), workspace, outputFiles)
                    : storageWriter.copyToTree(context, Uri.parse(request.outputTreeUri()), workspace, outputFiles);
            progressListener.onProgress(100, "완료", "저장 완료");

            return new ExtractionResult(copiedFiles, ExtractionOutputs.buildSummary(request, copiedFiles));
        } catch (PyException exception) {
            throw new ExtractionException(cleanPythonError(exception), exception);
        } finally {
            deleteRecursively(workspace);
        }
    }

    private void ensurePython(Context context) {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
        }
    }

    private String cleanPythonError(PyException exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "yt-dlp 실행 중 오류가 발생했습니다.";
        }

        String[] lines = message.trim().split("\\R");
        String lastLine = lines[lines.length - 1].trim();
        int separator = lastLine.indexOf(": ");
        if (separator >= 0 && separator + 2 < lastLine.length()) {
            return lastLine.substring(separator + 2);
        }
        return lastLine;
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
