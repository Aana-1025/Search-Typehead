package com.typeahead.metrics;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class MetricsCounters {

    private final AtomicLong suggestionRequests = new AtomicLong();
    private final AtomicLong suggestionCacheHits = new AtomicLong();
    private final AtomicLong suggestionCacheMisses = new AtomicLong();
    private final AtomicLong suggestionPostgresReads = new AtomicLong();
    private final AtomicLong searchesAccepted = new AtomicLong();
    private final AtomicLong searchesQueued = new AtomicLong();
    private final AtomicLong trendingRequests = new AtomicLong();

    public void incrementSuggestionRequests() {
        suggestionRequests.incrementAndGet();
    }

    public void incrementSuggestionCacheHits() {
        suggestionCacheHits.incrementAndGet();
    }

    public void incrementSuggestionCacheMisses() {
        suggestionCacheMisses.incrementAndGet();
    }

    public void incrementSuggestionPostgresReads() {
        suggestionPostgresReads.incrementAndGet();
    }

    public void incrementSearchesAccepted() {
        searchesAccepted.incrementAndGet();
    }

    public void incrementSearchesQueued() {
        searchesQueued.incrementAndGet();
    }

    public void incrementTrendingRequests() {
        trendingRequests.incrementAndGet();
    }

    public long suggestionRequests() {
        return suggestionRequests.get();
    }

    public long suggestionCacheHits() {
        return suggestionCacheHits.get();
    }

    public long suggestionCacheMisses() {
        return suggestionCacheMisses.get();
    }

    public long suggestionPostgresReads() {
        return suggestionPostgresReads.get();
    }

    public long searchesAccepted() {
        return searchesAccepted.get();
    }

    public long searchesQueued() {
        return searchesQueued.get();
    }

    public long trendingRequests() {
        return trendingRequests.get();
    }
}
