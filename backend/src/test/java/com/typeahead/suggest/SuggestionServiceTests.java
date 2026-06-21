package com.typeahead.suggest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.typeahead.cache.SuggestionCacheService;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTests {

    @Mock
    private SuggestionRepository suggestionRepository;

    @Mock
    private SuggestionCacheService suggestionCacheService;

    @Test
    void returnsEmptyResponseForMissingPrefix() {
        SuggestionService suggestionService = new SuggestionService(suggestionRepository, suggestionCacheService);

        SuggestionResponse response = suggestionService.suggest(null);

        assertThat(response.prefix()).isEmpty();
        assertThat(response.count()).isZero();
        assertThat(response.suggestions()).isEmpty();
        assertThat(response.source()).isEqualTo("postgres");
        verifyNoInteractions(suggestionRepository, suggestionCacheService);
    }

    @Test
    void returnsCachedSuggestionsWhenRedisHasPrefix() {
        SuggestionService suggestionService = new SuggestionService(suggestionRepository, suggestionCacheService);
        SuggestionResponse cached = new SuggestionResponse(
            "iph",
            1,
            List.of(new SuggestionItem("iphone", 249943L)),
            "cache"
        );

        when(suggestionCacheService.getIfPresent("iph")).thenReturn(cached);

        SuggestionResponse response = suggestionService.suggest("iph");

        assertThat(response).isEqualTo(cached);
        verify(suggestionCacheService).getIfPresent("iph");
        verifyNoInteractions(suggestionRepository);
    }

    @Test
    void normalizesMixedCaseAndWhitespaceBeforeLookup() {
        SuggestionService suggestionService = new SuggestionService(suggestionRepository, suggestionCacheService);
        List<SuggestionItem> suggestions = List.of(new SuggestionItem("iphone 15", 249943L));
        SuggestionResponse postgresResponse = new SuggestionResponse("iph", 1, suggestions, "postgres");

        when(suggestionCacheService.getIfPresent("iph")).thenReturn(null);
        when(suggestionRepository.findSuggestions("iph", 10)).thenReturn(suggestions);
        when(suggestionCacheService.cacheResponse("iph", suggestions, "postgres")).thenReturn(postgresResponse);

        SuggestionResponse response = suggestionService.suggest("  IPH  ");

        assertThat(response.prefix()).isEqualTo("iph");
        assertThat(response.count()).isEqualTo(1);
        assertThat(response.suggestions()).containsExactlyElementsOf(suggestions);
        assertThat(response.source()).isEqualTo("postgres");
        verify(suggestionCacheService).getIfPresent("iph");
        verify(suggestionRepository).findSuggestions("iph", 10);
        verify(suggestionCacheService).cacheResponse("iph", suggestions, "postgres");
    }

    @Test
    void returnsEmptyResponseForNoMatches() {
        SuggestionService suggestionService = new SuggestionService(suggestionRepository, suggestionCacheService);
        SuggestionResponse postgresResponse = new SuggestionResponse("zzzzzz", 0, List.of(), "postgres");

        when(suggestionCacheService.getIfPresent("zzzzzz")).thenReturn(null);
        when(suggestionRepository.findSuggestions("zzzzzz", 10)).thenReturn(List.of());
        when(suggestionCacheService.cacheResponse("zzzzzz", List.of(), "postgres")).thenReturn(postgresResponse);

        SuggestionResponse response = suggestionService.suggest("zzzzzz");

        assertThat(response.prefix()).isEqualTo("zzzzzz");
        assertThat(response.count()).isZero();
        assertThat(response.suggestions()).isEmpty();
        assertThat(response.source()).isEqualTo("postgres");
    }
}
