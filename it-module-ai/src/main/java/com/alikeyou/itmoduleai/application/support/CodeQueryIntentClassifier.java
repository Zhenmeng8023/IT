package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class CodeQueryIntentClassifier {

    public AiAnalysisMode classify(AiAnalysisMode requestedMode, String question, String symbolHint) {
        if (requestedMode == AiAnalysisMode.DOC_QA
                || requestedMode == AiAnalysisMode.CODE_LOCATE
                || requestedMode == AiAnalysisMode.CODE_LOGIC) {
            return requestedMode;
        }
        if (requestedMode == AiAnalysisMode.CODE_ANALYSIS) {
            return AiAnalysisMode.CODE_LOGIC;
        }
        String text = normalize(question + " " + (symbolHint == null ? "" : symbolHint));
        if (!StringUtils.hasText(text)) {
            return AiAnalysisMode.DOC_QA;
        }
        if (containsAny(text, "怎么实现", "如何实现", "为什么", "调用链", "调用关系", "执行流程", "业务逻辑",
                "代码逻辑", "trace", "flow", "call graph", "call chain", "implementation", "how does")) {
            return AiAnalysisMode.CODE_LOGIC;
        }
        if (containsAny(text, "在哪", "哪里", "哪个文件", "文件位置", "定位", "入口文件", "定义位置",
                "where", "locate", "find file", "which file", "definition")) {
            return AiAnalysisMode.CODE_LOCATE;
        }
        return AiAnalysisMode.DOC_QA;
    }

    private boolean containsAny(String source, String... needles) {
        for (String needle : needles) {
            if (source.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace('\n', ' ').replace('\r', ' ').trim();
    }
}
