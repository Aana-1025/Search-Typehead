package com.typeahead.search;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.typeahead.batch.BatchWriteService;
import com.typeahead.batch.SearchEvent;
import com.typeahead.batch.SearchEventQueue;
import com.typeahead.dataset.DatasetNormalizer;

@Service
public class SearchService {

    private static final SearchResponse SEARCHED_RESPONSE = new SearchResponse("Searched");

    private final SearchEventQueue searchEventQueue;
    private final BatchWriteService batchWriteService;

    public SearchService(
        SearchEventQueue searchEventQueue,
        BatchWriteService batchWriteService
    ) {
        this.searchEventQueue = searchEventQueue;
        this.batchWriteService = batchWriteService;
    }

    public SearchResponse search(SearchRequest request) {
        if (request == null) {
            throw invalidQuery();
        }

        String normalizedQuery = DatasetNormalizer.normalizeQuery(request.query());
        if (normalizedQuery.isEmpty()) {
            throw invalidQuery();
        }

        String queryText = request.query() == null ? normalizedQuery : request.query().trim();
        if (queryText.isEmpty()) {
            queryText = normalizedQuery;
        }
        int queueSize = searchEventQueue.enqueue(new SearchEvent(queryText, normalizedQuery, Instant.now()));
        batchWriteService.triggerAsyncFlushIfNeeded(queueSize);

        return SEARCHED_RESPONSE;
    }

    private ResponseStatusException invalidQuery() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be empty");
    }
}
