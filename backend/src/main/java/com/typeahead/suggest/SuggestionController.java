package com.typeahead.suggest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/suggest")
    public SuggestionResponse suggest(@RequestParam(name = "q", required = false) String queryPrefix) {
        return suggestionService.suggest(queryPrefix);
    }
}
