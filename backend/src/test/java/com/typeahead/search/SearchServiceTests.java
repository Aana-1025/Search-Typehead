package com.typeahead.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.typeahead.batch.BatchWriteService;
import com.typeahead.batch.SearchEvent;
import com.typeahead.batch.SearchEventQueue;
import com.typeahead.metrics.MetricsCounters;

@ExtendWith(MockitoExtension.class)
class SearchServiceTests {

    @Mock
    private SearchEventQueue searchEventQueue;

    @Mock
    private BatchWriteService batchWriteService;

    @Mock
    private MetricsCounters metricsCounters;

    @Test
    void rejectsMissingQueryBody() {
        SearchService searchService = new SearchService(
            searchEventQueue,
            batchWriteService,
            metricsCounters
        );

        assertThatThrownBy(() -> searchService.search(null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void rejectsBlankQuery() {
        SearchService searchService = new SearchService(
            searchEventQueue,
            batchWriteService,
            metricsCounters
        );

        assertThatThrownBy(() -> searchService.search(new SearchRequest("   ")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST");

        verify(searchEventQueue, never()).enqueue(any());
    }

    @Test
    void enqueuesNormalizedSearchEventAndReturnsSearched() {
        SearchService searchService = new SearchService(
            searchEventQueue,
            batchWriteService,
            metricsCounters
        );
        when(searchEventQueue.enqueue(any(SearchEvent.class))).thenReturn(1);

        SearchResponse response = searchService.search(new SearchRequest("  IPHONE  "));

        assertThat(response.message()).isEqualTo("Searched");
        verify(searchEventQueue).enqueue(argThat(searchEvent ->
            searchEvent.queryText().equals("IPHONE") &&
                searchEvent.normalizedQuery().equals("iphone") &&
                searchEvent.createdAt() != null
        ));
        verify(metricsCounters).incrementSearchesAccepted();
        verify(metricsCounters).incrementSearchesQueued();
        verify(batchWriteService).triggerAsyncFlushIfNeeded(1);
        verifyNoMoreInteractions(batchWriteService);
    }
}
