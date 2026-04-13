package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.KnowledgeChunkPreviewRequest;
import com.alikeyou.itmoduleai.dto.response.KnowledgeChunkPreviewResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkingServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KnowledgeChunkingServiceImpl service = new KnowledgeChunkingServiceImpl(objectMapper);

    @Test
    void chunksVueByTopLevelBlocksAndScriptSymbols() throws Exception {
        KnowledgeDocument document = document("src/components/TicketPanel.vue", "vue");
        KnowledgeChunkPreviewRequest request = request();
        String source = """
                <template>
                  <div>{{ total }}</div>
                </template>
                <script setup lang="ts">
                import { computed, watch } from 'vue'
                const total = computed(() => 1)
                watch(total, () => console.log(total.value))
                function submitTicket() {
                  return total.value
                }
                </script>
                <style scoped>
                .panel { color: red; }
                </style>
                """;

        List<KnowledgeChunkPreviewResponse> chunks = service.previewChunks(null, document, source, request);
        List<Map<String, Object>> metadata = metadata(chunks);

        assertThat(metadata).extracting(item -> item.get("sectionName"))
                .contains("template", "script", "style");
        assertThat(metadata).extracting(item -> item.get("language"))
                .contains("vue", "typescript");
        assertThat(metadata).extracting(item -> item.get("symbolName"))
                .contains("imports", "total", "submitTicket");
        assertThat(metadata).allSatisfy(item -> {
            assertThat(item.get("path")).isEqualTo("src/components/TicketPanel.vue");
            assertThat(item.get("startLine")).isNotNull();
            assertThat(item.get("endLine")).isNotNull();
        });
    }

    @Test
    void chunksJavaByTypeAndMethods() throws Exception {
        KnowledgeDocument document = document("src/main/java/demo/TicketService.java", "java");
        String source = """
                package demo;

                public class TicketService {
                    public void createTicket() {
                        validate();
                    }

                    private boolean validate() {
                        return true;
                    }
                }
                """;

        List<KnowledgeChunkPreviewResponse> chunks = service.previewChunks(null, document, source, request());
        List<Map<String, Object>> metadata = metadata(chunks);

        assertThat(metadata).extracting(item -> item.get("symbolName"))
                .contains("TicketService", "createTicket", "validate");
        assertThat(metadata).extracting(item -> item.get("symbolType"))
                .contains("class", "method");
    }

    @Test
    void chunksSqlByStatements() throws Exception {
        KnowledgeDocument document = document("schema/init.sql", "sql");
        String source = """
                create table ticket (
                  id bigint primary key
                );
                insert into ticket(id) values (1);
                select * from ticket;
                """;

        List<KnowledgeChunkPreviewResponse> chunks = service.previewChunks(null, document, source, request());
        List<Map<String, Object>> metadata = metadata(chunks);

        assertThat(chunks).hasSize(3);
        assertThat(metadata).extracting(item -> item.get("symbolType"))
                .contains("sql-create", "sql-insert", "sql-select");
    }

    @Test
    void buildsJavaDeclarationAndReferenceDrafts() {
        KnowledgeDocument document = document("src/main/java/demo/TicketController.java", "java");
        String source = """
                package demo;
                import java.util.List;
                import org.springframework.web.bind.annotation.GetMapping;

                public class TicketController {
                    @GetMapping("/tickets")
                    public List<String> listTickets() {
                        return loadTickets();
                    }

                    private List<String> loadTickets() {
                        return List.of("a");
                    }
                }
                """;

        var draft = service.buildIndexDraft(null, document, source, request());

        assertThat(draft.symbols()).extracting(item -> item.symbolName())
                .contains("TicketController", "listTickets", "loadTickets");
        assertThat(draft.references()).extracting(item -> item.refKind())
                .contains("IMPORT", "CALL", "ROUTE");
    }

    @Test
    void buildsVueAndSqlUseReferenceDrafts() {
        KnowledgeDocument document = document("src/views/OrderView.vue", "vue");
        String source = """
                <template>
                  <router-link to="/orders">Orders</router-link>
                </template>
                <script setup lang="ts">
                import axios from 'axios'
                import { useRouter } from 'vue-router'
                const router = useRouter()
                const sql = "select * from orders"
                async function loadOrders() {
                  await axios.get("https://api.example.com/orders")
                  router.push('/orders/detail')
                }
                </script>
                """;

        var draft = service.buildIndexDraft(null, document, source, request());

        assertThat(draft.symbols()).extracting(item -> item.symbolName())
                .contains("loadOrders");
        assertThat(draft.references()).extracting(item -> item.refKind())
                .contains("IMPORT", "API", "ROUTE", "CALL", "SQL_USE");
    }

    @Test
    void chunksMarkdownByHeadingsAndPreservesSectionPath() throws Exception {
        KnowledgeDocument document = document("docs/flow.md", "markdown");
        KnowledgeChunkPreviewRequest request = request();
        request.setChunkStrategy("MARKDOWN");
        String source = """
                # 业务链路
                ## 页面数据从哪里来
                页面通过接口拉取订单列表。
                ## 状态在哪里流转
                状态在提交后从 draft 变为 submitted。
                """;

        List<KnowledgeChunkPreviewResponse> chunks = service.previewChunks(null, document, source, request);
        List<Map<String, Object>> metadata = metadata(chunks);

        assertThat(chunks).isNotEmpty();
        assertThat(metadata).extracting(item -> item.get("sectionName"))
                .contains("# 业务链路", "## 页面数据从哪里来", "## 状态在哪里流转");
        assertThat(metadata).extracting(item -> item.get("path"))
                .allMatch(path -> "docs/flow.md".equals(path));
    }

    private KnowledgeChunkPreviewRequest request() {
        KnowledgeChunkPreviewRequest request = new KnowledgeChunkPreviewRequest();
        request.setChunkStrategy("CODE");
        request.setMaxChars(800);
        request.setOverlapChars(0);
        return request;
    }

    private KnowledgeDocument document(String path, String language) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(1L);
        document.setTitle(path);
        document.setFileName(path.substring(path.lastIndexOf('/') + 1));
        document.setArchiveEntryPath(path);
        document.setLanguage(language);
        return document;
    }

    private List<Map<String, Object>> metadata(List<KnowledgeChunkPreviewResponse> chunks) {
        return chunks.stream()
                .map(chunk -> {
                    try {
                        return objectMapper.readValue(chunk.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
                    } catch (Exception ex) {
                        throw new AssertionError(ex);
                    }
                })
                .toList();
    }
}
