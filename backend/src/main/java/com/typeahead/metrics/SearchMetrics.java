package com.typeahead.metrics;

public record SearchMetrics(
    long accepted,
    long queued
) {
}
