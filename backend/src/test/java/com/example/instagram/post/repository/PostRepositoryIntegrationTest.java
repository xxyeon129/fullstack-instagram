package com.example.instagram.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.instagram.post.entity.Post;
import com.example.instagram.user.entity.User;
import com.example.instagram.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class PostRepositoryIntegrationTest {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("게시물을 저장하고 작성자와 함께 조회할 수 있다")
    void save_and_findWithUser() {
        User author = userRepository.save(User.builder()
                .email("a@b.com")
                .username("author")
                .password("secret")
                .build());

        Post saved = postRepository.save(Post.builder()
                .user(author)
                .caption("hello")
                .imageStorageKey(author.getId() + "/img.png")
                .build());

        Optional<Post> found = postRepository.findWithUserById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUsername()).isEqualTo("author");
        assertThat(found.get().getCaption()).isEqualTo("hello");
    }

    @Test
    @DisplayName("목록 조회 시 작성자 정보를 함께 로드한다")
    void feed_includesAuthor() {
        User author = userRepository.save(User.builder()
                .email("c@d.com")
                .username("u2")
                .password("secret")
                .build());

        postRepository.save(Post.builder()
                .user(author)
                .caption("p1")
                .imageStorageKey(author.getId() + "/a.png")
                .build());

        Page<Post> page = postRepository.findAllWithUserOrderByCreatedAtDesc(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getUser().getEmail()).isEqualTo("c@d.com");
    }
}
