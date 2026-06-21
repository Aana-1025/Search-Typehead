package com.typeahead.metrics;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MetricsController.class)
class MetricsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsService metricsService;

    @Test
    void metricsControllerReturnsHttp200() throws Exception {
        when(metricsService.summary()).thenReturn(new MetricsSummaryResponse(
            new SuggestionMetrics(4, 2, 2, 0.5, 2),
            new SearchMetrics(10, 10),
            new BatchWriteMetrics(3, 20, 5, 15),
            new TrendingMetrics(2),
            new DatabaseMetrics(100000, 4369342, 3, 3),
            "application"
        ));

        mockMvc.perform(get("/metrics/summary"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "suggestions": {
                    "requests": 4,
                    "cacheHits": 2,
                    "cacheMisses": 2,
                    "cacheHitRate": 0.5,
                    "postgresReads": 2
                  },
                  "searches": {
                    "accepted": 10,
                    "queued": 10
                  },
                  "batchWrites": {
                    "flushes": 3,
                    "rawEventsProcessed": 20,
                    "uniqueQueryWrites": 5,
                    "estimatedDbWritesAvoided": 15
                  },
                  "trending": {
                    "requests": 2
                  },
                  "database": {
                    "searchQueries": 100000,
                    "queryPrefixes": 4369342,
                    "activityBuckets": 3,
                    "batchAuditRows": 3
                  },
                  "source": "application"
                }
                """));
    }
}
