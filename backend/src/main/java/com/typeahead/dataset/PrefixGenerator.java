package com.typeahead.dataset;

import java.util.ArrayList;
import java.util.List;

public final class PrefixGenerator {

    private PrefixGenerator() {
    }

    public static List<String> generatePrefixes(String normalizedQuery, int prefixMaxLength) {
        String safeQuery = normalizedQuery == null ? "" : normalizedQuery;
        int prefixLimit = Math.min(safeQuery.length(), prefixMaxLength);
        List<String> prefixes = new ArrayList<>(prefixLimit);
        for (int length = 1; length <= prefixLimit; length++) {
            prefixes.add(safeQuery.substring(0, length));
        }
        return prefixes;
    }
}
