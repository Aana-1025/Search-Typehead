package com.typeahead.suggest;

import java.util.List;

public interface SuggestionRepository {

    List<SuggestionItem> findSuggestions(String normalizedPrefix, int limit);
}
