package com.ytet.android.extract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExtractionResult {
    private final List<StorageWriter.CopiedFile> copiedFiles;
    private final String summary;
    private final boolean partialFailure;

    public ExtractionResult(List<StorageWriter.CopiedFile> copiedFiles, String summary, boolean partialFailure) {
        this.copiedFiles = Collections.unmodifiableList(new ArrayList<>(copiedFiles));
        this.summary = summary;
        this.partialFailure = partialFailure;
    }

    public List<StorageWriter.CopiedFile> copiedFiles() {
        return copiedFiles;
    }

    public String summary() {
        return summary;
    }

    public boolean hasPartialFailure() {
        return partialFailure;
    }
}
