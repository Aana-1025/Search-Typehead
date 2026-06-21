package com.typeahead.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class DatasetCsvReaderTests {

    private final DatasetCsvReader datasetCsvReader = new DatasetCsvReader();

    @Test
    void normalizeQueryTrimsLowercasesAndCollapsesWhitespace() {
        assertThat(DatasetNormalizer.normalizeQuery("  Spring   Boot   Tutorial  "))
            .isEqualTo("spring boot tutorial");
    }

    @Test
    void generatePrefixesUsesNormalizedQueryStartUpToMaxLength() {
        assertThat(PrefixGenerator.generatePrefixes("iphone", 4))
            .containsExactly("i", "ip", "iph", "ipho");
    }

    @Test
    void readParsesAndValidatesTinyCsvSample() throws Exception {
        Path sampleCsv = Files.createTempFile("dataset-sample", ".csv");
        Files.writeString(sampleCsv, """
            query,count
            iPhone 15 Pro,120
            Spring   Boot Tutorial,85
            pizza near me,25
            """);

        List<CsvQueryRecord> records = datasetCsvReader.read(sampleCsv);

        assertThat(records).hasSize(3);
        assertThat(records.get(0)).isEqualTo(new CsvQueryRecord("iPhone 15 Pro", "iphone 15 pro", 120));
        assertThat(records.get(1).normalizedQuery()).isEqualTo("spring boot tutorial");
        assertThat(records.get(2).count()).isEqualTo(25);
    }
}
