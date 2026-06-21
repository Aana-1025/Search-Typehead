package com.typeahead.trending;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrendingController {

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @GetMapping("/trending")
    public TrendingResponse trending(
        @RequestParam(name = "window", required = false) String window,
        @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return trendingService.trending(window, limit);
    }
}
