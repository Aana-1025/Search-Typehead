package com.typeahead.cache;

public final class CacheKeyFactory {

    public static final String MODE_COUNT = "count";

    private CacheKeyFactory() {
    }

    public static String buildCacheKey(String mode, String normalizedPrefix) {
        return "suggest:v1:%s:%s".formatted(mode, normalizedPrefix);
    }

    public static String buildRedisKey(String ownerNode, String mode, String normalizedPrefix) {
        return "suggest:%s:v1:%s:%s".formatted(ownerNode, mode, normalizedPrefix);
    }
}
