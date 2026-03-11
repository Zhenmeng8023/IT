package com.alikeyou.itmoduleblog.utils;

public class BlogUtil {
    // 生成博客的唯一标识
    public static String generateBlogId() {
        return "blog_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    // 生成评论的唯一标识
    public static String generateCommentId() {
        return "comment_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
}