package com.typeahead.suggest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTests {

    @Mock
    private SuggestionRepository suggestionRepository;

    @Test
    void returnsEmptyResponseForMissingPrefix() {
        SuggestionService suggestionService = new SuggestionService(suggestionRepository);

        SuggestionResponse response = suggestionService.suggest(null);

        assertThat(response.prefix()).isEmpty();
        assertThat(response.count()).isZero();
        assertThat(response.suggestions()).isEmpty();
        assertThat(response.source()).isEqualTo("postgres");
        verifyNoInteractions(suggestionRepository);
    }

    @Test
    void normalizesMixedCaseAndWhitespaceBeforeLookup() {
        SuggestionService suggestionService = new SuggestionService(suggestionRepository);
        List<SuggestionItem> suggestions = List.of(new SuggestionItem("iphone 15", 249943L));
        when(suggestionRepository.findSuggestions("iph", 10)).thenReturn(suggestions);

        SuggestionResponse response = suggestionService.suggest("  IPH  ");

        assertThat(response.prefix()).isEqualTo("iph");
        assertThat(response.count()).isEqualTo(1);
        assertThat(response.suggestions()).containsExactlyElementsOf(suggestions);
        assertThat(response.source()).isEqualTo("postgres");
    }

    @Test
    void returnsEmptyResponseForNoMatches() {
        SuggestionService suggestionService = new SuggestionService(suggestionRepository);
        when(suggestionRepository.findSuggestions("zzzzzz", 10)).thenReturn(List.of());

        SuggestionResponse response = suggestionService.suggest("zzzzzz");

        assertThat(response.prefix()).isEmpty();
        assertThat(response.count()).isZero();
        assertThat(response.suggestions()).isEmpty();
        assertThat(response.source()).isEqualTo("postgres");
    }
}
