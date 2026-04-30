package com.example.instagram.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.instagram.post.entity.Post;
import com.example.instagram.user.entity.User;
import com.example.instagram.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시물 목록 + 작성자 조회 시 N+1을 피하기 위한 쿼리 상한을 고정한다.
 * (구현이 EntityGraph/조인 전략을 바꿀 때 테스트가 실패하도록 한다.)
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class PostQueryPerformanceIntegrationTest {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private SessionFactory sessionFactory;

    @BeforeEach
    void setUp() {
        sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("피드 조회 시 작성자 접근까지 포함해 준비된 SQL 개수가 과도하게 늘지 않는다")
    void feed_doesNotExplodeStatementCount() {
        User author = userRepository.save(User.builder()
                .email("perf@example.com")
                .username("perfuser")
                .password("secret")
                .build());

        for (int i = 0; i < 15; i++) {
            postRepository.save(Post.builder()
                    .user(author)
                    .caption("c" + i)
                    .imageStorageKey(author.getId() + "/img-" + i + ".png")
                    .build());
        }
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        Page<Post> page = postRepository.findAllWithUserOrderByCreatedAtDesc(PageRequest.of(0, 20));
        page.getContent().forEach(p -> p.getUser().getUsername());

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3L);
    }
}
