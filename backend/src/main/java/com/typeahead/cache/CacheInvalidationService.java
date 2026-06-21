package com.typeahead.cache;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.typeahead.dataset.PrefixGenerator;

@Service
public class CacheInvalidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheInvalidationService.class);

    private final CacheProperties cacheProperties;
    private final SuggestionCacheService suggestionCacheService;

    public CacheInvalidationService(CacheProperties cacheProperties, SuggestionCacheService suggestionCacheService) {
        this.cacheProperties = cacheProperties;
        this.suggestionCacheService = suggestionCacheService;
    }

    public void invalidateQueryPrefixes(String normalizedQuery, int prefixMaxLength) {
        if (!cacheProperties.enabled() || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return;
        }

        List<String> prefixes = PrefixGenerator.generatePrefixes(normalizedQuery, prefixMaxLength);
        for (String prefix : prefixes) {
            try {
                suggestionCacheService.evictPrefix(prefix);
            } catch (RuntimeException exception) {
                LOGGER.warn("Redis cache invalidation failed for prefix {}", prefix, exception);
            }
        }
    }
}
