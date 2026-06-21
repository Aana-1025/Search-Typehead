package com.typeahead.dataset;

import java.nio.file.Path;
import java.time.Duration;

public record DatasetLoadSummary(
    Path datasetPath,
    int csvRowsRead,
    int searchQueriesInserted,
    long queryPrefixesInserted,
    int prefixMaxLengthUsed,
    Duration totalTimeTaken
) {
}
