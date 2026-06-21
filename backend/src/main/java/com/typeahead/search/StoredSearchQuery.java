package com.typeahead.search;

public record StoredSearchQuery(
    long id,
    String queryText,
    String normalizedQuery,
    long totalCount,
    double trendScore
) {
}
