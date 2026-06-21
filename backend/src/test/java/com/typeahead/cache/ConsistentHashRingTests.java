package com.typeahead.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ConsistentHashRingTests {

    private final ConsistentHashRing consistentHashRing = new ConsistentHashRing(
        List.of("cache-node-a", "cache-node-b", "cache-node-c"),
        100
    );

    @Test
    void sameKeyMapsToSameNode() {
        String cacheKey = CacheKeyFactory.buildCacheKey(CacheKeyFactory.MODE_COUNT, "iph");

        CacheNode first = consistentHashRing.resolveOwner(cacheKey);
        CacheNode second = consistentHashRing.resolveOwner(cacheKey);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentKeysResolveToConfiguredLogicalNodes() {
        CacheNode first = consistentHashRing.resolveOwner(
            CacheKeyFactory.buildCacheKey(CacheKeyFactory.MODE_COUNT, "iph")
        );
        CacheNode second = consistentHashRing.resolveOwner(
            CacheKeyFactory.buildCacheKey(CacheKeyFactory.MODE_COUNT, "spring boot")
        );

        assertThat(consistentHashRing.logicalNodeNames()).contains(first.name(), second.name());
    }

    @Test
    void exposesConfiguredLogicalNodes() {
        assertThat(consistentHashRing.logicalNodeNames())
            .containsExactly("cache-node-a", "cache-node-b", "cache-node-c");
    }
}
