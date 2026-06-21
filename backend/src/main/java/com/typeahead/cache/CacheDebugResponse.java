package com.typeahead.cache;

import java.util.List;

public record CacheDebugResponse(
    boolean enabled,
    String prefix,
    String mode,
    String cacheKey,
    String redisKey,
    String ownerNode,
    List<String> logicalNodes,
    String status,
    long ttlSeconds
) {
}
