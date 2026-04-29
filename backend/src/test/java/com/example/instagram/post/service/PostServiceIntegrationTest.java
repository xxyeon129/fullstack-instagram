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
    @DisplayName("게시물을 생성한다")
    void create_post_success() throws Exception {
        // given
        User author = userRepository.save(User.builder().email("test123@test.com").username("test").password("testpw").build());
        MockMultipartFile testImage = new MockMultipartFile("image", "testimg.png", "image/png", "png-bytes".getBytes());

        // when
        PostResponse response = postService.create(author.getId(), "testCaption", testImage);

        // then
        assertThat(postRepository.findById(response.id())).isPresent();
    }

//    @Test
//    @DisplayName("멀티파트 이미지를 저장하고 게시물 메타데이터를 영속화한다")
//    void create_persistsPost_andStoresFile() throws Exception {
//        User author = userRepository.save(User.builder()
//                .email("svc@example.com")
//                .username("svcuser")
//                .password("secret")
//                .build());
//
//        MockMultipartFile image = new MockMultipartFile(
//                "image",
//                "upload.png",
//                "image/png",
//                "png-bytes".getBytes()
//        );
//
//        PostResponse response = postService.create(author.getId(), "caption", image);
//
//        assertThat(response.authorId()).isEqualTo(author.getId());
//        assertThat(response.imageUrl()).contains("/api/v1/media/");
//        assertThat(postRepository.findById(response.id())).isPresent();
//    }

    @Test
    @DisplayName("게시물을 조회한다")
    void getById_success() throws Exception {
        // given
        User author = userRepository.save(User.builder().email("test123@test.com").userName("test").password("testpw").build());
        Post post = postRepository.save(Post.builder().user(author).caption("testCaption").imageStorageKey(author.getId() + "/saved.png").build());

        // when
        PostResponse response = postService.getById(post.getId());

        // then
        assertThat(response.id()).isEqualTo(savedPost.getId());
    }

    @Test
    @DisplayName("존재하지 않는 게시물은 예외가 발생한다")
    void getById_not_found() throws Exception {

    }

    @Test
    @DisplayName("게시물을 수정한다")
    void update_success () throws Exception {
        // given
        User author = userRepository.save(User.builder().email("test123@test.com").userName("test").password("testpw").build());
        Post post = postRepository.save(Post.builder().user(author).caption("testCaption").imageStorageKey(author.getId() + "/saved.png").build());
        PostUpdateRequest request = new PostUpdateRequest("after");

        // when
        PostResponse response = postService.update(author.getId(), post.getId(), request);

        // then
        assertThat(postResponse.caption()).isEqualTo("after");
    }

    @Test
    @DisplayName("게시물을 삭제한다")
    void delete_success() throws Exception {
        // given
        User author = userRepository.save(User.builder().email("test123@test.com").userName("test").password("testpw").build());
        Post post = postRepository.save(Post.builder().user(author).caption("testCaption").imageStorageKey(author.getId() + "/saved.png").build());

        // when
        postService.delete(author.getId(), post.getId());

        // then
        assertThat(postRepository.findById(post.getId())).isEmpty();
    }
}
