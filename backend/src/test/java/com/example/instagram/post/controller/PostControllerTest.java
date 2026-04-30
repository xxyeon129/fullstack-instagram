package com.example.instagram.post.controller;

import com.example.instagram.global.config.SecurityConfig;
import com.example.instagram.global.exception.CustomException;
import com.example.instagram.global.exception.ErrorCode;
import com.example.instagram.global.security.JwtAuthenticationFilter;
import com.example.instagram.post.dto.PostResponse;
import com.example.instagram.post.dto.PostUpdateRequest;
import com.example.instagram.post.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doAnswer;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final PostResponse SAMPLE = new PostResponse(
            10L,
            1L,
            "writer",
            "캡션",
            "/api/v1/media/1/abc.png",
            LocalDateTime.now(),
            LocalDateTime.now()
    );

    @BeforeEach
    void jwtFilterPassesThrough() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private static UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
    }

    @Nested
    @DisplayName("POST /api/v1/posts (multipart)")
    class Create {

        @Test
        @DisplayName("이미지와 캡션(선택)으로 등록하면 201과 게시물 정보를 반환한다")
        void success() throws Exception {
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "photo.png",
                    "image/png",
                    "fake-image".getBytes()
            );

            given(postService.create_post(eq(1L), eq("hello"), any())).willReturn(SAMPLE);

            mockMvc.perform(multipart("/api/v1/posts")
                            .file(image)
                            .param("caption", "hello")
                            .with(authentication(auth(1L))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("게시물이 등록되었습니다."))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.authorId").value(1))
                    .andExpect(jsonPath("$.data.imageUrl").value("/api/v1/media/1/abc.png"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts/{postId}")
    class GetById {

        @Test
        @DisplayName("존재하는 게시물이면 200과 상세를 반환한다")
        void success() throws Exception {
            given(postService.getById(10L)).willReturn(SAMPLE);

            mockMvc.perform(get("/api/v1/posts/10").with(authentication(auth(1L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.authorUsername").value("writer"));
        }

        @Test
        @DisplayName("없는 게시물이면 404를 반환한다")
        void notFoundReturns404() throws Exception {
            given(postService.getById(99L)).willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

            mockMvc.perform(get("/api/v1/posts/99").with(authentication(auth(1L))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("게시물을 찾을 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts (feed)")
    class Feed {

        @Test
        @DisplayName("페이지네이션으로 목록을 반환한다")
        void success() throws Exception {
            given(postService.findFeed(any())).willReturn(
                    new PageImpl<>(List.of(SAMPLE), PageRequest.of(0, 20), 1)
            );

            mockMvc.perform(get("/api/v1/posts").with(authentication(auth(1L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(10));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/posts/{postId}")
    class Update {

        @Test
        @DisplayName("캡션 수정에 성공하면 200을 반환한다")
        void success() throws Exception {
            PostUpdateRequest body = new PostUpdateRequest("new caption");
            given(postService.update(eq(1L), eq(10L), any(PostUpdateRequest.class))).willReturn(SAMPLE);

            mockMvc.perform(put("/api/v1/posts/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .with(authentication(auth(1L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("게시물이 수정되었습니다."));
        }

        @Test
        @DisplayName("캡션이 2200자를 넘으면 400을 반환한다")
        void captionTooLongReturns400() throws Exception {
            String tooLong = "x".repeat(2201);
            PostUpdateRequest body = new PostUpdateRequest(tooLong);

            mockMvc.perform(put("/api/v1/posts/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .with(authentication(auth(1L))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/posts/{postId}")
    class Delete {

        @Test
        @DisplayName("삭제에 성공하면 200을 반환한다")
        void success() throws Exception {
            mockMvc.perform(delete("/api/v1/posts/10").with(authentication(auth(1L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("게시물이 삭제되었습니다."));
        }
    }
}
