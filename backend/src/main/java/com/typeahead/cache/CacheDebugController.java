package com.typeahead.cache;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CacheDebugController {

    private final SuggestionCacheService suggestionCacheService;

    public CacheDebugController(SuggestionCacheService suggestionCacheService) {
        this.suggestionCacheService = suggestionCacheService;
    }

    @GetMapping("/cache/debug")
    public CacheDebugResponse debug(
        @RequestParam("prefix") String prefix,
        @RequestParam(name = "mode", defaultValue = CacheKeyFactory.MODE_COUNT) String mode
    ) {
        return suggestionCacheService.debug(prefix, mode);
    }
}
