package com.typeahead.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.typeahead.cache.CacheInvalidationService;
import com.typeahead.dataset.DatasetProperties;
import com.typeahead.search.StoredSearchQuery;

@ExtendWith(MockitoExtension.class)
class BatchWriteServiceTests {

    @Mock
    private BatchWriteRepository batchWriteRepository;

    @Mock
    private BatchFlushAuditRepository batchFlushAuditRepository;

    @Mock
    private CacheInvalidationService cacheInvalidationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    void aggregatesRepeatedQueriesIntoOneDatabaseWritePerUniqueQuery() {
        SearchEventQueue searchEventQueue = new SearchEventQueue();
        BatchWriteService batchWriteService = new BatchWriteService(
            new BatchWriteProperties(true, 5000, 500, 5000),
            new DatasetProperties(false, "", 1000, 50),
            searchEventQueue,
            batchWriteRepository,
            batchFlushAuditRepository,
            cacheInvalidationService,
            transactionTemplate
        );

        searchEventQueue.enqueue(new SearchEvent("batch test iphone", "batch test iphone", java.time.Instant.now()));
        searchEventQueue.enqueue(new SearchEvent("batch test iphone", "batch test iphone", java.time.Instant.now()));
        searchEventQueue.enqueue(new SearchEvent("iphone", "iphone", java.time.Instant.now()));

        when(batchWriteRepository.upsertQuery("batch test iphone", "batch test iphone", 2L))
            .thenReturn(new StoredSearchQuery(11L, "batch test iphone", "batch test iphone", 12L, 12.0));
        when(batchWriteRepository.upsertQuery("iphone", "iphone", 1L))
            .thenReturn(new StoredSearchQuery(12L, "iphone", "iphone", 50L, 50.0));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            org.springframework.transaction.support.TransactionCallback<BatchFlushResult> callback =
                invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        BatchFlushResult result = batchWriteService.flushNow();

        assertThat(result.status()).isEqualTo(BatchFlushStatus.SUCCESS);
        assertThat(result.rawEventCount()).isEqualTo(3);
        assertThat(result.uniqueQueryCount()).isEqualTo(2);
        assertThat(result.dbWriteCount()).isEqualTo(2);
        verify(batchWriteRepository).upsertQuery("batch test iphone", "batch test iphone", 2L);
        verify(batchWriteRepository).upsertQuery("iphone", "iphone", 1L);
        verify(batchWriteRepository).replacePrefixes(
            new StoredSearchQuery(11L, "batch test iphone", "batch test iphone", 12L, 12.0),
            50
        );
        verify(batchWriteRepository).replacePrefixes(
            new StoredSearchQuery(12L, "iphone", "iphone", 50L, 50.0),
            50
        );
        verify(batchWriteRepository, times(2)).upsertActivityBucket(anyLong(), any(), eq("HOUR"), anyLong());
        verify(batchFlushAuditRepository).insertAudit(any(BatchFlushResult.class));
        verify(cacheInvalidationService).invalidateQueryPrefixes("batch test iphone", 50);
        verify(cacheInvalidationService).invalidateQueryPrefixes("iphone", 50);
        assertThat(searchEventQueue.size()).isZero();
    }

    @Test
    void emptyFlushDoesNothingSafely() {
        SearchEventQueue searchEventQueue = new SearchEventQueue();
        BatchWriteService batchWriteService = new BatchWriteService(
            new BatchWriteProperties(true, 5000, 500, 5000),
            new DatasetProperties(false, "", 1000, 50),
            searchEventQueue,
            batchWriteRepository,
            batchFlushAuditRepository,
            cacheInvalidationService,
            transactionTemplate
        );

        BatchFlushResult result = batchWriteService.flushNow();

        assertThat(result.status()).isEqualTo(BatchFlushStatus.SKIPPED_EMPTY);
        verifyNoInteractions(batchWriteRepository, batchFlushAuditRepository, cacheInvalidationService);
    }

    @Test
    void recordsFailedAuditAndRequeuesEventsWhenDatabaseWriteFails() {
        SearchEventQueue searchEventQueue = new SearchEventQueue();
        BatchWriteService batchWriteService = new BatchWriteService(
            new BatchWriteProperties(true, 5000, 500, 5000),
            new DatasetProperties(false, "", 1000, 50),
            searchEventQueue,
            batchWriteRepository,
            batchFlushAuditRepository,
            cacheInvalidationService,
            transactionTemplate
        );

        searchEventQueue.enqueue(new SearchEvent("iphone", "iphone", java.time.Instant.now()));
        when(batchWriteRepository.upsertQuery("iphone", "iphone", 1L))
            .thenThrow(new IllegalStateException("db failure"));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            org.springframework.transaction.support.TransactionCallback<BatchFlushResult> callback =
                invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        org.assertj.core.api.Assertions.assertThatThrownBy(batchWriteService::flushNow)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("db failure");

        verify(batchFlushAuditRepository).insertAudit(any(BatchFlushResult.class));
        verify(cacheInvalidationService, never()).invalidateQueryPrefixes(any(), anyInt());
        assertThat(searchEventQueue.size()).isEqualTo(1);
    }

    @Test
    void scheduledNonEmptyFlushWritesAuditRow() {
        SearchEventQueue searchEventQueue = new SearchEventQueue();
        BatchWriteService batchWriteService = new BatchWriteService(
            new BatchWriteProperties(true, 5000, 500, 5000),
            new DatasetProperties(false, "", 1000, 50),
            searchEventQueue,
            batchWriteRepository,
            batchFlushAuditRepository,
            cacheInvalidationService,
            transactionTemplate
        );

        searchEventQueue.enqueue(new SearchEvent("iphone", "iphone", java.time.Instant.now()));
        when(batchWriteRepository.upsertQuery("iphone", "iphone", 1L))
            .thenReturn(new StoredSearchQuery(7L, "iphone", "iphone", 20L, 20.0));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            org.springframework.transaction.support.TransactionCallback<BatchFlushResult> callback =
                invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        batchWriteService.scheduledFlush();

        verify(batchFlushAuditRepository).insertAudit(any(BatchFlushResult.class));
        verify(cacheInvalidationService).invalidateQueryPrefixes("iphone", 50);
        assertThat(searchEventQueue.size()).isZero();
    }
}
