package com.typeahead.trending;

public record TrendingItem(
    String query,
    long totalCount,
    long recentCount,
    double score
) {
}
