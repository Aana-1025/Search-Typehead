package com.typeahead.dataset;

public record CsvQueryRecord(
    String queryText,
    String normalizedQuery,
    long count
) {
}
