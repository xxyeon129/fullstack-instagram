package com.example.instagram.post.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 등 객체 스토리지 연동 시 구현체를 등록한다.
 * {@code app.storage.type=s3} 일 때 활성화할 수 있도록 두고, 실제 AWS SDK 연동은 후속 작업으로 남긴다.
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3ImageStorage implements ImageStorage {

    @Override
    public String store(Long userId, MultipartFile file) {
        throw new UnsupportedOperationException("S3 storage is not configured yet.");
    }

    @Override
    public void deleteIfExists(String storageKey) {
        throw new UnsupportedOperationException("S3 storage is not configured yet.");
    }
}
