package com.typeahead.suggest;

import java.util.List;

import org.springframework.stereotype.Service;

import com.typeahead.dataset.DatasetNormalizer;

@Service
public class SuggestionService {

    private static final int MAX_SUGGESTIONS = 10;
    private static final String SOURCE = "postgres";

    private final SuggestionRepository suggestionRepository;

    public SuggestionService(SuggestionRepository suggestionRepository) {
        this.suggestionRepository = suggestionRepository;
    }

    public SuggestionResponse suggest(String rawPrefix) {
        String normalizedPrefix = DatasetNormalizer.normalizeQuery(rawPrefix);
        if (normalizedPrefix.isEmpty()) {
            return emptyResponse();
        }

        List<SuggestionItem> suggestions = suggestionRepository.findSuggestions(normalizedPrefix, MAX_SUGGESTIONS);
        if (suggestions.isEmpty()) {
            return emptyResponse();
        }

        return new SuggestionResponse(normalizedPrefix, suggestions.size(), suggestions, SOURCE);
    }

    private SuggestionResponse emptyResponse() {
        return new SuggestionResponse("", 0, List.of(), SOURCE);
    }
}
