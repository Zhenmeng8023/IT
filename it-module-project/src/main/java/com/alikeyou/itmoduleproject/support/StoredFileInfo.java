package com.alikeyou.itmoduleproject.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoredFileInfo {
    private String originalFilename;
    private String storedPath;
    private String extension;
    private long size;
}
