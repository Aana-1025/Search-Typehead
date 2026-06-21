package com.typeahead.metrics;

public record SuggestionMetrics(
    long requests,
    long cacheHits,
    long cacheMisses,
    double cacheHitRate,
    long postgresReads
) {
}
