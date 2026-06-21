package com.typeahead.search;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.typeahead.dataset.DatasetNormalizer;
import com.typeahead.dataset.DatasetProperties;

@Service
public class SearchService {

    private static final SearchResponse SEARCHED_RESPONSE = new SearchResponse("Searched");

    private final SearchRepository searchRepository;
    private final DatasetProperties datasetProperties;

    public SearchService(SearchRepository searchRepository, DatasetProperties datasetProperties) {
        this.searchRepository = searchRepository;
        this.datasetProperties = datasetProperties;
    }

    @Transactional
    public SearchResponse search(SearchRequest request) {
        if (request == null) {
            throw invalidQuery();
        }

        String normalizedQuery = DatasetNormalizer.normalizeQuery(request.query());
        if (normalizedQuery.isEmpty()) {
            throw invalidQuery();
        }

        StoredSearchQuery updatedQuery = upsertQuery(request.query(), normalizedQuery);
        searchRepository.replacePrefixes(updatedQuery, datasetProperties.prefixMaxLength());

        return SEARCHED_RESPONSE;
    }

    private StoredSearchQuery upsertQuery(String rawQuery, String normalizedQuery) {
        Optional<StoredSearchQuery> existing = searchRepository.findByNormalizedQuery(normalizedQuery);
        if (existing.isPresent()) {
            return searchRepository.incrementQuery(existing.get().id());
        }

        String queryText = rawQuery == null ? normalizedQuery : rawQuery.trim();
        if (queryText.isEmpty()) {
            queryText = normalizedQuery;
        }
        return searchRepository.insertNewQuery(queryText, normalizedQuery);
    }

    private ResponseStatusException invalidQuery() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be empty");
    }
}
