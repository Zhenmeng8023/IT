package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.repository.ProjectBlobRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class ProjectRepoStorageSupport {

    private final ProjectBlobRepository projectBlobRepository;

    public ProjectRepoStorageSupport(ProjectBlobRepository projectBlobRepository) {
        this.projectBlobRepository = projectBlobRepository;
    }

    public ProjectBlob saveMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        try {
            byte[] bytes = file.getBytes();
            String sha256 = sha256(bytes);
            long size = bytes.length;
            return projectBlobRepository.findBySha256AndSizeBytes(sha256, size)
                    .orElseGet(() -> {
                        try {
                            String ext = extension(file.getOriginalFilename());
                            Path root = Paths.get(System.getProperty("user.dir"), "upload", "project-repo", "blobs");
                            Path dir = root.resolve(sha256.substring(0, 2)).resolve(sha256.substring(2, 4));
                            Files.createDirectories(dir);
                            Path target = dir.resolve(sha256 + ext);
                            if (!Files.exists(target)) {
                                Files.write(target, bytes);
                            }
                            return projectBlobRepository.save(ProjectBlob.builder()
                                    .sha256(sha256)
                                    .sizeBytes(size)
                                    .mimeType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream")
                                    .storagePath(target.toString())
                                    .build());
                        } catch (Exception e) {
                            throw new BusinessException("保存文件内容失败：" + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            throw new BusinessException("读取上传文件失败：" + e.getMessage());
        }
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        return index >= 0 ? filename.substring(index) : "";
    }

    private String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}
