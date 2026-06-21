package com.typeahead.suggest;

import java.util.List;

public record SuggestionResponse(
    String prefix,
    int count,
    List<SuggestionItem> suggestions,
    String source
) {
}
