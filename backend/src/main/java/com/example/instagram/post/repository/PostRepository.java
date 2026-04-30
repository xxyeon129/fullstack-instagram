package com.example.instagram.post.repository;

import com.example.instagram.post.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "user")
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    Page<Post> findAllWithUserOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findWithUserById(@Param("id") Long id);
}
