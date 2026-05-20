package com.ytet.android.extract;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public final class RuntimeInstaller {
    public RuntimePaths install(Context context) throws ExtractionException {
        File runtimeDir = new File(context.getFilesDir(), "runtime");
        if (!runtimeDir.exists() && !runtimeDir.mkdirs()) {
            throw new ExtractionException("런타임 폴더를 만들 수 없습니다: " + runtimeDir);
        }

        try {
            File ytDlp = installOne(context, runtimeDir, "yt-dlp");
            File ffmpeg = installOne(context, runtimeDir, "ffmpeg");
            return new RuntimePaths(ytDlp, ffmpeg);
        } catch (IOException exception) {
            throw new ExtractionException("Android용 yt-dlp/ffmpeg 런타임을 준비하지 못했습니다.", exception);
        }
    }

    private File installOne(Context context, File runtimeDir, String executableName) throws IOException, ExtractionException {
        String assetPath = findAssetPath(context.getAssets(), executableName);
        if (assetPath == null) {
            throw new ExtractionException(
                    "app/src/main/assets/runtime/<ABI>/" + executableName
                            + " 파일이 필요합니다. Android용 실행 파일을 추가한 뒤 다시 빌드하세요."
            );
        }

        File target = new File(runtimeDir, executableName);
        try (InputStream input = context.getAssets().open(assetPath);
             OutputStream output = Files.newOutputStream(target.toPath())) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        if (!target.setExecutable(true, false)) {
            throw new ExtractionException("실행 권한을 설정하지 못했습니다: " + target.getName());
        }
        return target;
    }

    private String findAssetPath(AssetManager assets, String executableName) throws IOException {
        for (String abi : Build.SUPPORTED_ABIS) {
            String path = "runtime/" + abi + "/" + executableName;
            if (assetExists(assets, path)) {
                return path;
            }
        }

        String commonPath = "runtime/common/" + executableName;
        return assetExists(assets, commonPath) ? commonPath : null;
    }

    private boolean assetExists(AssetManager assets, String path) throws IOException {
        try (InputStream ignored = assets.open(path)) {
            return true;
        } catch (FileNotFoundException exception) {
            return false;
        }
    }

    public static final class RuntimePaths {
        private final File ytDlp;
        private final File ffmpeg;

        RuntimePaths(File ytDlp, File ffmpeg) {
            this.ytDlp = ytDlp;
            this.ffmpeg = ffmpeg;
        }

        public File ytDlp() {
            return ytDlp;
        }

        public File ffmpeg() {
            return ffmpeg;
        }
    }
}

