package com.alikeyou.itmoduleproject.support;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ProjectPathUtils {

    private ProjectPathUtils() {
    }

    public static String normalize(String path) {
        if (!StringUtils.hasText(path)) {
            throw new BusinessException("文件路径不能为空");
        }
        String value = path.trim().replace("\\", "/");
        if (value.contains("://") || value.matches("^[a-zA-Z]:.*") || value.startsWith("~")) {
            throw new BusinessException("文件路径必须位于项目仓库根目录下");
        }

        List<String> segments = new ArrayList<>();
        for (String segment : value.split("/")) {
            if (!StringUtils.hasText(segment) || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                throw new BusinessException("文件路径不能越出项目仓库根目录");
            }
            segments.add(segment);
        }

        if (segments.isEmpty()) {
            return "/";
        }
        return "/" + String.join("/", segments);
    }

    public static String extractFileName(String canonicalPath) {
        String path = normalize(canonicalPath);
        int index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
    }
}
