package com.alikeyou.itmoduleblog.service;

public interface ViewLogService {

    boolean recordBlogView(Long blogId, Long userId, String ipAddress, String userAgent);
}
