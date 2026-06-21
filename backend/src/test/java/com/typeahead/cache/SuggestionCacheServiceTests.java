package com.typeahead.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.typeahead.suggest.SuggestionItem;
import com.typeahead.suggest.SuggestionResponse;

@ExtendWith(MockitoExtension.class)
class SuggestionCacheServiceTests {

    @Mock
    private RedisSuggestionCache redisSuggestionCache;

    private final CacheProperties cacheProperties = new CacheProperties(
        true,
        300,
        100,
        List.of("cache-node-a", "cache-node-b", "cache-node-c")
    );

    private final ConsistentHashRing consistentHashRing = new ConsistentHashRing(
        List.of("cache-node-a", "cache-node-b", "cache-node-c"),
        100
    );

    @Test
    void cacheHitReturnsCachedResponse() {
        SuggestionCacheService suggestionCacheService = new SuggestionCacheService(
            cacheProperties,
            consistentHashRing,
            redisSuggestionCache
        );
        String cacheKey = CacheKeyFactory.buildCacheKey("count", "iph");
        String redisKey = CacheKeyFactory.buildRedisKey(
            consistentHashRing.resolveOwner(cacheKey).name(),
            "count",
            "iph"
        );
        List<SuggestionItem> cachedSuggestions = List.of(new SuggestionItem("iphone", 249945L));

        when(redisSuggestionCache.getSuggestions(redisKey)).thenReturn(Optional.of(cachedSuggestions));

        SuggestionResponse response = suggestionCacheService.getIfPresent("iph");

        assertThat(response).isEqualTo(new SuggestionResponse("iph", 1, cachedSuggestions, "cache"));
    }

    @Test
    void cacheMissFallsBackToPostgresResponseAndCachesResult() {
        SuggestionCacheService suggestionCacheService = new SuggestionCacheService(
            cacheProperties,
            consistentHashRing,
            redisSuggestionCache
        );
        List<SuggestionItem> suggestions = List.of(new SuggestionItem("iphone", 249945L));
        String cacheKey = CacheKeyFactory.buildCacheKey("count", "iph");
        String redisKey = CacheKeyFactory.buildRedisKey(
            consistentHashRing.resolveOwner(cacheKey).name(),
            "count",
            "iph"
        );

        when(redisSuggestionCache.getSuggestions(redisKey)).thenReturn(Optional.empty());

        SuggestionResponse cachedMiss = suggestionCacheService.getIfPresent("iph");
        SuggestionResponse postgresResponse = suggestionCacheService.cacheResponse("iph", suggestions, "postgres");

        assertThat(cachedMiss).isNull();
        assertThat(postgresResponse).isEqualTo(new SuggestionResponse("iph", 1, suggestions, "postgres"));
        verify(redisSuggestionCache).putSuggestions(
            org.mockito.ArgumentMatchers.eq(redisKey),
            org.mockito.ArgumentMatchers.eq(suggestions),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void redisFailureFallsBackToPostgresResponse() {
        SuggestionCacheService suggestionCacheService = new SuggestionCacheService(
            cacheProperties,
            consistentHashRing,
            redisSuggestionCache
        );
        List<SuggestionItem> postgresSuggestions = List.of(new SuggestionItem("iphone", 249945L));
        String cacheKey = CacheKeyFactory.buildCacheKey("count", "iph");
        String redisKey = CacheKeyFactory.buildRedisKey(
            consistentHashRing.resolveOwner(cacheKey).name(),
            "count",
            "iph"
        );

        when(redisSuggestionCache.getSuggestions(redisKey)).thenThrow(new IllegalStateException("redis unavailable"));

        SuggestionResponse cachedResponse = suggestionCacheService.getIfPresent("iph");
        SuggestionResponse fallbackResponse = suggestionCacheService.cacheResponse(
            "iph",
            postgresSuggestions,
            "postgres"
        );

        assertThat(cachedResponse).isNull();
        assertThat(fallbackResponse).isEqualTo(new SuggestionResponse("iph", 1, postgresSuggestions, "postgres"));
    }

    @Test
    void cacheDisabledSkipsRedisLookup() {
        SuggestionCacheService suggestionCacheService = new SuggestionCacheService(
            new CacheProperties(false, 300, 100, List.of("cache-node-a")),
            new ConsistentHashRing(List.of("cache-node-a"), 100),
            redisSuggestionCache
        );

        SuggestionResponse response = suggestionCacheService.cacheResponse("iph", List.of(), "postgres");

        assertThat(response).isEqualTo(new SuggestionResponse("iph", 0, List.of(), "postgres"));
        verifyNoInteractions(redisSuggestionCache);
    }
}
