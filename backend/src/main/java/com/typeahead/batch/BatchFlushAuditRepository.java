package com.typeahead.batch;

import java.sql.Timestamp;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BatchFlushAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public BatchFlushAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertAudit(BatchFlushResult batchFlushResult) {
        jdbcTemplate.update(
            """
            INSERT INTO batch_flush_audit (
                batch_id,
                raw_event_count,
                unique_query_count,
                db_write_count,
                started_at,
                finished_at,
                status
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            batchFlushResult.batchId(),
            batchFlushResult.rawEventCount(),
            batchFlushResult.uniqueQueryCount(),
            batchFlushResult.dbWriteCount(),
            Timestamp.from(batchFlushResult.startedAt()),
            Timestamp.from(batchFlushResult.finishedAt()),
            batchFlushResult.status().name()
        );
    }
}
