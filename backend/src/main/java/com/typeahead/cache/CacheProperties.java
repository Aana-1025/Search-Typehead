package com.typeahead.cache;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
    boolean enabled,
    long ttlSeconds,
    int virtualNodes,
    List<String> logicalNodes
) {
}
