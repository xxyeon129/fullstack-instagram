package com.example.instagram.user.service;

import com.example.instagram.global.exception.CustomException;
import com.example.instagram.global.security.JwtTokenProvider;
import com.example.instagram.user.dto.AccessTokenResponse;
import com.example.instagram.user.dto.RefreshTokenRequest;
import com.example.instagram.user.entity.User;
import com.example.instagram.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthQueryServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthQueryService authQueryService;

    private User buildUser(Long id, String email, String username) {
        User user = User.builder()
                .email(email)
                .username(username)
                .password("encodedPassword")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Nested
    @DisplayName("refreshAccessToken()")
    class RefreshAccessToken {

        @Test
        @DisplayName("유효한 리프레시 토큰이면 새 액세스 토큰을 반환한다")
        void 성공() {
            RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");
            User user = buildUser(1L, "test@example.com", "testuser");

            given(jwtTokenProvider.getUserId(request.refreshToken())).willReturn(1L);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("refresh:1")).willReturn("validRefreshToken");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(jwtTokenProvider.createAccessToken(1L, user.getEmail())).willReturn("newAccessToken");

            AccessTokenResponse response = authQueryService.refreshAccessToken(request);

            assertThat(response.accessToken()).isEqualTo("newAccessToken");
        }

        @Test
        @DisplayName("토큰 파싱에 실패하면 UNAUTHORIZED 예외를 던진다")
        void 토큰_파싱_실패_예외() {
            RefreshTokenRequest request = new RefreshTokenRequest("invalidToken");
            given(jwtTokenProvider.getUserId(request.refreshToken())).willThrow(new RuntimeException("파싱 오류"));

            assertThatThrownBy(() -> authQueryService.refreshAccessToken(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("유효하지 않은 리프레시 토큰입니다.")
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        @DisplayName("Redis에 저장된 토큰과 불일치하면 UNAUTHORIZED 예외를 던진다")
        void 토큰_불일치_예외() {
            RefreshTokenRequest request = new RefreshTokenRequest("tokenA");

            given(jwtTokenProvider.getUserId(request.refreshToken())).willReturn(1L);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("refresh:1")).willReturn("tokenB");

            assertThatThrownBy(() -> authQueryService.refreshAccessToken(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("유효하지 않은 리프레시 토큰입니다.")
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        @DisplayName("Redis에 토큰이 없으면 UNAUTHORIZED 예외를 던진다")
        void Redis에_토큰_없음_예외() {
            RefreshTokenRequest request = new RefreshTokenRequest("someToken");

            given(jwtTokenProvider.getUserId(request.refreshToken())).willReturn(1L);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("refresh:1")).willReturn(null);

            assertThatThrownBy(() -> authQueryService.refreshAccessToken(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("유효하지 않은 리프레시 토큰입니다.")
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        @DisplayName("토큰은 유효하나 사용자가 존재하지 않으면 UNAUTHORIZED 예외를 던진다")
        void 사용자_없음_예외() {
            RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");

            given(jwtTokenProvider.getUserId(request.refreshToken())).willReturn(999L);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("refresh:999")).willReturn("validRefreshToken");
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authQueryService.refreshAccessToken(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("사용자를 찾을 수 없습니다.")
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
    }
}
