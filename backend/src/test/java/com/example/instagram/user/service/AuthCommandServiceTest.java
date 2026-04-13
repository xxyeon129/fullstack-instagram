package com.example.instagram.user.service;

import com.example.instagram.global.exception.CustomException;
import com.example.instagram.global.security.JwtTokenProvider;
import com.example.instagram.user.dto.LoginRequest;
import com.example.instagram.user.dto.LoginResponse;
import com.example.instagram.user.dto.SignupRequest;
import com.example.instagram.user.dto.SignupResponse;
import com.example.instagram.user.entity.User;
import com.example.instagram.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthCommandService authCommandService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authCommandService, "refreshTokenExpDays", 7L);
    }

    private User buildUser(Long id, String email, String username) {
        User user = User.builder()
                .email(email)
                .username(username)
                .password("encodedPassword")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Nested
    @DisplayName("signup()")
    class Signup {

        @Test
        @DisplayName("정상 요청이면 사용자 정보를 저장하고 응답을 반환한다")
        void 성공() {
            SignupRequest request = new SignupRequest("test@example.com", "ValidPass1!", "testuser");
            User savedUser = buildUser(1L, request.email(), request.username());

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.existsByUsername(request.username())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            SignupResponse response = authCommandService.signup(request);

            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("test@example.com");
            assertThat(response.username()).isEqualTo("testuser");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 CONFLICT 예외를 던진다")
        void 이메일_중복_예외() {
            SignupRequest request = new SignupRequest("dup@example.com", "ValidPass1!", "testuser");
            given(userRepository.existsByEmail(request.email())).willReturn(true);

            assertThatThrownBy(() -> authCommandService.signup(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("이미 사용 중인 이메일입니다.")
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("이미 사용 중인 사용자명이면 CONFLICT 예외를 던진다")
        void 사용자명_중복_예외() {
            SignupRequest request = new SignupRequest("new@example.com", "ValidPass1!", "dupuser");
            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(userRepository.existsByUsername(request.username())).willReturn(true);

            assertThatThrownBy(() -> authCommandService.signup(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("이미 사용 중인 사용자 이름입니다.")
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("이메일과 비밀번호가 일치하면 액세스 토큰과 리프레시 토큰을 반환한다")
        void 성공() {
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            User user = buildUser(1L, request.email(), "testuser");

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtTokenProvider.createAccessToken(1L, user.getEmail())).willReturn("accessToken");
            given(jwtTokenProvider.createRefreshToken(1L)).willReturn("refreshToken");
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

            LoginResponse response = authCommandService.login(request);

            assertThat(response.accessToken()).isEqualTo("accessToken");
            assertThat(response.refreshToken()).isEqualTo("refreshToken");
            then(valueOperations).should().set(eq("refresh:1"), eq("refreshToken"), any());
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 UNAUTHORIZED 예외를 던진다")
        void 이메일_없음_예외() {
            LoginRequest request = new LoginRequest("notfound@example.com", "password123");
            given(userRepository.findByEmail(request.email())).willReturn(Optional.empty());

            assertThatThrownBy(() -> authCommandService.login(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.")
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 UNAUTHORIZED 예외를 던진다")
        void 비밀번호_불일치_예외() {
            LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");
            User user = buildUser(1L, request.email(), "testuser");

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

            assertThatThrownBy(() -> authCommandService.login(request))
                    .isInstanceOf(CustomException.class)
                    .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.")
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
    }
}
