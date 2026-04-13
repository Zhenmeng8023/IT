package com.alikeyou.itmoduleai.migration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class It10V8MigrationValidationTest {

    @Test
    void sqlContainsRequiredPostMigrationChecks() throws Exception {
        Path sql = Path.of("..", "sql", "IT10_v8.sql").normalize();
        String script = Files.readString(sql, StandardCharsets.UTF_8);

        assertThat(script).contains("v8_new_tables");
        assertThat(script).contains("v8_required_columns_missing");
        assertThat(script).contains("v8_required_indexes_missing");
        assertThat(script).contains("v8_required_foreign_keys_missing");
        assertThat(script).contains("v8_retrieval_log_invalid_symbol_refs");
        assertThat(script).contains("v8_stream_columns_null_count");
        assertThat(script).contains("v8_message_stream_state_values");
    }
}
