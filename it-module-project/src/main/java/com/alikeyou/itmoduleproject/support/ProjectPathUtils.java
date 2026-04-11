package com.alikeyou.itmoduleproject.support;

import org.springframework.util.StringUtils;

public final class ProjectPathUtils {

    private ProjectPathUtils() {
    }

    public static String normalize(String path) {
        if (!StringUtils.hasText(path)) {
            throw new BusinessException("文件路径不能为空");
        }
        String value = path.trim().replace("\\", "/");
        while (value.contains("//")) {
            value = value.replace("//", "/");
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static String extractFileName(String canonicalPath) {
        String path = normalize(canonicalPath);
        int index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
    }
}
