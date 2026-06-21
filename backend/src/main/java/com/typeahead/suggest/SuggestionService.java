package com.typeahead.suggest;

import java.util.List;

import org.springframework.stereotype.Service;

import com.typeahead.cache.SuggestionCacheService;
import com.typeahead.dataset.DatasetNormalizer;

@Service
public class SuggestionService {

    private static final int MAX_SUGGESTIONS = 10;
    private static final String SOURCE = "postgres";

    private final SuggestionRepository suggestionRepository;
    private final SuggestionCacheService suggestionCacheService;

    public SuggestionService(
        SuggestionRepository suggestionRepository,
        SuggestionCacheService suggestionCacheService
    ) {
        this.suggestionRepository = suggestionRepository;
        this.suggestionCacheService = suggestionCacheService;
    }

    public SuggestionResponse suggest(String rawPrefix) {
        String normalizedPrefix = DatasetNormalizer.normalizeQuery(rawPrefix);
        if (normalizedPrefix.isEmpty()) {
            return emptyResponse();
        }

        SuggestionResponse cachedResponse = suggestionCacheService.getIfPresent(normalizedPrefix);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        List<SuggestionItem> suggestions = suggestionRepository.findSuggestions(normalizedPrefix, MAX_SUGGESTIONS);
        return suggestionCacheService.cacheResponse(normalizedPrefix, suggestions, SOURCE);
    }

    private SuggestionResponse emptyResponse() {
        return new SuggestionResponse("", 0, List.of(), SOURCE);
    }
}
