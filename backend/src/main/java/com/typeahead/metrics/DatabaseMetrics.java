package com.typeahead.metrics;

public record DatabaseMetrics(
    long searchQueries,
    long queryPrefixes,
    long activityBuckets,
    long batchAuditRows
) {
}
