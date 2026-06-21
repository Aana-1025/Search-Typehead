package com.typeahead.cache;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.typeahead.dataset.DatasetNormalizer;
import com.typeahead.suggest.SuggestionItem;
import com.typeahead.suggest.SuggestionResponse;

@Service
public class SuggestionCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestionCacheService.class);

    private final CacheProperties cacheProperties;
    private final ConsistentHashRing consistentHashRing;
    private final RedisSuggestionCache redisSuggestionCache;

    public SuggestionCacheService(
        CacheProperties cacheProperties,
        ConsistentHashRing consistentHashRing,
        RedisSuggestionCache redisSuggestionCache
    ) {
        this.cacheProperties = cacheProperties;
        this.consistentHashRing = consistentHashRing;
        this.redisSuggestionCache = redisSuggestionCache;
    }

    public SuggestionResponse getIfPresent(String normalizedPrefix) {
        if (!isCacheable(normalizedPrefix)) {
            return null;
        }

        String mode = CacheKeyFactory.MODE_COUNT;
        String cacheKey = CacheKeyFactory.buildCacheKey(mode, normalizedPrefix);
        CacheNode ownerNode = consistentHashRing.resolveOwner(cacheKey);
        String redisKey = CacheKeyFactory.buildRedisKey(ownerNode.name(), mode, normalizedPrefix);

        try {
            return redisSuggestionCache.getSuggestions(redisKey)
                .map(suggestions -> new SuggestionResponse(
                    normalizedPrefix,
                    suggestions.size(),
                    suggestions,
                    "cache"
                ))
                .orElse(null);
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis cache read failed for key {}", redisKey, exception);
            return null;
        }
    }

    public SuggestionResponse cacheResponse(String normalizedPrefix, List<SuggestionItem> suggestions, String source) {
        SuggestionResponse response = new SuggestionResponse(
            normalizedPrefix,
            suggestions.size(),
            suggestions,
            source
        );

        if (!isCacheable(normalizedPrefix)) {
            return response;
        }

        String mode = CacheKeyFactory.MODE_COUNT;
        String cacheKey = CacheKeyFactory.buildCacheKey(mode, normalizedPrefix);
        CacheNode ownerNode = consistentHashRing.resolveOwner(cacheKey);
        String redisKey = CacheKeyFactory.buildRedisKey(ownerNode.name(), mode, normalizedPrefix);

        try {
            redisSuggestionCache.putSuggestions(
                redisKey,
                suggestions,
                Duration.ofSeconds(cacheProperties.ttlSeconds())
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis cache write failed for key {}", redisKey, exception);
        }

        return response;
    }

    public CacheDebugResponse debug(String rawPrefix, String rawMode) {
        String normalizedPrefix = DatasetNormalizer.normalizeQuery(rawPrefix);
        String mode = normalizeMode(rawMode);

        if (!cacheProperties.enabled() || normalizedPrefix.isEmpty()) {
            return new CacheDebugResponse(
                cacheProperties.enabled(),
                normalizedPrefix,
                mode,
                normalizedPrefix.isEmpty() ? "" : CacheKeyFactory.buildCacheKey(mode, normalizedPrefix),
                "",
                "",
                consistentHashRing.logicalNodeNames(),
                "MISS",
                cacheProperties.ttlSeconds()
            );
        }

        String cacheKey = CacheKeyFactory.buildCacheKey(mode, normalizedPrefix);
        CacheNode ownerNode = consistentHashRing.resolveOwner(cacheKey);
        String redisKey = CacheKeyFactory.buildRedisKey(ownerNode.name(), mode, normalizedPrefix);

        try {
            boolean hit = redisSuggestionCache.hasKey(redisKey);
            long ttlSeconds = hit ? redisSuggestionCache.ttlSeconds(redisKey) : cacheProperties.ttlSeconds();

            return new CacheDebugResponse(
                true,
                normalizedPrefix,
                mode,
                cacheKey,
                redisKey,
                ownerNode.name(),
                consistentHashRing.logicalNodeNames(),
                hit ? "HIT" : "MISS",
                ttlSeconds
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis debug lookup failed for key {}", redisKey, exception);
            return new CacheDebugResponse(
                true,
                normalizedPrefix,
                mode,
                cacheKey,
                redisKey,
                ownerNode.name(),
                consistentHashRing.logicalNodeNames(),
                "ERROR",
                cacheProperties.ttlSeconds()
            );
        }
    }

    public void evictPrefix(String normalizedPrefix) {
        if (!isCacheable(normalizedPrefix)) {
            return;
        }

        String mode = CacheKeyFactory.MODE_COUNT;
        String cacheKey = CacheKeyFactory.buildCacheKey(mode, normalizedPrefix);
        CacheNode ownerNode = consistentHashRing.resolveOwner(cacheKey);
        String redisKey = CacheKeyFactory.buildRedisKey(ownerNode.name(), mode, normalizedPrefix);

        redisSuggestionCache.delete(redisKey);
    }

    private boolean isCacheable(String normalizedPrefix) {
        return cacheProperties.enabled() && normalizedPrefix != null && !normalizedPrefix.isEmpty();
    }

    private String normalizeMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return CacheKeyFactory.MODE_COUNT;
        }
        return rawMode.trim().toLowerCase();
    }
}
