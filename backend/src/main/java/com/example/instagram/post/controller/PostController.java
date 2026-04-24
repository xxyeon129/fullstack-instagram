package com.example.instagram.post.controller;

import com.example.instagram.global.response.ApiResponse;
import com.example.instagram.post.dto.PostResponse;
import com.example.instagram.post.dto.PostUpdateRequest;
import com.example.instagram.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> create(
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "caption", required = false) String caption,
            @AuthenticationPrincipal Long userId) {
        PostResponse response = postService.create(userId, caption, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("게시물이 등록되었습니다.", response));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getById(@PathVariable Long postId) {
        PostResponse response = postService.getById(postId);
        return ResponseEntity.ok(ApiResponse.ok("게시물 조회에 성공했습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponse>>> feed(Pageable pageable) {
        Page<PostResponse> page = postService.findFeed(pageable);
        return ResponseEntity.ok(ApiResponse.ok("게시물 목록 조회에 성공했습니다.", page));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> update(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal Long userId) {
        PostResponse response = postService.update(userId, postId, request);
        return ResponseEntity.ok(ApiResponse.ok("게시물이 수정되었습니다.", response));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {
        postService.delete(userId, postId);
        return ResponseEntity.ok(ApiResponse.ok("게시물이 삭제되었습니다.", null));
    }
}
