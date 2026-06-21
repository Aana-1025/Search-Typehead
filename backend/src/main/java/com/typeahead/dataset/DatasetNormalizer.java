package com.typeahead.dataset;

public final class DatasetNormalizer {

    private DatasetNormalizer() {
    }

    public static String normalizeQuery(String query) {
        return query == null ? "" : String.join(" ", query.trim().toLowerCase().split("\\s+"));
    }
}
