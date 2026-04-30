package com.example.instagram.post.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {

    String store(Long userId, MultipartFile file);

    void deleteIfExists(String storageKey);
}
