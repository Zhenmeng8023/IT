package com.alikeyou.itmoduleblog.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BlogAutoAuditService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile("(?is)<(script|style).*?>.*?</\\1>");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://|www\\.)", Pattern.CASE_INSENSITIVE);

    private final Set<String> rejectKeywords;
    private final Set<String> manualReviewKeywords;

    public BlogAutoAuditService() {
        this(
                List.of(
                        "代开发票",
                        "出售枪支",
                        "买卖枪支",
                        "出售毒品",
                        "买卖毒品",
                        "境外赌场",
                        "洗钱通道",
                        "招嫖",
                        "代考包过",
                        "出售考试答案"
                ),
                List.of(
                        "加微信",
                        "微信联系",
                        "联系方式",
                        "扫码进群",
                        "私聊我",
                        "群号",
                        "qq联系",
                        "telegram",
                        "whatsapp",
                        "博彩",
                        "刷单",
                        "返利",
                        "引流",
                        "代写",
                        "代考"
                )
        );
    }

    BlogAutoAuditService(Collection<String> rejectKeywords, Collection<String> manualReviewKeywords) {
        this.rejectKeywords = normalizeKeywords(rejectKeywords);
        this.manualReviewKeywords = normalizeKeywords(manualReviewKeywords);
    }

    public AuditDecision audit(String title, String summary, String content) {
        String plainContent = stripHtml(content);
        String mergedText = joinNonBlank(title, summary, plainContent);
        String normalizedText = normalizeText(mergedText);

        Set<String> rejectHits = collectKeywordHits(normalizedText, rejectKeywords);
        if (!rejectHits.isEmpty()) {
            String hitText = String.join("、", rejectHits);
            return AuditDecision.rejected(
                    new BigDecimal("95.00"),
                    "自动审核未通过，命中高风险词：" + hitText,
                    "请删除违规内容后再重新提交。"
            );
        }

        List<String> reasons = new ArrayList<>();
        Set<String> manualHits = collectKeywordHits(normalizedText, manualReviewKeywords);
        if (!manualHits.isEmpty()) {
            reasons.add("命中需人工复核词：" + String.join("、", manualHits));
        }

        if (countMatches(PHONE_PATTERN, mergedText) > 0) {
            reasons.add("正文包含手机号");
        }
        if (countMatches(EMAIL_PATTERN, mergedText) > 0) {
            reasons.add("正文包含邮箱地址");
        }
        if (countMatches(URL_PATTERN, mergedText) >= 3) {
            reasons.add("正文包含较多外部链接");
        }

        if (!reasons.isEmpty()) {
            return AuditDecision.pending(
                    new BigDecimal("62.00"),
                    "自动审核建议人工复核：" + String.join("；", reasons),
                    "请管理员重点检查导流、联系方式或营销信息。"
            );
        }

        return AuditDecision.approved(
                new BigDecimal("8.00"),
                "自动审核通过，未发现明显风险内容。",
                "可直接发布。"
        );
    }

    private Set<String> normalizeKeywords(Collection<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Set.of();
        }
        return keywords.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeText)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> collectKeywordHits(String text, Set<String> keywords) {
        if (!StringUtils.hasText(text) || keywords == null || keywords.isEmpty()) {
            return Set.of();
        }

        Set<String> hits = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                hits.add(keyword);
            }
        }
        return hits;
    }

    private int countMatches(Pattern pattern, String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String stripHtml(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String sanitized = SCRIPT_STYLE_PATTERN.matcher(content).replaceAll(" ");
        sanitized = sanitized.replaceAll("(?i)<br\\s*/?>", "\n");
        sanitized = sanitized.replaceAll("(?i)</p>", "\n");
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll(" ");
        sanitized = sanitized
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return sanitized.replaceAll("\\s+", " ").trim();
    }

    private String joinNonBlank(String... items) {
        return java.util.Arrays.stream(items)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining("\n"));
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text
                .toLowerCase(Locale.ROOT)
                .replace('\u3000', ' ')
                .replaceAll("\\s+", "");
    }

    public record AuditDecision(
            String blogStatus,
            String auditStatus,
            BigDecimal auditScore,
            String auditReason,
            String autoReviewSuggestion,
            boolean requiresManualReview
    ) {
        public static AuditDecision approved(BigDecimal score, String reason, String suggestion) {
            return new AuditDecision("published", "APPROVED", score, reason, suggestion, false);
        }

        public static AuditDecision pending(BigDecimal score, String reason, String suggestion) {
            return new AuditDecision("pending", "PENDING", score, reason, suggestion, true);
        }

        public static AuditDecision rejected(BigDecimal score, String reason, String suggestion) {
            return new AuditDecision("rejected", "REJECTED", score, reason, suggestion, false);
        }
    }
}
