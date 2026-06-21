package com.typeahead.batch;

import java.time.LocalDateTime;

import com.typeahead.search.StoredSearchQuery;

public interface BatchWriteRepository {

    StoredSearchQuery upsertQuery(String queryText, String normalizedQuery, long countDelta);

    void replacePrefixes(StoredSearchQuery searchQuery, int prefixMaxLength);

    void upsertActivityBucket(long queryId, LocalDateTime bucketStart, String bucketGranularity, long countDelta);
}
