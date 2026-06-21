package com.typeahead.search;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.typeahead.dataset.PrefixGenerator;

@Repository
public class PostgresSearchRepository implements SearchRepository {

    private static final RowMapper<StoredSearchQuery> SEARCH_QUERY_ROW_MAPPER = (resultSet, rowNum) ->
        new StoredSearchQuery(
            resultSet.getLong("id"),
            resultSet.getString("query_text"),
            resultSet.getString("normalized_query"),
            resultSet.getLong("total_count"),
            resultSet.getDouble("trend_score")
        );

    private final JdbcTemplate jdbcTemplate;

    public PostgresSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<StoredSearchQuery> findByNormalizedQuery(String normalizedQuery) {
        List<StoredSearchQuery> matches = jdbcTemplate.query(
            """
            SELECT id, query_text, normalized_query, total_count, trend_score
            FROM search_queries
            WHERE normalized_query = ?
            """,
            SEARCH_QUERY_ROW_MAPPER,
            normalizedQuery
        );
        return matches.stream().findFirst();
    }

    @Override
    public StoredSearchQuery insertNewQuery(String queryText, String normalizedQuery) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO search_queries (
                    query_text,
                    normalized_query,
                    total_count,
                    trend_score,
                    last_searched_at
                ) VALUES (?, ?, 1, 1, NOW())
                """,
                new String[] { "id" }
            );
            statement.setString(1, queryText);
            statement.setString(2, normalizedQuery);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create search query row");
        }

        return findById(key.longValue());
    }

    @Override
    public StoredSearchQuery incrementQuery(long id) {
        jdbcTemplate.update(
            """
            UPDATE search_queries
            SET total_count = total_count + 1,
                trend_score = total_count + 1,
                last_searched_at = NOW(),
                updated_at = NOW()
            WHERE id = ?
            """,
            id
        );
        return findById(id);
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

    private StoredSearchQuery findById(long id) {
        return jdbcTemplate.queryForObject(
            """
            SELECT id, query_text, normalized_query, total_count, trend_score
            FROM search_queries
            WHERE id = ?
            """,
            SEARCH_QUERY_ROW_MAPPER,
            id
        );
    }
}
