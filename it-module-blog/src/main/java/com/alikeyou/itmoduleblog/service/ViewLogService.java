package com.alikeyou.itmoduleblog.service;

public interface ViewLogService {

    void recordBlogView(Long blogId, Long userId, String ipAddress, String userAgent);
}
