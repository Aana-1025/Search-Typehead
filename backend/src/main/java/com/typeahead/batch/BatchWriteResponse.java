package com.typeahead.batch;

public record BatchWriteResponse(
    String status,
    long rawEventCount,
    long uniqueQueryCount,
    long dbWriteCount
) {
}
