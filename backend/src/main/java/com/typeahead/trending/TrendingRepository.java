package com.typeahead.trending;

import java.time.Instant;
import java.util.List;

public interface TrendingRepository {

    List<TrendingItem> findTrendingSince(Instant cutoffInclusive);
}
