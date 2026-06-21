package com.typeahead.trending;

import java.util.List;

public record TrendingResponse(
    String window,
    int count,
    List<TrendingItem> items,
    String source
) {
}
