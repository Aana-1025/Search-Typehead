package com.typeahead.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CacheKeyFactoryTests {

    @Test
    void buildsExpectedCacheKeyFormat() {
        assertThat(CacheKeyFactory.buildCacheKey("count", "iph"))
            .isEqualTo("suggest:v1:count:iph");
    }

    @Test
    void buildsExpectedRedisKeyWithOwnerNode() {
        assertThat(CacheKeyFactory.buildRedisKey("cache-node-b", "count", "iph"))
            .isEqualTo("suggest:cache-node-b:v1:count:iph");
    }
}
