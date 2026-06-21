package com.typeahead.batch;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BatchWriteController.class)
class BatchWriteControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BatchWriteService batchWriteService;

    @Test
    void returnsBatchDebugPayload() throws Exception {
        when(batchWriteService.debug()).thenReturn(new BatchDebugResponse(true, 0, 5000, 500));

        mockMvc.perform(get("/batch/debug"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "enabled": true,
                  "queueSize": 0,
                  "flushIntervalMs": 5000,
                  "maxEvents": 500
                }
                """));
    }

    @Test
    void triggersManualFlushEndpoint() throws Exception {
        when(batchWriteService.flushNow()).thenReturn(new BatchFlushResult(
            UUID.randomUUID(),
            10,
            1,
            1,
            Instant.now(),
            Instant.now(),
            BatchFlushStatus.SUCCESS
        ));

        mockMvc.perform(post("/batch/flush"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "status":"SUCCESS",
                  "rawEventCount":10,
                  "uniqueQueryCount":1,
                  "dbWriteCount":1
                }
                """));
    }

    @Test
    void returnsSuccessfulEmptyFlushPayload() throws Exception {
        when(batchWriteService.flushNow()).thenReturn(new BatchFlushResult(
            UUID.randomUUID(),
            0,
            0,
            0,
            Instant.now(),
            Instant.now(),
            BatchFlushStatus.SKIPPED_EMPTY
        ));

        mockMvc.perform(post("/batch/flush"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "status":"SKIPPED_EMPTY",
                  "rawEventCount":0,
                  "uniqueQueryCount":0,
                  "dbWriteCount":0
                }
                """));
    }
}
