package com.typeahead.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.typeahead.dataset.DatasetProperties;

@ExtendWith(MockitoExtension.class)
class SearchServiceTests {

    @Mock
    private SearchRepository searchRepository;

    @Test
    void rejectsMissingQueryBody() {
        SearchService searchService = new SearchService(searchRepository, new DatasetProperties(false, "", 1000, 50));

        assertThatThrownBy(() -> searchService.search(null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void rejectsBlankQuery() {
        SearchService searchService = new SearchService(searchRepository, new DatasetProperties(false, "", 1000, 50));

        assertThatThrownBy(() -> searchService.search(new SearchRequest("   ")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400 BAD_REQUEST");

        verify(searchRepository, never()).findByNormalizedQuery(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void normalizesMixedCaseAndUpdatesExistingQuery() {
        SearchService searchService = new SearchService(searchRepository, new DatasetProperties(false, "", 1000, 50));
        StoredSearchQuery existing = new StoredSearchQuery(7L, "iphone", "iphone", 10L, 10.0);
        StoredSearchQuery updated = new StoredSearchQuery(7L, "iphone", "iphone", 11L, 11.0);

        when(searchRepository.findByNormalizedQuery("iphone")).thenReturn(Optional.of(existing));
        when(searchRepository.incrementQuery(7L)).thenReturn(updated);

        SearchResponse response = searchService.search(new SearchRequest("  IPHONE  "));

        assertThat(response.message()).isEqualTo("Searched");
        verify(searchRepository).findByNormalizedQuery("iphone");
        verify(searchRepository).incrementQuery(7L);
        verify(searchRepository).replacePrefixes(updated, 50);
        verifyNoMoreInteractions(searchRepository);
    }

    @Test
    void insertsNewQueryWhenMissing() {
        SearchService searchService = new SearchService(searchRepository, new DatasetProperties(false, "", 1000, 50));
        StoredSearchQuery inserted = new StoredSearchQuery(9L, "new test search query", "new test search query", 1L, 1.0);

        when(searchRepository.findByNormalizedQuery("new test search query")).thenReturn(Optional.empty());
        when(searchRepository.insertNewQuery("new test search query", "new test search query")).thenReturn(inserted);

        SearchResponse response = searchService.search(new SearchRequest("  new test search query  "));

        assertThat(response.message()).isEqualTo("Searched");
        verify(searchRepository).findByNormalizedQuery("new test search query");
        verify(searchRepository).insertNewQuery("new test search query", "new test search query");
        verify(searchRepository).replacePrefixes(inserted, 50);
        verifyNoMoreInteractions(searchRepository);
    }
}
