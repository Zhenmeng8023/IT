package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.enums.AiStructuredApplyTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiScenePostProcessorTest {

    private final AiScenePostProcessor processor = new AiScenePostProcessor(new ObjectMapper());

    @Test
    @SuppressWarnings("unchecked")
    void projectTaskActionBuildsStructuredTaskPayload() {
        String raw = """
                {
                  "summary": "execution plan",
                  "phases": [
                    {
                      "name": "phase-1",
                      "tasks": [
                        {"title":"build parser","goal":"extract fields","deliverable":"dto","priority":"P1","estimate":"1d"}
                      ]
                    }
                  ],
                  "executionOrder": ["phase-1", "phase-2"],
                  "risks": ["schema mismatch"]
                }
                """;

        AiScenePostProcessor.ProcessedAiResult result = processor.process("project.assistant", "project.detail.tasks", raw);

        assertThat(result.displayText()).isEqualTo("execution plan");
        assertThat(result.applyTargets()).contains(AiStructuredApplyTarget.PROJECT_DETAIL_TASKS);
        assertThat(result.structured()).containsKeys("phases", "tasks", "executionOrder", "risks");
        assertThat((List<Map<String, Object>>) result.structured().get("phases")).hasSize(1);
        assertThat((List<Map<String, Object>>) result.structured().get("tasks")).isNotEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageExplainSceneBuildsSectionStructure() {
        String raw = """
                {
                  "pageSummary": "this page displays order metrics",
                  "sections": [
                    {"title":"header","purpose":"show filters","dataSource":"query params","interaction":"change date"}
                  ],
                  "dataFlow": ["ui -> api -> chart"]
                }
                """;

        AiScenePostProcessor.ProcessedAiResult result = processor.process("page.explain", null, raw);

        assertThat(result.displayText()).contains("order metrics");
        assertThat(result.applyTargets()).contains(AiStructuredApplyTarget.PAGE_EXPLAIN);
        assertThat(result.structured()).containsKeys("pageSummary", "sections", "dataFlow", "userActions", "caveats");
        assertThat((List<Map<String, Object>>) result.structured().get("sections")).hasSize(1);
    }

    @Test
    void invalidStructuredInputFallsBackToTextWithoutThrowing() {
        String raw = "plain answer without json";

        AiScenePostProcessor.ProcessedAiResult result = processor.process("project.detail.risks", null, raw);

        assertThat(result.displayText()).isEqualTo(raw);
        assertThat(result.structured()).containsKey("risks");
    }
}
