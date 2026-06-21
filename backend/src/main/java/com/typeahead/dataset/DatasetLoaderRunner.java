package com.typeahead.dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.dataset", name = "load", havingValue = "true")
public class DatasetLoaderRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetLoaderRunner.class);

    private final DatasetLoadingService datasetLoadingService;
    private final DatasetProperties datasetProperties;

    public DatasetLoaderRunner(DatasetLoadingService datasetLoadingService, DatasetProperties datasetProperties) {
        this.datasetLoadingService = datasetLoadingService;
        this.datasetProperties = datasetProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LOGGER.info("Starting dataset load from {}", datasetProperties.path());
        DatasetLoadSummary summary = datasetLoadingService.loadDataset(datasetProperties);
        LOGGER.info("Dataset path: {}", summary.datasetPath());
        LOGGER.info("CSV rows read: {}", summary.csvRowsRead());
        LOGGER.info("search_queries rows inserted: {}", summary.searchQueriesInserted());
        LOGGER.info("query_prefixes rows inserted: {}", summary.queryPrefixesInserted());
        LOGGER.info("Prefix max length used: {}", summary.prefixMaxLengthUsed());
        LOGGER.info("Total time taken: {} ms", summary.totalTimeTaken().toMillis());
    }
}
