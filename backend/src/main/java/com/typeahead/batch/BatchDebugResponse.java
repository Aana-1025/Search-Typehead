package com.typeahead.batch;

public record BatchDebugResponse(
    boolean enabled,
    int queueSize,
    long flushIntervalMs,
    int maxEvents
) {
}
