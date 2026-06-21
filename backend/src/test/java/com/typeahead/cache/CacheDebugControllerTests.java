package com.typeahead.cache;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CacheDebugController.class)
class CacheDebugControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SuggestionCacheService suggestionCacheService;

    @Test
    void returnsDebugPayload() throws Exception {
        when(suggestionCacheService.debug("iph", "count")).thenReturn(new CacheDebugResponse(
            true,
            "iph",
            "count",
            "suggest:v1:count:iph",
            "suggest:cache-node-b:v1:count:iph",
            "cache-node-b",
            List.of("cache-node-a", "cache-node-b", "cache-node-c"),
            "MISS",
            300
        ));

        mockMvc.perform(get("/cache/debug").param("prefix", "iph"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "enabled": true,
                  "prefix": "iph",
                  "mode": "count",
                  "cacheKey": "suggest:v1:count:iph",
                  "redisKey": "suggest:cache-node-b:v1:count:iph",
                  "ownerNode": "cache-node-b",
                  "logicalNodes": ["cache-node-a", "cache-node-b", "cache-node-c"],
                  "status": "MISS",
                  "ttlSeconds": 300
                }
                """));
    }
}
