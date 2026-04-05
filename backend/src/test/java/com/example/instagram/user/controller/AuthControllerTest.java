package com.example.instagram.user.controller;

import com.example.instagram.global.config.SecurityConfig;
import com.example.instagram.global.exception.CustomException;
import com.example.instagram.global.security.JwtAuthenticationFilter;
import com.example.instagram.user.dto.AccessTokenResponse;
import com.example.instagram.user.dto.LoginRequest;
import com.example.instagram.user.dto.LoginResponse;
import com.example.instagram.user.dto.RefreshTokenRequest;
import com.example.instagram.user.dto.SignupRequest;
import com.example.instagram.user.dto.SignupResponse;
import com.example.instagram.user.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void jwtFilterPassesThrough() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Nested
    @DisplayName("POST /api/v1/auth/signup")
    class Signup {

        @Test
        @DisplayName("정상 요청이면 201과 사용자 정보를 반환한다")
        void 성공() throws Exception {
            SignupRequest request = new SignupRequest("test@example.com", "password123", "testuser");
            SignupResponse response = new SignupResponse(1L, "test@example.com", "testuser");

            given(authService.signup(any(SignupRequest.class))).willReturn(response);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }

        @Test
        @DisplayName("이메일이 중복되면 409를 반환한다")
        void 이메일_중복_409() throws Exception {
            SignupRequest request = new SignupRequest("dup@example.com", "password123", "testuser");

            given(authService.signup(any(SignupRequest.class)))
                    .willThrow(new CustomException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."));

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        }

        @Test
        @DisplayName("사용자명이 중복되면 409를 반환한다")
        void 사용자명_중복_409() throws Exception {
            SignupRequest request = new SignupRequest("new@example.com", "password123", "dupuser");

            given(authService.signup(any(SignupRequest.class)))
                    .willThrow(new CustomException(HttpStatus.CONFLICT, "이미 사용 중인 사용자 이름입니다."));

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이미 사용 중인 사용자 이름입니다."));
        }

        @Test
        @DisplayName("이메일 형식이 올바르지 않으면 400을 반환한다")
        void 이메일_형식_오류_400() throws Exception {
            SignupRequest request = new SignupRequest("invalid-email", "password123", "testuser");

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400을 반환한다")
        void 비밀번호_길이_오류_400() throws Exception {
            SignupRequest request = new SignupRequest("test@example.com", "short", "testuser");

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("사용자명이 1자이면 400을 반환한다")
        void 사용자명_길이_오류_400() throws Exception {
            SignupRequest request = new SignupRequest("test@example.com", "password123", "a");

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("이메일과 비밀번호가 일치하면 200과 토큰을 반환한다")
        void 성공() throws Exception {
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            LoginResponse response = new LoginResponse("accessToken", "refreshToken");

            given(authService.login(any(LoginRequest.class))).willReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                    .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"));
        }

        @Test
        @DisplayName("이메일 또는 비밀번호가 틀리면 401을 반환한다")
        void 인증_실패_401() throws Exception {
            LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");

            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new CustomException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("이메일이 비어있으면 400을 반환한다")
        void 이메일_누락_400() throws Exception {
            LoginRequest request = new LoginRequest("", "password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("유효한 리프레시 토큰이면 200과 새 액세스 토큰을 반환한다")
        void 성공() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");
            AccessTokenResponse response = new AccessTokenResponse("newAccessToken");

            given(authService.refreshAccessToken(any(RefreshTokenRequest.class))).willReturn(response);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("액세스 토큰 재발급에 성공했습니다."))
                    .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"));
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰이면 401을 반환한다")
        void 유효하지_않은_토큰_401() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("invalidToken");

            given(authService.refreshAccessToken(any(RefreshTokenRequest.class)))
                    .willThrow(new CustomException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("유효하지 않은 리프레시 토큰입니다."));
        }

        @Test
        @DisplayName("리프레시 토큰이 비어있으면 400을 반환한다")
        void 토큰_누락_400() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("");

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
