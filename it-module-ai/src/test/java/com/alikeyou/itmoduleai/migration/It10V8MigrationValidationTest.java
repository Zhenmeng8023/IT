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

    @Test
    void frontAiPermissionSeedInjectsPermissionsThroughRoleMenu() throws Exception {
        Path sql = Path.of("..", "sql", "insert", "IT10_v8_insert.sql").normalize();
        String script = Files.readString(sql, StandardCharsets.UTF_8);

        assertThat(script).contains("view:front:ai:assistant");
        assertThat(script).contains("view:front:ai:kb:self");
        assertThat(script).contains("edit:front:ai:kb:self");
        assertThat(script).contains("view:front:ai:kb:project");
        assertThat(script).contains("manage:front:ai:kb:member");
        assertThat(script).contains("(435, '隐藏-前台-AI助手'");
        assertThat(script).contains("(440, '隐藏-前台-知识库成员管理'");
        assertThat(script).contains("(1, 315), (1, 316), (1, 317), (1, 318), (1, 319), (1, 320),");
        assertThat(script).contains("(1, 435), (1, 436), (1, 437), (1, 438), (1, 439), (1, 440), (1, 265),");
        assertThat(script).contains("(3, 435), (3, 436), (3, 438),");
        assertThat(script).contains("(4, 435), (4, 436), (4, 437), (4, 438);");
    }
}
