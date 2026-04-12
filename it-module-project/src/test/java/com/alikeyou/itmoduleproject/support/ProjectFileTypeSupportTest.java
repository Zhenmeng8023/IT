package com.alikeyou.itmoduleproject.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectFileTypeSupportTest {

    @Test
    void resolve_shouldReturnExtensionForDocxPath() {
        assertEquals("docx", ProjectFileTypeSupport.resolve("/docs/需求说明.docx", null));
    }

    @Test
    void resolve_shouldReturnFolderForDirectoryPath() {
        assertEquals("folder", ProjectFileTypeSupport.resolve("/docs/", null));
    }

    @Test
    void resolve_shouldFallbackToBinWhenNoExtensionExists() {
        assertEquals("bin", ProjectFileTypeSupport.resolve("/docs/README", null));
    }
}
