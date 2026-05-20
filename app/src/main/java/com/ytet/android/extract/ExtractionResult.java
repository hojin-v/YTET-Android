package com.ytet.android.extract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExtractionResult {
    private final List<StorageWriter.CopiedFile> copiedFiles;
    private final String summary;

    public ExtractionResult(List<StorageWriter.CopiedFile> copiedFiles, String summary) {
        this.copiedFiles = Collections.unmodifiableList(new ArrayList<>(copiedFiles));
        this.summary = summary;
    }

    public List<StorageWriter.CopiedFile> copiedFiles() {
        return copiedFiles;
    }

    public String summary() {
        return summary;
    }
}

