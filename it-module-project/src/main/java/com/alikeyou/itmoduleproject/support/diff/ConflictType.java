package com.alikeyou.itmoduleproject.support.diff;

import java.util.Locale;

/**
 * Shared conflict taxonomy for advanced merge and workspace conflict handling.
 */
public enum ConflictType {
    CONTENT_CONFLICT,
    DELETE_MODIFY_CONFLICT,
    RENAME_CONFLICT,
    MOVE_CONFLICT,
    TARGET_PATH_OCCUPIED,
    MISSING_BASE,
    STALE_BRANCH;

    public static ConflictType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ConflictType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
