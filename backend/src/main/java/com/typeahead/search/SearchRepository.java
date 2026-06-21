package com.typeahead.search;

import java.util.Optional;

public interface SearchRepository {

    Optional<StoredSearchQuery> findByNormalizedQuery(String normalizedQuery);

    StoredSearchQuery insertNewQuery(String queryText, String normalizedQuery);

    StoredSearchQuery incrementQuery(long id);

    void replacePrefixes(StoredSearchQuery searchQuery, int prefixMaxLength);
}
