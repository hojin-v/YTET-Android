package com.ytet.android.extract;

public final class ExtractionCanceledException extends ExtractionException {
    public ExtractionCanceledException(String message) {
        super(message);
    }

    public ExtractionCanceledException(String message, Throwable cause) {
        super(message, cause);
    }
}
