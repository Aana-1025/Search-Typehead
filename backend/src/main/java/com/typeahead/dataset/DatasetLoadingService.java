package com.typeahead.dataset;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "app.dataset", name = "load", havingValue = "true")
public class DatasetLoadingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetLoadingService.class);

    private final DatasetCsvReader datasetCsvReader;
    private final JdbcTemplate jdbcTemplate;

    public DatasetLoadingService(DatasetCsvReader datasetCsvReader, JdbcTemplate jdbcTemplate) {
        this.datasetCsvReader = datasetCsvReader;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public DatasetLoadSummary loadDataset(DatasetProperties properties) throws Exception {
        Instant start = Instant.now();
        Path datasetPath = Path.of(properties.path()).normalize();
        List<CsvQueryRecord> records = datasetCsvReader.read(datasetPath);

        clearSeedData();
        batchInsertSearchQueries(records, properties.batchSize());
        List<LoadedSearchQuery> loadedQueries = fetchInsertedQueries();
        long prefixesInserted = batchInsertPrefixes(loadedQueries, properties.batchSize(), properties.prefixMaxLength());

        return new DatasetLoadSummary(
            datasetPath,
            records.size(),
            loadedQueries.size(),
            prefixesInserted,
            properties.prefixMaxLength(),
            Duration.between(start, Instant.now())
        );
    }

    private void clearSeedData() {
        jdbcTemplate.execute("TRUNCATE TABLE query_activity_buckets, query_prefixes, search_queries RESTART IDENTITY");
        LOGGER.info("Cleared existing seed data from query_activity_buckets, query_prefixes, and search_queries");
    }

    private void batchInsertSearchQueries(List<CsvQueryRecord> records, int batchSize) {
        String sql = """
            INSERT INTO search_queries (
                query_text,
                normalized_query,
                total_count,
                trend_score
            ) VALUES (?, ?, ?, ?)
            """;

        int batchNumber = 0;
        for (int start = 0; start < records.size(); start += batchSize) {
            List<CsvQueryRecord> batch = records.subList(start, Math.min(start + batchSize, records.size()));
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement statement, int index) throws SQLException {
                    CsvQueryRecord record = batch.get(index);
                    statement.setString(1, record.queryText());
                    statement.setString(2, record.normalizedQuery());
                    statement.setLong(3, record.count());
                    statement.setDouble(4, (double) record.count());
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
            batchNumber++;
            if (batchNumber % 10 == 0 || start + batch.size() == records.size()) {
                LOGGER.info(
                    "Inserted search_queries batch {} ({} / {})",
                    batchNumber,
                    start + batch.size(),
                    records.size()
                );
            }
        }
    }

    private List<LoadedSearchQuery> fetchInsertedQueries() {
        return jdbcTemplate.query(
            """
            SELECT id, query_text, normalized_query, total_count, trend_score
            FROM search_queries
            ORDER BY id
            """,
            (resultSet, rowNumber) -> new LoadedSearchQuery(
                resultSet.getLong("id"),
                resultSet.getString("query_text"),
                resultSet.getString("normalized_query"),
                resultSet.getLong("total_count"),
                resultSet.getDouble("trend_score")
            )
        );
    }

    private long batchInsertPrefixes(List<LoadedSearchQuery> queries, int batchSize, int prefixMaxLength) {
        String sql = """
            INSERT INTO query_prefixes (
                prefix,
                query_id,
                query_text,
                normalized_query,
                total_count,
                trend_score
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

        List<QueryPrefixRecord> batch = new ArrayList<>(batchSize);
        long insertedCount = 0;
        int prefixBatchNumber = 0;

        for (LoadedSearchQuery query : queries) {
            for (String prefix : PrefixGenerator.generatePrefixes(query.normalizedQuery(), prefixMaxLength)) {
                batch.add(new QueryPrefixRecord(
                    prefix,
                    query.id(),
                    query.queryText(),
                    query.normalizedQuery(),
                    query.totalCount(),
                    query.trendScore()
                ));

                if (batch.size() == batchSize) {
                    insertedCount += executePrefixBatch(sql, batch);
                    prefixBatchNumber++;
                    if (prefixBatchNumber % 50 == 0) {
                        LOGGER.info("Inserted query_prefixes batch {} ({} rows so far)", prefixBatchNumber, insertedCount);
                    }
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            insertedCount += executePrefixBatch(sql, batch);
            prefixBatchNumber++;
            LOGGER.info("Inserted final query_prefixes batch {} ({} rows total)", prefixBatchNumber, insertedCount);
        }

        return insertedCount;
    }

    private int executePrefixBatch(String sql, List<QueryPrefixRecord> batch) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                QueryPrefixRecord record = batch.get(index);
                statement.setString(1, record.prefix());
                statement.setLong(2, record.queryId());
                statement.setString(3, record.queryText());
                statement.setString(4, record.normalizedQuery());
                statement.setLong(5, record.totalCount());
                statement.setDouble(6, record.trendScore());
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
        return batch.size();
    }
}
