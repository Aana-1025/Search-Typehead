package com.typeahead.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.batch")
public record BatchWriteProperties(
    boolean enabled,
    long flushIntervalMs,
    int maxEvents,
    int maxDrainEvents
) {
}
