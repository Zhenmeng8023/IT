package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.entity.ViewLog;
import com.alikeyou.itmoduleblog.repository.ViewLogRepository;
import com.alikeyou.itmoduleblog.service.ViewLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

@Service
public class ViewLogServiceImpl implements ViewLogService {

    private static final String BLOG_TARGET_TYPE = "blog";
    private static final Duration DEDUP_WINDOW = Duration.ofMinutes(5);

    @Autowired
    private ViewLogRepository viewLogRepository;

    @Override
    @Transactional
    public boolean recordBlogView(Long blogId, Long userId, String ipAddress, String userAgent) {
        if (blogId == null) {
            return false;
        }

        Instant now = Instant.now();
        String normalizedIp = trimToNull(ipAddress, 45);
        String normalizedUserAgent = trimToNull(userAgent, 512);

        if (isDuplicateView(blogId, userId, normalizedIp, normalizedUserAgent, now)) {
            return false;
        }

        ViewLog viewLog = new ViewLog();
        viewLog.setUserId(userId);
        viewLog.setTargetType(BLOG_TARGET_TYPE);
        viewLog.setTargetId(blogId);
        viewLog.setIpAddress(normalizedIp);
        viewLog.setUserAgent(normalizedUserAgent);
        viewLog.setCreatedAt(now);
        viewLogRepository.save(viewLog);
        return true;
    }

    private boolean isDuplicateView(Long blogId, Long userId, String ipAddress, String userAgent, Instant now) {
        Instant threshold = now.minus(DEDUP_WINDOW);
        if (userId != null) {
            return viewLogRepository
                    .findTopByUserIdAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                            userId,
                            BLOG_TARGET_TYPE,
                            blogId,
                            threshold
                    )
                    .isPresent();
        }

        if (!StringUtils.hasText(ipAddress) || !StringUtils.hasText(userAgent)) {
            return false;
        }

        return viewLogRepository
                .findTopByUserIdIsNullAndTargetTypeAndTargetIdAndIpAddressAndUserAgentAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        BLOG_TARGET_TYPE,
                        blogId,
                        ipAddress,
                        userAgent,
                        threshold
                )
                .isPresent();
    }

    private String trimToNull(String value, Integer maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (maxLength != null && trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }
}
