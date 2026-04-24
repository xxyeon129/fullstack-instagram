package com.example.instagram.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.instagram.post.dto.PostResponse;
import com.example.instagram.post.repository.PostRepository;
import com.example.instagram.post.storage.LocalImageStorage;
import com.example.instagram.user.entity.User;
import com.example.instagram.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostService.class, LocalImageStorage.class})
@TestPropertySource(properties = {
        "app.storage.type=local",
        "app.storage.local.base-directory=${java.io.tmpdir}/instagram-post-service-test",
        "app.media.public-base-path=/api/v1/media"
})
@Transactional
class PostServiceIntegrationTest {

    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("멀티파트 이미지를 저장하고 게시물 메타데이터를 영속화한다")
    void create_persistsPost_andStoresFile() throws Exception {
        User author = userRepository.save(User.builder()
                .email("svc@example.com")
                .username("svcuser")
                .password("secret")
                .build());

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "upload.png",
                "image/png",
                "png-bytes".getBytes()
        );

        PostResponse response = postService.create(author.getId(), "caption", image);

        assertThat(response.authorId()).isEqualTo(author.getId());
        assertThat(response.imageUrl()).contains("/api/v1/media/");
        assertThat(postRepository.findById(response.id())).isPresent();
    }
}
