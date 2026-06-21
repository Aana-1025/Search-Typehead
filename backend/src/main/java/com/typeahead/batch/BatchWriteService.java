package com.typeahead.batch;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.typeahead.cache.CacheInvalidationService;
import com.typeahead.dataset.DatasetProperties;
import com.typeahead.search.StoredSearchQuery;

@Service
public class BatchWriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchWriteService.class);
    private static final String BUCKET_GRANULARITY = "HOUR";

    private final BatchWriteProperties batchWriteProperties;
    private final DatasetProperties datasetProperties;
    private final SearchEventQueue searchEventQueue;
    private final BatchWriteRepository batchWriteRepository;
    private final BatchFlushAuditRepository batchFlushAuditRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final TransactionTemplate transactionTemplate;
    private final AtomicBoolean flushInProgress = new AtomicBoolean(false);
    private final ExecutorService flushExecutor = Executors.newSingleThreadExecutor();

    public BatchWriteService(
        BatchWriteProperties batchWriteProperties,
        DatasetProperties datasetProperties,
        SearchEventQueue searchEventQueue,
        BatchWriteRepository batchWriteRepository,
        BatchFlushAuditRepository batchFlushAuditRepository,
        CacheInvalidationService cacheInvalidationService,
        TransactionTemplate transactionTemplate
    ) {
        this.batchWriteProperties = batchWriteProperties;
        this.datasetProperties = datasetProperties;
        this.searchEventQueue = searchEventQueue;
        this.batchWriteRepository = batchWriteRepository;
        this.batchFlushAuditRepository = batchFlushAuditRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.transactionTemplate = transactionTemplate;
    }

    public BatchDebugResponse debug() {
        return new BatchDebugResponse(
            batchWriteProperties.enabled(),
            searchEventQueue.size(),
            batchWriteProperties.flushIntervalMs(),
            batchWriteProperties.maxEvents()
        );
    }

    public void triggerAsyncFlushIfNeeded(int queueSize) {
        if (!batchWriteProperties.enabled() || queueSize < batchWriteProperties.maxEvents()) {
            return;
        }

        if (flushInProgress.compareAndSet(false, true)) {
            flushExecutor.submit(this::flushWithLockHeld);
        }
    }

    @Scheduled(fixedDelayString = "${app.batch.flush-interval-ms:5000}")
    public void scheduledFlush() {
        if (!batchWriteProperties.enabled()) {
            return;
        }

        if (flushInProgress.compareAndSet(false, true)) {
            flushWithLockHeld();
        }
    }

    public BatchFlushResult flushNow() {
        if (!batchWriteProperties.enabled()) {
            Instant now = Instant.now();
            return new BatchFlushResult(UUID.randomUUID(), 0, 0, 0, now, now, BatchFlushStatus.SKIPPED_EMPTY);
        }

        if (!flushInProgress.compareAndSet(false, true)) {
            Instant now = Instant.now();
            return new BatchFlushResult(UUID.randomUUID(), 0, 0, 0, now, now, BatchFlushStatus.SKIPPED_BUSY);
        }

        return flushWithLockHeld();
    }

    @PreDestroy
    void shutdownExecutor() {
        flushExecutor.shutdown();
    }

    private BatchFlushResult flushWithLockHeld() {
        try {
            return drainAggregateWriteAndAudit();
        } finally {
            flushInProgress.set(false);
        }
    }

    private BatchFlushResult drainAggregateWriteAndAudit() {
        Instant startedAt = Instant.now();
        UUID batchId = UUID.randomUUID();
        List<SearchEvent> drainedEvents = searchEventQueue.drain(batchWriteProperties.maxDrainEvents());

        if (drainedEvents.isEmpty()) {
            return new BatchFlushResult(batchId, 0, 0, 0, startedAt, Instant.now(), BatchFlushStatus.SKIPPED_EMPTY);
        }

        Map<String, AggregatedSearchUpdate> aggregatedUpdates = aggregate(drainedEvents);

        try {
            BatchFlushResult successResult = transactionTemplate.execute(transactionStatus -> {
                BatchFlushResult flushedResult = flushAggregatedUpdates(
                    batchId,
                    startedAt,
                    drainedEvents.size(),
                    aggregatedUpdates
                );
                batchFlushAuditRepository.insertAudit(flushedResult);
                return flushedResult;
            });

            if (successResult == null) {
                throw new IllegalStateException("Batch flush transaction returned no result");
            }

            invalidateCaches(aggregatedUpdates.keySet());
            logResult(successResult);
            return successResult;
        } catch (RuntimeException exception) {
            searchEventQueue.requeueAll(drainedEvents);
            BatchFlushResult failedResult = new BatchFlushResult(
                batchId,
                drainedEvents.size(),
                aggregatedUpdates.size(),
                0,
                startedAt,
                Instant.now(),
                BatchFlushStatus.FAILED
            );
            try {
                batchFlushAuditRepository.insertAudit(failedResult);
            } catch (RuntimeException auditException) {
                LOGGER.warn("BATCH_FLUSH audit write failed batchId={}", batchId, auditException);
            }
            logFailure(failedResult, exception);
            throw exception;
        }
    }

    private BatchFlushResult flushAggregatedUpdates(
        UUID batchId,
        Instant startedAt,
        int rawEventCount,
        Map<String, AggregatedSearchUpdate> aggregatedUpdates
    ) {
        LocalDateTime bucketStart = LocalDateTime.ofInstant(startedAt, ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS);
        long dbWriteCount = 0;

        for (AggregatedSearchUpdate aggregatedUpdate : aggregatedUpdates.values()) {
            StoredSearchQuery updatedQuery = batchWriteRepository.upsertQuery(
                aggregatedUpdate.queryText(),
                aggregatedUpdate.normalizedQuery(),
                aggregatedUpdate.countDelta()
            );
            batchWriteRepository.replacePrefixes(updatedQuery, datasetProperties.prefixMaxLength());
            batchWriteRepository.upsertActivityBucket(
                updatedQuery.id(),
                bucketStart,
                BUCKET_GRANULARITY,
                aggregatedUpdate.countDelta()
            );
            dbWriteCount++;
        }

        return new BatchFlushResult(
            batchId,
            rawEventCount,
            aggregatedUpdates.size(),
            dbWriteCount,
            startedAt,
            Instant.now(),
            BatchFlushStatus.SUCCESS
        );
    }

    private Map<String, AggregatedSearchUpdate> aggregate(List<SearchEvent> drainedEvents) {
        Map<String, AggregatedSearchUpdate> aggregatedUpdates = new LinkedHashMap<>();

        for (SearchEvent searchEvent : drainedEvents) {
            aggregatedUpdates.compute(
                searchEvent.normalizedQuery(),
                (normalizedQuery, existing) -> {
                    if (existing == null) {
                        return new AggregatedSearchUpdate(
                            searchEvent.queryText(),
                            normalizedQuery,
                            1L
                        );
                    }

                    return new AggregatedSearchUpdate(
                        searchEvent.queryText(),
                        normalizedQuery,
                        existing.countDelta() + 1L
                    );
                }
            );
        }

        return aggregatedUpdates;
    }

    private void invalidateCaches(Iterable<String> normalizedQueries) {
        for (String normalizedQuery : normalizedQueries) {
            try {
                cacheInvalidationService.invalidateQueryPrefixes(normalizedQuery, datasetProperties.prefixMaxLength());
            } catch (RuntimeException exception) {
                LOGGER.warn("BATCH_FLUSH cache invalidation failed normalizedQuery={}", normalizedQuery, exception);
            }
        }
    }

    private void logResult(BatchFlushResult result) {
        long durationMs = ChronoUnit.MILLIS.between(result.startedAt(), result.finishedAt());
        LOGGER.info(
            "BATCH_FLUSH batchId={} rawEvents={} uniqueQueries={} dbWrites={} durationMs={} status={}",
            result.batchId(),
            result.rawEventCount(),
            result.uniqueQueryCount(),
            result.dbWriteCount(),
            durationMs,
            result.status()
        );
    }

    private void logFailure(BatchFlushResult result, RuntimeException exception) {
        long durationMs = ChronoUnit.MILLIS.between(result.startedAt(), result.finishedAt());
        LOGGER.warn(
            "BATCH_FLUSH batchId={} rawEvents={} uniqueQueries={} dbWrites={} durationMs={} status={}",
            result.batchId(),
            result.rawEventCount(),
            result.uniqueQueryCount(),
            result.dbWriteCount(),
            durationMs,
            result.status(),
            exception
        );
    }

    private record AggregatedSearchUpdate(
        String queryText,
        String normalizedQuery,
        long countDelta
    ) {
    }
}
