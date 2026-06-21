package com.typeahead.metrics;

public record MetricsSummaryResponse(
    SuggestionMetrics suggestions,
    SearchMetrics searches,
    BatchWriteMetrics batchWrites,
    TrendingMetrics trending,
    DatabaseMetrics database,
    String source
) {
}
