package com.alikeyou.itmoduleproject.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

public final class ProjectFileTypeSupport {

    private ProjectFileTypeSupport() {
    }

    public static String resolve(String canonicalPath, String fallbackFilename) {
        if (isFolderPath(canonicalPath) || isFolderPath(fallbackFilename)) {
            return "folder";
        }
        String extension = resolveExtension(canonicalPath);
        if (!StringUtils.hasText(extension)) {
            extension = resolveExtension(fallbackFilename);
        }
        return StringUtils.hasText(extension) ? extension.trim().toLowerCase(Locale.ROOT) : "bin";
    }

    private static String resolveExtension(String pathOrName) {
        if (!StringUtils.hasText(pathOrName)) {
            return null;
        }
        String normalized = pathOrName.trim().replace('\\', '/');
        String fileName = StringUtils.getFilename(normalized);
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        return StringUtils.getFilenameExtension(fileName);
    }

    private static boolean isFolderPath(String pathOrName) {
        if (!StringUtils.hasText(pathOrName)) {
            return false;
        }
        String normalized = pathOrName.trim().replace('\\', '/');
        return "/".equals(normalized) || normalized.endsWith("/");
    }
}
