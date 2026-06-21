package com.typeahead.metrics;

import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private static final String SOURCE = "application";

    private final MetricsCounters metricsCounters;
    private final PostgresMetricsRepository postgresMetricsRepository;

    public MetricsService(MetricsCounters metricsCounters, PostgresMetricsRepository postgresMetricsRepository) {
        this.metricsCounters = metricsCounters;
        this.postgresMetricsRepository = postgresMetricsRepository;
    }

    public MetricsSummaryResponse summary() {
        long suggestionRequests = metricsCounters.suggestionRequests();
        long suggestionCacheHits = metricsCounters.suggestionCacheHits();
        long suggestionCacheMisses = metricsCounters.suggestionCacheMisses();
        long suggestionPostgresReads = metricsCounters.suggestionPostgresReads();

        double cacheHitRate = suggestionRequests == 0
            ? 0.0
            : (double) suggestionCacheHits / suggestionRequests;

        return new MetricsSummaryResponse(
            new SuggestionMetrics(
                suggestionRequests,
                suggestionCacheHits,
                suggestionCacheMisses,
                cacheHitRate,
                suggestionPostgresReads
            ),
            new SearchMetrics(
                metricsCounters.searchesAccepted(),
                metricsCounters.searchesQueued()
            ),
            postgresMetricsRepository.fetchBatchWriteMetrics(),
            new TrendingMetrics(metricsCounters.trendingRequests()),
            postgresMetricsRepository.fetchDatabaseMetrics(),
            SOURCE
        );
    }
}
