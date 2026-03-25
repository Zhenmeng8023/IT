package com.alikeyou.itmoduleproject.support;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    com.alikeyou.itmoduleproject.support.StoredFileInfo store(Long projectId, String subFolder, MultipartFile multipartFile);

    Resource loadAsResource(String storedPath);
}
