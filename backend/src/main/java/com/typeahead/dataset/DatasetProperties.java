package com.typeahead.dataset;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dataset")
public record DatasetProperties(
    boolean load,
    String path,
    int batchSize,
    int prefixMaxLength
) {
}
