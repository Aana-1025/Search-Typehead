package com.typeahead.suggest;

import java.util.List;

import org.springframework.stereotype.Service;

import com.typeahead.cache.SuggestionCacheService;
import com.typeahead.dataset.DatasetNormalizer;
import com.typeahead.metrics.MetricsCounters;

@Service
public class SuggestionService {

    private static final int MAX_SUGGESTIONS = 10;
    private static final String SOURCE = "postgres";

    private final SuggestionRepository suggestionRepository;
    private final SuggestionCacheService suggestionCacheService;
    private final MetricsCounters metricsCounters;

    public SuggestionService(
        SuggestionRepository suggestionRepository,
        SuggestionCacheService suggestionCacheService,
        MetricsCounters metricsCounters
    ) {
        this.suggestionRepository = suggestionRepository;
        this.suggestionCacheService = suggestionCacheService;
        this.metricsCounters = metricsCounters;
    }

    public SuggestionResponse suggest(String rawPrefix) {
        metricsCounters.incrementSuggestionRequests();

        String normalizedPrefix = DatasetNormalizer.normalizeQuery(rawPrefix);
        if (normalizedPrefix.isEmpty()) {
            return emptyResponse();
        }

        SuggestionResponse cachedResponse = suggestionCacheService.getIfPresent(normalizedPrefix);
        if (cachedResponse != null) {
            metricsCounters.incrementSuggestionCacheHits();
            return cachedResponse;
        }

        metricsCounters.incrementSuggestionCacheMisses();
        metricsCounters.incrementSuggestionPostgresReads();
        List<SuggestionItem> suggestions = suggestionRepository.findSuggestions(normalizedPrefix, MAX_SUGGESTIONS);
        return suggestionCacheService.cacheResponse(normalizedPrefix, suggestions, SOURCE);
    }

    private SuggestionResponse emptyResponse() {
        return new SuggestionResponse("", 0, List.of(), SOURCE);
    }
}
