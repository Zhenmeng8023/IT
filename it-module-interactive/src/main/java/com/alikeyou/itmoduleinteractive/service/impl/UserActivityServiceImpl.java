package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.entity.ViewLog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleblog.repository.ViewLogRepository;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleComment;
import com.alikeyou.itmodulecircle.repository.CircleCommentRepository;
import com.alikeyou.itmodulecircle.repository.CircleRepository;
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

        addCommits(userId, commitStart, commitEndExclusive, perDayBreakdown, summary);
        addBlogs(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        addCircles(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        addCircleComments(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        addBlogComments(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        addLikes(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        addCollects(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);
        addViews(userId, startInstant, endExclusive, zoneId, perDayBreakdown, summary);

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
            Instant publishInstant = resolvePublishedInstant(blog);
            if (!isValidPublishedBlog(blog) || publishInstant == null) {
                continue;
            }
            if (publishInstant.isBefore(start) || !publishInstant.isBefore(end)) {
                continue;
            }
            increment("blogs", publishInstant, zoneId, perDayBreakdown, summary);
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

    private Instant resolvePublishedInstant(Blog blog) {
        if (blog == null) {
            return null;
        }
        return blog.getPublishTime();
    }

    private boolean isValidPublishedBlog(Blog blog) {
        if (blog == null) {
            return false;
        }

        String status = normalizeText(blog.getStatus()).toLowerCase();
        if (!"published".equals(status) && !"approved".equals(status) && !"public".equals(status)) {
            return false;
        }

        String title = normalizeText(blog.getTitle());
        String summary = stripHtml(blog.getSummary());
        String content = stripHtml(blog.getContent());
        String totalText = (title + " " + summary + " " + content).trim();

        return isMeaningfulText(title, 6)
                && isMeaningfulText(totalText, 80)
                && (content.length() >= 120 || summary.length() >= 40);
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
