package com.typeahead.batch;

import java.time.Instant;
import java.util.UUID;

public record BatchFlushResult(
    UUID batchId,
    long rawEventCount,
    long uniqueQueryCount,
    long dbWriteCount,
    Instant startedAt,
    Instant finishedAt,
    BatchFlushStatus status
) {
}
