package com.typeahead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.typeahead.suggest.SuggestionRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
class TypeaheadApplicationTests {

    private static final Path MIGRATION_FILE =
        Path.of("src", "main", "resources", "db", "migration", "V1__create_core_schema.sql");

    @MockBean
    private SuggestionRepository suggestionRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void healthEndpointReturnsExpectedPayload() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {"status":"UP","service":"search-typeahead-backend"}
                """));
    }

    @Test
    void coreSchemaMigrationExistsAndContainsExpectedTables() throws Exception {
        assertThatCode(() -> Files.exists(MIGRATION_FILE)).doesNotThrowAnyException();
        assertThat(Files.exists(MIGRATION_FILE)).isTrue();

        String migration = Files.readString(MIGRATION_FILE);

        assertThat(migration).contains("CREATE TABLE search_queries");
        assertThat(migration).contains("CREATE TABLE query_prefixes");
        assertThat(migration).contains("CREATE TABLE query_activity_buckets");
        assertThat(migration).contains("CREATE TABLE batch_flush_audit");
        assertThat(migration).contains("CREATE TABLE processed_kafka_offsets");
    }
}
