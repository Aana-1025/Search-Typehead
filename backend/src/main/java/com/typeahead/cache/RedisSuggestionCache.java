package com.typeahead.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typeahead.suggest.SuggestionItem;

@Component
public class RedisSuggestionCache {

    private static final TypeReference<List<SuggestionItem>> SUGGESTION_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSuggestionCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<List<SuggestionItem>> getSuggestions(String redisKey) {
        String cachedValue = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(cachedValue, SUGGESTION_LIST_TYPE));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize cached suggestions", exception);
        }
    }

    public void putSuggestions(String redisKey, List<SuggestionItem> suggestions, Duration ttl) {
        try {
            String payload = objectMapper.writeValueAsString(suggestions);
            stringRedisTemplate.opsForValue().set(redisKey, payload, ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize suggestions for cache", exception);
        }
    }

    public boolean hasKey(String redisKey) {
        Boolean hasKey = stringRedisTemplate.hasKey(redisKey);
        return Boolean.TRUE.equals(hasKey);
    }

    public long ttlSeconds(String redisKey) {
        Long ttlSeconds = stringRedisTemplate.getExpire(redisKey);
        return ttlSeconds == null ? -1L : ttlSeconds;
    }

    public void delete(String redisKey) {
        stringRedisTemplate.delete(redisKey);
    }
}
