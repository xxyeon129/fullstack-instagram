package com.example.instagram.post.service;

import com.example.instagram.global.exception.CustomException;
import com.example.instagram.global.exception.ErrorCode;
import com.example.instagram.post.dto.PostResponse;
import com.example.instagram.post.dto.PostUpdateRequest;
import com.example.instagram.post.entity.Post;
import com.example.instagram.post.repository.PostRepository;
import com.example.instagram.post.storage.ImageStorage;
import com.example.instagram.user.entity.User;
import com.example.instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ImageStorage imageStorage;

    @Value("${app.media.public-base-path:/api/v1/media}")
    private String mediaPublicBasePath;

    @Transactional
    public PostResponse create(Long authorId, String caption, MultipartFile image) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String storageKey = imageStorage.store(authorId, image);
        Post post = Post.builder()
                .user(author)
                .caption(caption)
                .imageStorageKey(storageKey)
                .build();
        Post saved = postRepository.save(post);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PostResponse getById(Long postId) {
        Post post = postRepository.findWithUserById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> findFeed(Pageable pageable) {
        return postRepository.findAllWithUserOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PostResponse update(Long requesterId, Long postId, PostUpdateRequest request) {
        Post post = postRepository.findWithUserById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        assertOwner(requesterId, post);
        post.updateCaption(request.caption());
        return toResponse(post);
    }

    @Transactional
    public void delete(Long requesterId, Long postId) {
        Post post = postRepository.findWithUserById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        assertOwner(requesterId, post);
        imageStorage.deleteIfExists(post.getImageStorageKey());
        postRepository.delete(post);
    }

    private void assertOwner(Long requesterId, Post post) {
        if (!post.getUser().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_POST_ACCESS);
        }
    }

    private PostResponse toResponse(Post post) {
        User user = post.getUser();
        return new PostResponse(
                post.getId(),
                user.getId(),
                user.getUsername(),
                post.getCaption(),
                buildImageUrl(post.getImageStorageKey()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private String buildImageUrl(String storageKey) {
        String base = mediaPublicBasePath.endsWith("/")
                ? mediaPublicBasePath.substring(0, mediaPublicBasePath.length() - 1)
                : mediaPublicBasePath;
        return base + "/" + storageKey;
    }
}
