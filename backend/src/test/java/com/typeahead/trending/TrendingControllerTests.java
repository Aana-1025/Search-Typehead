package com.typeahead.trending;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(TrendingController.class)
class TrendingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrendingService trendingService;

    @Test
    void returnsDefaultTrendingPayload() throws Exception {
        when(trendingService.trending(null, null)).thenReturn(new TrendingResponse(
            "24h",
            1,
            List.of(new TrendingItem("iphone", 249948L, 620L, 620.0)),
            "postgres"
        ));

        mockMvc.perform(get("/trending"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "window":"24h",
                  "count":1,
                  "items":[
                    {
                      "query":"iphone",
                      "totalCount":249948,
                      "recentCount":620,
                      "score":620.0
                    }
                  ],
                  "source":"postgres"
                }
                """));
    }

    @Test
    void emptyTrendingListReturns200() throws Exception {
        when(trendingService.trending("24h", 10)).thenReturn(new TrendingResponse(
            "24h",
            0,
            List.of(),
            "postgres"
        ));

        mockMvc.perform(get("/trending").param("window", "24h").param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {"window":"24h","count":0,"items":[],"source":"postgres"}
                """));
    }

    @Test
    void invalidWindowReturns400() throws Exception {
        when(trendingService.trending("bad", 10))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "window must be one of 1h, 6h, 24h, 7d"));

        mockMvc.perform(get("/trending").param("window", "bad").param("limit", "10"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidLimitReturns400() throws Exception {
        when(trendingService.trending("24h", 0))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than or equal to 1"));

        mockMvc.perform(get("/trending").param("window", "24h").param("limit", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void customLimitWorks() throws Exception {
        when(trendingService.trending("1h", 2)).thenReturn(new TrendingResponse(
            "1h",
            2,
            List.of(
                new TrendingItem("iphone", 249948L, 50L, 50.0),
                new TrendingItem("ipad", 140000L, 40L, 40.0)
            ),
            "postgres"
        ));

        mockMvc.perform(get("/trending").param("window", "1h").param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {
                  "window":"1h",
                  "count":2,
                  "items":[
                    {"query":"iphone","totalCount":249948,"recentCount":50,"score":50.0},
                    {"query":"ipad","totalCount":140000,"recentCount":40,"score":40.0}
                  ],
                  "source":"postgres"
                }
                """));
    }
}
