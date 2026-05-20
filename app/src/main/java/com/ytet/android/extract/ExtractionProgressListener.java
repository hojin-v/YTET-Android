package com.ytet.android.extract;

public interface ExtractionProgressListener {
    void onProgress(int percent, String stage, String message);
}

