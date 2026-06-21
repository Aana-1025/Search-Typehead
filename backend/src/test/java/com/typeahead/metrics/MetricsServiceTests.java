package com.typeahead.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTests {

    @Mock
    private PostgresMetricsRepository postgresMetricsRepository;

    @Test
    void metricsSummaryReturnsDefaultZerosWhenNoEventsHappened() {
        MetricsCounters metricsCounters = new MetricsCounters();
        MetricsService metricsService = new MetricsService(metricsCounters, postgresMetricsRepository);

        when(postgresMetricsRepository.fetchBatchWriteMetrics()).thenReturn(new BatchWriteMetrics(0, 0, 0, 0));
        when(postgresMetricsRepository.fetchDatabaseMetrics()).thenReturn(new DatabaseMetrics(0, 0, 0, 0));

        MetricsSummaryResponse response = metricsService.summary();

        assertThat(response.suggestions()).isEqualTo(new SuggestionMetrics(0, 0, 0, 0.0, 0));
        assertThat(response.searches()).isEqualTo(new SearchMetrics(0, 0));
        assertThat(response.batchWrites()).isEqualTo(new BatchWriteMetrics(0, 0, 0, 0));
        assertThat(response.trending()).isEqualTo(new TrendingMetrics(0));
        assertThat(response.database()).isEqualTo(new DatabaseMetrics(0, 0, 0, 0));
        assertThat(response.source()).isEqualTo("application");
    }

    @Test
    void cacheHitRateCalculationWorks() {
        MetricsCounters metricsCounters = new MetricsCounters();
        MetricsService metricsService = new MetricsService(metricsCounters, postgresMetricsRepository);

        metricsCounters.incrementSuggestionRequests();
        metricsCounters.incrementSuggestionRequests();
        metricsCounters.incrementSuggestionRequests();
        metricsCounters.incrementSuggestionRequests();
        metricsCounters.incrementSuggestionCacheHits();
        metricsCounters.incrementSuggestionCacheHits();
        metricsCounters.incrementSuggestionCacheMisses();
        metricsCounters.incrementSuggestionCacheMisses();
        metricsCounters.incrementSuggestionPostgresReads();
        metricsCounters.incrementSuggestionPostgresReads();

        when(postgresMetricsRepository.fetchBatchWriteMetrics()).thenReturn(new BatchWriteMetrics(0, 0, 0, 0));
        when(postgresMetricsRepository.fetchDatabaseMetrics()).thenReturn(new DatabaseMetrics(0, 0, 0, 0));

        MetricsSummaryResponse response = metricsService.summary();

        assertThat(response.suggestions().cacheHitRate()).isEqualTo(0.5);
        assertThat(response.suggestions().cacheHits()).isEqualTo(2);
        assertThat(response.suggestions().cacheMisses()).isEqualTo(2);
        assertThat(response.suggestions().postgresReads()).isEqualTo(2);
    }

    @Test
    void batchDbWriteReductionIsCalculatedCorrectly() {
        MetricsCounters metricsCounters = new MetricsCounters();
        MetricsService metricsService = new MetricsService(metricsCounters, postgresMetricsRepository);

        when(postgresMetricsRepository.fetchBatchWriteMetrics()).thenReturn(new BatchWriteMetrics(3, 20, 5, 15));
        when(postgresMetricsRepository.fetchDatabaseMetrics()).thenReturn(new DatabaseMetrics(100000, 4369342, 3, 3));

        MetricsSummaryResponse response = metricsService.summary();

        assertThat(response.batchWrites().rawEventsProcessed()).isEqualTo(20);
        assertThat(response.batchWrites().uniqueQueryWrites()).isEqualTo(5);
        assertThat(response.batchWrites().estimatedDbWritesAvoided()).isEqualTo(15);
    }
}
