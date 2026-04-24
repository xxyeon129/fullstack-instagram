package com.example.instagram.post.storage;

import com.example.instagram.global.exception.CustomException;
import com.example.instagram.global.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {

    private final Path baseDirectory;

    public LocalImageStorage(@Value("${app.storage.local.base-directory}") String baseDirectory) {
        this.baseDirectory = Path.of(baseDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String store(Long userId, MultipartFile file) {
        validateImage(file);
        String extension = resolveExtension(file);
        String objectName = UUID.randomUUID() + extension;
        Path userDir = baseDirectory.resolve(String.valueOf(userId));
        try {
            Files.createDirectories(userDir);
            Path destination = userDir.resolve(objectName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, destination);
            }
        } catch (IOException e) {
            log.warn("Failed to store image for user {}", userId, e);
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
        return userId + "/" + objectName;
    }

    @Override
    public void deleteIfExists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        Path path = resolvePhysicalPath(storageKey);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete image {}", storageKey, e);
        }
    }

    public Path resolvePhysicalPath(String storageKey) {
        Path relative = Path.of(storageKey).normalize();
        if (relative.startsWith("..")) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
        Path resolved = baseDirectory.resolve(relative).normalize();
        if (!resolved.startsWith(baseDirectory)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
        return resolved;
    }

    private static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
    }

    private static String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null) {
            return "";
        }
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            return "";
        }
        return original.substring(dot).toLowerCase(Locale.ROOT);
    }
}
