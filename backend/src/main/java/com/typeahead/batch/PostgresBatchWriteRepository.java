package com.typeahead.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.typeahead.dataset.PrefixGenerator;
import com.typeahead.search.StoredSearchQuery;

@Repository
public class PostgresBatchWriteRepository implements BatchWriteRepository {

    private static final RowMapper<StoredSearchQuery> SEARCH_QUERY_ROW_MAPPER = (resultSet, rowNum) ->
        new StoredSearchQuery(
            resultSet.getLong("id"),
            resultSet.getString("query_text"),
            resultSet.getString("normalized_query"),
            resultSet.getLong("total_count"),
            resultSet.getDouble("trend_score")
        );

    private final JdbcTemplate jdbcTemplate;

    public PostgresBatchWriteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public StoredSearchQuery upsertQuery(String queryText, String normalizedQuery, long countDelta) {
        return jdbcTemplate.queryForObject(
            """
            INSERT INTO search_queries (
                query_text,
                normalized_query,
                total_count,
                trend_score,
                last_searched_at
            ) VALUES (?, ?, ?, ?, NOW())
            ON CONFLICT (normalized_query) DO UPDATE
            SET query_text = EXCLUDED.query_text,
                total_count = search_queries.total_count + EXCLUDED.total_count,
                trend_score = search_queries.total_count + EXCLUDED.total_count,
                last_searched_at = NOW(),
                updated_at = NOW()
            RETURNING id, query_text, normalized_query, total_count, trend_score
            """,
            SEARCH_QUERY_ROW_MAPPER,
            queryText,
            normalizedQuery,
            countDelta,
            (double) countDelta
        );
    }

    @Override
    public void replacePrefixes(StoredSearchQuery searchQuery, int prefixMaxLength) {
        jdbcTemplate.update("DELETE FROM query_prefixes WHERE query_id = ?", searchQuery.id());

        List<String> prefixes = PrefixGenerator.generatePrefixes(searchQuery.normalizedQuery(), prefixMaxLength);
        if (prefixes.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
            """
            INSERT INTO query_prefixes (
                prefix,
                query_id,
                query_text,
                normalized_query,
                total_count,
                trend_score
            ) VALUES (?, ?, ?, ?, ?, ?)
            """,
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement statement, int index) throws SQLException {
                    String prefix = prefixes.get(index);
                    statement.setString(1, prefix);
                    statement.setLong(2, searchQuery.id());
                    statement.setString(3, searchQuery.queryText());
                    statement.setString(4, searchQuery.normalizedQuery());
                    statement.setLong(5, searchQuery.totalCount());
                    statement.setDouble(6, searchQuery.trendScore());
                }

                @Override
                public int getBatchSize() {
                    return prefixes.size();
                }
            }
        );
    }

    @Override
    public void upsertActivityBucket(long queryId, LocalDateTime bucketStart, String bucketGranularity, long countDelta) {
        jdbcTemplate.update(
            """
            INSERT INTO query_activity_buckets (
                query_id,
                bucket_start,
                bucket_granularity,
                search_count
            ) VALUES (?, ?, ?, ?)
            ON CONFLICT (query_id, bucket_start, bucket_granularity) DO UPDATE
            SET search_count = query_activity_buckets.search_count + EXCLUDED.search_count
            """,
            queryId,
            Timestamp.valueOf(bucketStart),
            bucketGranularity,
            countDelta
        );
    }
}
