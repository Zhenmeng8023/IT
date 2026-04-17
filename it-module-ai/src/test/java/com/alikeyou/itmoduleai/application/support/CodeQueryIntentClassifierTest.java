package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeQueryIntentClassifierTest {

    private final CodeQueryIntentClassifier classifier = new CodeQueryIntentClassifier();

    @Test
    void actionCodeOverridesKeywordHeuristicInAutoMode() {
        AiAnalysisMode mode = classifier.classify(null, "code.locate", "how does payment flow", null);

        assertThat(mode).isEqualTo(AiAnalysisMode.CODE_LOCATE);
    }

    @Test
    void locateKeywordsRouteToCodeLocate() {
        AiAnalysisMode mode = classifier.classify(null, null,
                "\u5165\u53e3\u5b9a\u4e49\u5728\u54ea\u4e2a\u6587\u4ef6", null);

        assertThat(mode).isEqualTo(AiAnalysisMode.CODE_LOCATE);
    }

    @Test
    void logicKeywordsRouteToCodeLogic() {
        AiAnalysisMode mode = classifier.classify(null, null,
                "\u8fd9\u4e2a\u6a21\u5757\u8c03\u7528\u94fe\u548c\u6d41\u7a0b\u662f\u4ec0\u4e48", null);

        assertThat(mode).isEqualTo(AiAnalysisMode.CODE_LOGIC);
    }

    @Test
    void summaryKeywordsRouteToDocQa() {
        AiAnalysisMode mode = classifier.classify(null, null,
                "\u8bf7\u7ed9\u6211\u603b\u7ed3\u6458\u8981\u5e76\u6da6\u8272\u6807\u7b7e", null);

        assertThat(mode).isEqualTo(AiAnalysisMode.DOC_QA);
    }
}
