package com.alikeyou.itmoduleproject.support.diff;

import java.util.Locale;

/**
 * Shared semantic change types for workspace, commit, merge, and release diffs.
 */
public enum ChangeType {
    ADD,
    MODIFY,
    DELETE,
    RENAME,
    MOVE;

    public static ChangeType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ChangeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
