package com.typeahead.batch;

import java.time.Instant;

public record SearchEvent(
    String queryText,
    String normalizedQuery,
    Instant createdAt
) {
}
