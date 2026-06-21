package com.typeahead.suggest;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresSuggestionRepository implements SuggestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresSuggestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SuggestionItem> findSuggestions(String normalizedPrefix, int limit) {
        return jdbcTemplate.query(
            """
            SELECT query_text, total_count
            FROM query_prefixes
            WHERE prefix = ?
              AND normalized_query LIKE ?
            ORDER BY total_count DESC, query_text ASC
            LIMIT ?
            """,
            (resultSet, rowNum) -> new SuggestionItem(
                resultSet.getString("query_text"),
                resultSet.getLong("total_count")
            ),
            normalizedPrefix,
            normalizedPrefix + "%",
            limit
        );
    }
}
