package com.typeahead.metrics;

public record BatchWriteMetrics(
    long flushes,
    long rawEventsProcessed,
    long uniqueQueryWrites,
    long estimatedDbWritesAvoided
) {
}
