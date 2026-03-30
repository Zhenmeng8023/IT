package com.alikeyou.itmoduleproject.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${project.storage.root:uploads/project}")
    private String storageRoot;

    @Override
    public StoredFileInfo store(Long projectId, String subFolder, MultipartFile multipartFile) {
        if (multipartFile == null) {
            throw new BusinessException("上传文件不能为空");
        }

        try {
            String originalFilename = normalizeOriginalFilename(multipartFile.getOriginalFilename());
            String extension = StringUtils.getFilenameExtension(originalFilename);
            String savedFileName = UUID.randomUUID() + (StringUtils.hasText(extension) ? "." + extension : "");

            Path folder = Paths.get(storageRoot, String.valueOf(projectId), subFolder, LocalDate.now().toString());
            Files.createDirectories(folder);

            Path target = folder.resolve(savedFileName);
            Files.copy(multipartFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return new StoredFileInfo(
                    originalFilename,
                    target.toString().replace('\\', '/'),
                    extension,
                    multipartFile.getSize()
            );
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(String storedPath) {
        return new FileSystemResource(storedPath);
    }

    @Override
    public void delete(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storedPath));
        } catch (IOException e) {
            throw new BusinessException("文件删除失败：" + e.getMessage());
        }
    }

    private String normalizeOriginalFilename(String originalFilename) {
        String value = Objects.toString(originalFilename, "").replace('\\', '/').trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (!StringUtils.hasText(value)) {
            return "unnamed-file";
        }
        return value;
    }
}
