package com.typeahead.dataset;

public record QueryPrefixRecord(
    String prefix,
    long queryId,
    String queryText,
    String normalizedQuery,
    long totalCount,
    double trendScore
) {
}
