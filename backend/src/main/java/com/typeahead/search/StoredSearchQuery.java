package com.typeahead.search;

record StoredSearchQuery(
    long id,
    String queryText,
    String normalizedQuery,
    long totalCount,
    double trendScore
) {
}
