package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.entity.ViewLog;
import com.alikeyou.itmoduleblog.repository.ViewLogRepository;
import com.alikeyou.itmoduleblog.service.ViewLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class ViewLogServiceImpl implements ViewLogService {

    private static final String BLOG_TARGET_TYPE = "blog";

    @Autowired
    private ViewLogRepository viewLogRepository;

    @Override
    @Transactional
    public void recordBlogView(Long blogId, Long userId, String ipAddress, String userAgent) {
        if (blogId == null) {
            return;
        }

        ViewLog viewLog = new ViewLog();
        viewLog.setUserId(userId);
        viewLog.setTargetType(BLOG_TARGET_TYPE);
        viewLog.setTargetId(blogId);
        viewLog.setIpAddress(trimToNull(ipAddress, 45));
        viewLog.setUserAgent(trimToNull(userAgent, null));
        viewLog.setCreatedAt(Instant.now());
        viewLogRepository.save(viewLog);
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
