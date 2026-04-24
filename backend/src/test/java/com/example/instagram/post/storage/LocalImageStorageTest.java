package com.example.instagram.post.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.instagram.global.exception.CustomException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalImageStorageTest {

    @TempDir
    Path tempDir;

    private LocalImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalImageStorage(tempDir.toString());
    }

    @Test
    @DisplayName("이미지 파일을 사용자별 디렉터리에 저장하고 상대 키를 반환한다")
    void store_writesFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "image/png",
                "binary-content".getBytes()
        );

        String key = storage.store(42L, file);

        assertThat(key).startsWith("42/");
        Path physical = storage.resolvePhysicalPath(key);
        assertThat(Files.exists(physical)).isTrue();
        assertThat(Files.readString(physical)).isEqualTo("binary-content");
    }

    @Test
    @DisplayName("이미지가 아니면 INVALID_IMAGE로 거절한다")
    void reject_nonImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "x.txt",
                "text/plain",
                "hello".getBytes()
        );

        assertThatThrownBy(() -> storage.store(1L, file))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("경로 탈출 시도는 INVALID_IMAGE로 거절한다")
    void reject_pathTraversal() {
        assertThatThrownBy(() -> storage.resolvePhysicalPath("../secret"))
                .isInstanceOf(CustomException.class);
    }
}
