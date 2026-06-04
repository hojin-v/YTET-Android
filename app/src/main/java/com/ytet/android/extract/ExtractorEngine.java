package com.ytet.android.extract;

import android.content.Context;

import com.ytet.android.core.ExtractionRequest;

public interface ExtractorEngine {
    ExtractionResult extract(
            Context context,
            ExtractionRequest request,
            ExtractionProgressListener progressListener,
            ExtractionCancellationSignal cancellationSignal
    ) throws ExtractionException;
}
