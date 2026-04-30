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
import org.springframework.web.multipart.MultipartFile;import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ImageStorage imageStorage;

    @Value("${app.media.public-base-path:/api/v1/media}")
    private String mediaPublicBasePath;

    @Transactional
    public PostResponse create_post(Long authorId, String caption, MultipartFile image) {
        // 1. 유저 조회
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 이미지를 저장하고 storage key를 받음 --> 이미지 저장/구현 방식은 더 확인 필요
        String storageKey = imageStorage.store(authorId, image);
        Post post = Post.builder().user(author).caption(caption).imageStorageKey(storageKey).build();

        // 3. Post 엔티티를 빌더로 생성
        return toResponse(postRepository.save(post));
    }
//    public PostResponse create(Long authorId, String caption, MultipartFile image) {
//        User author = userRepository.findById(authorId)
//                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//
//        String storageKey = imageStorage.store(authorId, image);
//        Post post = Post.builder()
//                .user(author)
//                .caption(caption)
//                .imageStorageKey(storageKey)
//                .build();
//        Post saved = postRepository.save(post);
//        return toResponse(saved);
//    }

    @Transactional(readOnly = true)
    public PostResponse getById(Long postId) {
        // 1. Post 조회
        // findById를 사용하지 않는 이유: Transactional(readOnly = true) 환경에서 lazy 로딩이 발생
        // findWithUserById는 @EntityGraph??로 User를 함께 조회
        Post post = postRepository.findWithUserById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. toResponse()로 변환
        return toResponse(post);
    }
//    public PostResponse getById(Long postId) {
//        Post post = postRepository.findWithUserById(postId)
//                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
//        return toResponse(post);
//    }

    @Transactional(readOnly = true)
    public Page<PostResponse> findFeed(Pageable pageable) {
        return postRepository.findAllWithUserOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PostResponse update(Long requesterId, Long postId, PostUpdateRequest request) {
        // 1. post 조회
        Post post = postRepository.findWithUserById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 본인 게시물인지 검증
        if (!post.getUser().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_POST_ACCESS);
        }

        // 3. updateCaption 호출
        post.updateCaption(request.caption());

        // 4. toResponse 반환
        return toResponse(post);
    }
//    public PostResponse update(Long requesterId, Long postId, PostUpdateRequest request) {
//        Post post = postRepository.findWithUserById(postId)
//                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
//        assertOwner(requesterId, post);
//        post.updateCaption(request.caption());
//        return toResponse(post);
//    }

    @Transactional
    public void delete(Long authorId, Long postId) {
        // 1. Post 조회
        Post post = postRepository.findWithUserById(authorId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 본인 게시물인지 검증
        if (!post.getUser().getId().equals(postId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_POST_ACCESS);
        }

        // 3. 이미지 삭제 처리 <-- 이미지 저장/구현 방식은 더 확인 필요
        imageStorage.deleteIfExists(post.getImageStorageKey());

        // 4. post 삭제 처리
        postRepository.delete(post);
    }
//    public void delete(Long requesterId, Long postId) {
//        Post post = postRepository.findWithUserById(postId)
//                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
//        assertOwner(requesterId, post);
//        imageStorage.deleteIfExists(post.getImageStorageKey());
//        postRepository.delete(post);
//    }

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
