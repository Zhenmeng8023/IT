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
            return saveBytes(file.getBytes(), file.getOriginalFilename(),
                    StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream");
        } catch (Exception e) {
            throw new BusinessException("读取上传文件失败：" + e.getMessage());
        }
    }

    public ProjectBlob saveExistingFile(String storedPath, String originalFilename) {
        if (!StringUtils.hasText(storedPath)) {
            throw new BusinessException("历史项目文件路径不存在，无法接入仓库");
        }
        try {
            Path source = Paths.get(storedPath);
            if (!Files.exists(source) || !Files.isReadable(source)) {
                throw new BusinessException("历史项目文件不存在或不可读：" + storedPath);
            }
            byte[] bytes = Files.readAllBytes(source);
            String mimeType = Files.probeContentType(source);
            return saveBytes(bytes, originalFilename, StringUtils.hasText(mimeType) ? mimeType : "application/octet-stream");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("历史项目文件接入仓库失败：" + e.getMessage());
        }
    }

    private ProjectBlob saveBytes(byte[] bytes, String originalFilename, String mimeType) throws Exception {
        String sha256 = sha256(bytes);
        long size = bytes.length;
        return projectBlobRepository.findBySha256AndSizeBytes(sha256, size)
                .orElseGet(() -> {
                    try {
                        String ext = extension(originalFilename);
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
                                .mimeType(StringUtils.hasText(mimeType) ? mimeType : "application/octet-stream")
                                .storagePath(target.toString())
                                .build());
                    } catch (Exception e) {
                        throw new BusinessException("保存文件内容失败：" + e.getMessage());
                    }
                });
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
