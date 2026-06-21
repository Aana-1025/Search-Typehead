package com.typeahead.trending;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresTrendingRepository implements TrendingRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresTrendingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TrendingItem> findTrendingSince(Instant cutoffInclusive) {
        return jdbcTemplate.query(
            """
            SELECT sq.query_text,
                   sq.total_count,
                   SUM(qab.search_count) AS recent_count
            FROM query_activity_buckets qab
            JOIN search_queries sq
              ON sq.id = qab.query_id
            WHERE qab.bucket_granularity = 'HOUR'
              AND qab.bucket_start >= ?
            GROUP BY sq.id, sq.query_text, sq.total_count
            """,
            (resultSet, rowNum) -> {
                long recentCount = resultSet.getLong("recent_count");
                return new TrendingItem(
                    resultSet.getString("query_text"),
                    resultSet.getLong("total_count"),
                    recentCount,
                    (double) recentCount
                );
            },
            Timestamp.from(cutoffInclusive)
        );
    }
}
