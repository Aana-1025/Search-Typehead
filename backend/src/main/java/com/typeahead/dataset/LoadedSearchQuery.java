package com.typeahead.dataset;

public record LoadedSearchQuery(
    long id,
    String queryText,
    String normalizedQuery,
    long totalCount,
    double trendScore
) {
}
