package com.example.instagram.post.storage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * S3 연동 구현 전 단계에서 계약(예외)만 고정한다. 실제 SDK/LocalStack 통합은 후속으로 추가한다.
 */
class S3ImageStorageTest {

    @Test
    @DisplayName("S3 구현 전에는 store 호출 시 UnsupportedOperationException을 던진다")
    void notImplemented() {
        S3ImageStorage storage = new S3ImageStorage();
        MockMultipartFile file = new MockMultipartFile("f", "a.png", "image/png", new byte[] {1});

        assertThatThrownBy(() -> storage.store(1L, file))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
