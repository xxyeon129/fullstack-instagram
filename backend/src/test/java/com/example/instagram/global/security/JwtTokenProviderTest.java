package com.example.instagram.global.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpMinutes", 15L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpDays", 7L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("액세스 토큰 생성 후 사용자 ID 추출 성공")
    void createAccessToken_성공() {
        String token = jwtTokenProvider.createAccessToken(1L, "test@example.com");

        Long userId = jwtTokenProvider.getUserId(token);

        assertThat(token).isNotBlank();
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("리프레시 토큰 생성 후 사용자 ID 추출 성공")
    void createRefreshToken_성공() {
        String token = jwtTokenProvider.createRefreshToken(1L);

        Long userId = jwtTokenProvider.getUserId(token);

        assertThat(token).isNotBlank();
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰은 서로 다른 값을 가진다")
    void accessToken과_refreshToken은_다르다() {
        String accessToken = jwtTokenProvider.createAccessToken(1L, "test@example.com");
        String refreshToken = jwtTokenProvider.createRefreshToken(1L);

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰으로 사용자 ID 추출 시 예외 발생")
    void getUserId_잘못된토큰_예외발생() {
        assertThatThrownBy(() -> jwtTokenProvider.getUserId("invalid.token.value"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("빈 문자열 토큰으로 사용자 ID 추출 시 예외 발생")
    void getUserId_빈토큰_예외발생() {
        assertThatThrownBy(() -> jwtTokenProvider.getUserId(""))
                .isInstanceOf(Exception.class);
    }
}
