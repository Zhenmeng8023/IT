package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.entity.ViewLog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleblog.repository.ViewLogRepository;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleComment;
import com.alikeyou.itmodulecircle.repository.CircleCommentRepository;
import com.alikeyou.itmodulecircle.repository.CircleRepository;
import com.alikeyou.itmodulecommon.entity.UserBehavior;
import com.alikeyou.itmodulecommon.repository.UserBehaviorRepository;
import com.alikeyou.itmoduleinteractive.dto.UserActivityHeatmapDayDTO;
import com.alikeyou.itmoduleinteractive.dto.UserActivityHeatmapResponseDTO;
import com.alikeyou.itmoduleinteractive.entity.CollectRecord;
import com.alikeyou.itmoduleinteractive.entity.Comment;
import com.alikeyou.itmoduleinteractive.entity.LikeRecord;
import com.alikeyou.itmoduleinteractive.repository.CollectRecordRepository;
import com.alikeyou.itmoduleinteractive.repository.CommentRepository;
import com.alikeyou.itmoduleinteractive.repository.LikeRecordRepository;
import com.alikeyou.itmoduleinteractive.service.UserActivityService;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class UserActivityServiceImpl implements UserActivityService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 180;
    private static final Map<String, Integer> HEATMAP_WEIGHTS = Map.of(
            "commits", 8,
            "blogs", 4,
            "posts", 1,
            "likes", 1,
            "collects", 1,
            "logs", 1
    );
    private static final Map<String, Integer> HEATMAP_CAPS = Map.of(
            "commits", 32,
            "blogs", 8,
            "posts", 2,
            "likes", 1,
            "collects", 1,
            "logs", 1
    );

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private ViewLogRepository viewLogRepository;

    @Autowired
    private CircleRepository circleRepository;

    @Autowired
    private CircleCommentRepository circleCommentRepository;

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Autowired
    private CollectRecordRepository collectRecordRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ProjectCommitRepository projectCommitRepository;

    @Autowired
    private UserBehaviorRepository userBehaviorRepository;

    @Override
    public long getUserReceivedLikes(Long userId) {
        long blogLikes = blogRepository.findByAuthorId(userId).stream()
                .map(Blog::getLikeCount)
                .filter(value -> value != null && value > 0)
                .mapToLong(Integer::longValue)
                .sum();

        long circleLikes = circleCommentRepository.findByAuthorIdOrderByCreatedAtDesc(userId).stream()
                .map(CircleComment::getLikes)
                .filter(value -> value != null && value > 0)
                .mapToLong(Integer::longValue)
                .sum();

        long commentLikes = commentRepository.findByAuthorIdOrderByCreatedAtDesc(userId).stream()
                .map(Comment::getLikes)
                .filter(value -> value != null && value > 0)
                .mapToLong(Integer::longValue)
                .sum();

        return blogLikes + circleLikes + commentLikes;
    }

    @Override
    public long getUserCollectCount(Long userId) {
        return collectRecordRepository.countByUserId(userId);
    }

    @Override
    public long getUserHistoryCount(Long userId) {
        return viewLogRepository.countByUserId(userId);
    }

    @Override
    public UserActivityHeatmapResponseDTO getUserActivityHeatmap(Long userId, int days) {
        int normalizedDays = normalizeDays(days);
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate endDate = LocalDate.now(zoneId);
        LocalDate startDate = endDate.minusDays(normalizedDays - 1L);
        Instant startInstant = startDate.atStartOfDay(zoneId).toInstant();
        Instant endExclusive = endDate.plusDays(1L).atStartOfDay(zoneId).toInstant();
        LocalDateTime commitStart = startDate.atStartOfDay();
        LocalDateTime commitEndExclusive = endDate.plusDays(1L).atStartOfDay();

        Map<LocalDate, Map<String, Integer>> perDayBreakdown = new LinkedHashMap<>();
        Map<String, Integer> summary = createSummaryMap();

        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1L)) {
            perDayBreakdown.put(cursor, createSummaryMap());
        }

        Map<String, Boolean> behaviorCovered = addRecordedBehaviors(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        if (!behaviorCovered.getOrDefault("commits", false)) {
            addCommits(userId, commitStart, commitEndExclusive, perDayBreakdown, summary);
        }
        if (!behaviorCovered.getOrDefault("blogs", false)) {
            addBlogs(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        }
        if (!behaviorCovered.getOrDefault("posts", false)) {
            addCircles(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
            addCircleComments(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
            addBlogComments(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        }
        if (!behaviorCovered.getOrDefault("likes", false)) {
            addLikes(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        }
        if (!behaviorCovered.getOrDefault("collects", false)) {
            addCollects(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        }
        if (!behaviorCovered.getOrDefault("logs", false)) {
            addViews(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        }

        UserActivityHeatmapResponseDTO response = new UserActivityHeatmapResponseDTO();
        response.setUserId(userId);
        response.setDays(normalizedDays);
        response.setStartDate(startDate.format(DATE_FORMATTER));
        response.setEndDate(endDate.format(DATE_FORMATTER));
        response.setSummary(summary);

        int total = 0;
        int busiestCount = 0;
        String busiestDate = null;

        for (Map.Entry<LocalDate, Map<String, Integer>> entry : perDayBreakdown.entrySet()) {
            int dayTotal = calculateWeightedScore(entry.getValue());
            UserActivityHeatmapDayDTO dayDTO = new UserActivityHeatmapDayDTO();
            dayDTO.setDate(entry.getKey().format(DATE_FORMATTER));
            dayDTO.setCount(dayTotal);
            dayDTO.setBreakdown(entry.getValue());
            response.getActivities().add(dayDTO);

            total += dayTotal;
            if (dayTotal > busiestCount) {
                busiestCount = dayTotal;
                busiestDate = dayDTO.getDate();
            }
        }

        response.setTotalCount(total);
        response.setBusiestCount(busiestCount);
        response.setBusiestDate(busiestDate);
        return response;
    }

    private int normalizeDays(int days) {
        if (days <= 0) {
            return DEFAULT_DAYS;
        }
        return Math.min(days, MAX_DAYS);
    }

    private Map<String, Integer> createSummaryMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("commits", 0);
        map.put("blogs", 0);
        map.put("posts", 0);
        map.put("likes", 0);
        map.put("collects", 0);
        map.put("logs", 0);
        return map;
    }

    private Map<String, Boolean> addRecordedBehaviors(Long userId, Instant start, Instant end, ZoneId zoneId,
                                                      Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                                                      Map<String, Integer> summary) {
        Map<String, Boolean> covered = new LinkedHashMap<>();
        createSummaryMap().keySet().forEach(key -> covered.put(key, false));

        List<UserBehavior> behaviors = userBehaviorRepository.findByUser_IdAndOccurredAtBetween(userId, start, end);
        for (UserBehavior behavior : behaviors) {
            String key = classifyBehavior(behavior);
            Instant occurredAt = behavior.getOccurredAt();
            if (key == null || occurredAt == null) {
                continue;
            }
            increment(key, occurredAt, zoneId, perDayBreakdown, summary);
            covered.put(key, true);
        }
        return covered;
    }

    private String classifyBehavior(UserBehavior behavior) {
        if (behavior == null) {
            return null;
        }

        String behaviorType = normalizeText(behavior.getBehaviorType()).toLowerCase();
        String targetType = normalizeText(behavior.getTargetType()).toLowerCase();
        String text = behaviorType + " " + targetType;

        if (containsAny(text, "commit", "git", "代码提交", "提交代码")) {
            return "commits";
        }
        if (containsAny(text, "like", "点赞")) {
            return "likes";
        }
        if (containsAny(text, "collect", "favorite", "bookmark", "star", "收藏")) {
            return "collects";
        }
        if (containsAny(text, "view", "read", "browse", "login", "search", "download", "浏览", "阅读", "登录", "搜索", "下载")) {
            return "logs";
        }
        if (containsAny(text, "circle", "post", "comment", "reply", "圈子", "帖子", "评论", "回复")) {
            return "posts";
        }
        if (containsAny(text, "blog", "article", "博客", "文章")) {
            return "blogs";
        }

        return "logs";
    }

    private boolean containsAny(String source, String... keywords) {
        if (source == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void addCommits(Long userId, LocalDateTime start, LocalDateTime end,
                            Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                            Map<String, Integer> summary) {
        List<ProjectCommit> commits = projectCommitRepository.findByOperatorIdAndCreatedAtBetween(userId, start, end);
        for (ProjectCommit commit : commits) {
            increment("commits", commit.getCreatedAt(), perDayBreakdown, summary);
        }
    }

    private void addBlogs(Long userId, Instant start, Instant end, ZoneId zoneId,
                          Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                          Map<String, Integer> summary) {
        List<Blog> blogs = blogRepository.findByAuthorId(userId);
        for (Blog blog : blogs) {
            Instant activeInstant = resolveBlogActivityInstant(blog);
            if (!isTrackableBlogActivity(blog) || activeInstant == null) {
                continue;
            }
            if (activeInstant.isBefore(start) || !activeInstant.isBefore(end)) {
                continue;
            }
            increment("blogs", activeInstant, zoneId, perDayBreakdown, summary);
        }
    }

    private void addCircles(Long userId, Instant start, Instant end, ZoneId zoneId,
                            Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                            Map<String, Integer> summary) {
        List<Circle> circles = circleRepository.findByCreatorIdAndCreatedAtBetween(userId, start, end);
        for (Circle circle : circles) {
            increment("posts", circle.getCreatedAt(), zoneId, perDayBreakdown, summary);
        }
    }

    private void addCircleComments(Long userId, Instant start, Instant end, ZoneId zoneId,
                                   Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                                   Map<String, Integer> summary) {
        List<CircleComment> comments = circleCommentRepository.findByAuthorIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, start, end);
        for (CircleComment item : comments) {
            increment("posts", item.getCreatedAt(), zoneId, perDayBreakdown, summary);
        }
    }

    private void addBlogComments(Long userId, Instant start, Instant end, ZoneId zoneId,
                                 Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                                 Map<String, Integer> summary) {
        List<Comment> comments = commentRepository.findByAuthorIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, start, end);
        for (Comment comment : comments) {
            increment("posts", comment.getCreatedAt(), zoneId, perDayBreakdown, summary);
        }
    }

    private void addLikes(Long userId, Instant start, Instant end, ZoneId zoneId,
                          Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                          Map<String, Integer> summary) {
        List<LikeRecord> records = likeRecordRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
        for (LikeRecord record : records) {
            increment("likes", record.getCreatedAt(), zoneId, perDayBreakdown, summary);
        }
    }

    private void addCollects(Long userId, Instant start, Instant end, ZoneId zoneId,
                             Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                             Map<String, Integer> summary) {
        List<CollectRecord> records = collectRecordRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
        for (CollectRecord record : records) {
            increment("collects", record.getCreatedAt(), zoneId, perDayBreakdown, summary);
        }
    }

    private void addViews(Long userId, Instant start, Instant end, ZoneId zoneId,
                          Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                          Map<String, Integer> summary) {
        List<ViewLog> logs = viewLogRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
        for (ViewLog log : logs) {
            increment("logs", log.getCreatedAt(), zoneId, perDayBreakdown, summary);
        }
    }

    private int calculateWeightedScore(Map<String, Integer> breakdown) {
        int score = 0;
        for (Map.Entry<String, Integer> entry : breakdown.entrySet()) {
            int weight = HEATMAP_WEIGHTS.getOrDefault(entry.getKey(), 1);
            int cap = HEATMAP_CAPS.getOrDefault(entry.getKey(), weight);
            score += Math.min(entry.getValue() * weight, cap);
        }
        return score;
    }

    private void increment(String key, Instant instant, ZoneId zoneId,
                           Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                           Map<String, Integer> summary) {
        if (instant == null) {
            return;
        }
        LocalDate date = instant.atZone(zoneId).toLocalDate();
        Map<String, Integer> dayBreakdown = perDayBreakdown.get(date);
        if (dayBreakdown == null) {
            return;
        }
        dayBreakdown.put(key, dayBreakdown.getOrDefault(key, 0) + 1);
        summary.put(key, summary.getOrDefault(key, 0) + 1);
    }

    private void increment(String key, LocalDateTime createdAt,
                           Map<LocalDate, Map<String, Integer>> perDayBreakdown,
                           Map<String, Integer> summary) {
        if (createdAt == null) {
            return;
        }
        Map<String, Integer> dayBreakdown = perDayBreakdown.get(createdAt.toLocalDate());
        if (dayBreakdown == null) {
            return;
        }
        dayBreakdown.put(key, dayBreakdown.getOrDefault(key, 0) + 1);
        summary.put(key, summary.getOrDefault(key, 0) + 1);
    }

    private Instant resolveBlogActivityInstant(Blog blog) {
        if (blog == null) {
            return null;
        }
        if (blog.getPublishTime() != null) {
            return blog.getPublishTime();
        }
        if (blog.getUpdatedAt() != null) {
            return blog.getUpdatedAt();
        }
        return blog.getCreatedAt();
    }

    private boolean isTrackableBlogActivity(Blog blog) {
        if (blog == null) {
            return false;
        }

        String status = normalizeText(blog.getStatus()).toLowerCase();
        if (!"published".equals(status)
                && !"pending".equals(status)
                && !"rejected".equals(status)
                && !"approved".equals(status)
                && !"approve".equals(status)
                && !"public".equals(status)) {
            return false;
        }

        String title = normalizeText(blog.getTitle());
        String summary = stripHtml(blog.getSummary());
        String content = stripHtml(blog.getContent());
        return StringUtils.hasText(title) || StringUtils.hasText(summary) || StringUtils.hasText(content);
    }

    private boolean isMeaningfulText(String text, int minLength) {
        String normalized = normalizeText(text);
        if (normalized.length() < minLength) {
            return false;
        }
        if (normalized.matches("(?i)^(测试|test|demo|草稿|无标题|111+|aaa+|123+|qwe+)$")) {
            return false;
        }

        String denseText = normalized.replaceAll("\\s+", "");
        long uniqueChars = denseText.chars().distinct().count();
        return uniqueChars >= Math.min(6, denseText.length());
    }

    private String stripHtml(String text) {
        return normalizeText(text)
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }
}
