package com.alikeyou.itmoduleblog.constant;

public class BlogConstant {
    // 博客状态
    public static final int BLOG_STATUS_DRAFT = 0; // 草稿
    public static final int BLOG_STATUS_PUBLISHED = 1; // 已发布
    public static final int BLOG_STATUS_DELETED = 2; // 已删除

    // 评论状态
    public static final int COMMENT_STATUS_PENDING = 0; // 待审核
    public static final int COMMENT_STATUS_APPROVED = 1; // 已审核
    public static final int COMMENT_STATUS_REJECTED = 2; // 已拒绝
}