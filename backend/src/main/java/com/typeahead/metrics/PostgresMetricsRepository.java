package com.typeahead.metrics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DatabaseMetrics fetchDatabaseMetrics() {
        return new DatabaseMetrics(
            countTable("search_queries"),
            countTable("query_prefixes"),
            countTable("query_activity_buckets"),
            countTable("batch_flush_audit")
        );
    }

    public BatchWriteMetrics fetchBatchWriteMetrics() {
        return jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) AS flushes,
                   COALESCE(SUM(raw_event_count), 0) AS raw_events_processed,
                   COALESCE(SUM(unique_query_count), 0) AS unique_query_writes
            FROM batch_flush_audit
            WHERE status = 'SUCCESS'
            """,
            (resultSet, rowNum) -> {
                long flushes = resultSet.getLong("flushes");
                long rawEventsProcessed = resultSet.getLong("raw_events_processed");
                long uniqueQueryWrites = resultSet.getLong("unique_query_writes");
                return new BatchWriteMetrics(
                    flushes,
                    rawEventsProcessed,
                    uniqueQueryWrites,
                    Math.max(0L, rawEventsProcessed - uniqueQueryWrites)
                );
            }
        );
    }

    private long countTable(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count == null ? 0L : count;
    }
}
