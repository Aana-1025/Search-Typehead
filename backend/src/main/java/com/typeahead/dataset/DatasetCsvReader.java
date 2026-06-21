package com.typeahead.dataset;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class DatasetCsvReader {

    public List<CsvQueryRecord> read(Path csvPath) throws IOException {
        List<CsvQueryRecord> records = new ArrayList<>();
        Set<String> normalizedQueries = new HashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String header = reader.readLine();
            if (!"query,count".equals(header)) {
                throw new IllegalArgumentException("CSV header must be exactly query,count");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    throw new IllegalArgumentException("CSV line " + lineNumber + " is blank");
                }

                int commaIndex = line.lastIndexOf(',');
                if (commaIndex <= 0 || commaIndex == line.length() - 1) {
                    throw new IllegalArgumentException("CSV line " + lineNumber + " must contain query,count");
                }

                String queryText = line.substring(0, commaIndex);
                String countText = line.substring(commaIndex + 1).trim();
                String normalizedQuery = DatasetNormalizer.normalizeQuery(queryText);
                if (normalizedQuery.isEmpty()) {
                    throw new IllegalArgumentException("CSV line " + lineNumber + " has an empty query");
                }

                long count;
                try {
                    count = Long.parseLong(countText);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                        "CSV line " + lineNumber + " has a non-integer count: " + countText,
                        exception
                    );
                }

                if (count <= 0) {
                    throw new IllegalArgumentException("CSV line " + lineNumber + " must have a positive count");
                }
                if (!normalizedQueries.add(normalizedQuery)) {
                    throw new IllegalArgumentException(
                        "CSV line " + lineNumber + " duplicates normalized query: " + normalizedQuery
                    );
                }

                records.add(new CsvQueryRecord(queryText.strip(), normalizedQuery, count));
            }
        }

        return records;
    }
}
