package com.example.instagram.user.service;

import com.example.instagram.user.dto.AccessTokenResponse;
import com.example.instagram.user.dto.LoginRequest;
import com.example.instagram.user.dto.LoginResponse;
import com.example.instagram.user.dto.RefreshTokenRequest;
import com.example.instagram.user.dto.SignupRequest;
import com.example.instagram.user.dto.SignupResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthCommandService authCommandService;
    @Mock
    private AuthQueryService authQueryService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("signup()")
    class Signup {

        @Test
        @DisplayName("AuthCommandService.signup에 위임한다")
        void delegates() {
            SignupRequest request = new SignupRequest("test@example.com", "ValidPass1!", "testuser");
            SignupResponse expected = new SignupResponse(1L, request.email(), request.username());
            given(authCommandService.signup(request)).willReturn(expected);

            SignupResponse response = authService.signup(request);

            assertThat(response).isEqualTo(expected);
            then(authCommandService).should().signup(request);
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("AuthCommandService.login에 위임한다")
        void delegates() {
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            LoginResponse expected = new LoginResponse("access", "refresh");
            given(authCommandService.login(request)).willReturn(expected);

            LoginResponse response = authService.login(request);

            assertThat(response).isEqualTo(expected);
            then(authCommandService).should().login(request);
        }
    }

    @Nested
    @DisplayName("refreshAccessToken()")
    class RefreshAccessToken {

        @Test
        @DisplayName("AuthQueryService.refreshAccessToken에 위임한다")
        void delegates() {
            RefreshTokenRequest request = new RefreshTokenRequest("refresh");
            AccessTokenResponse expected = new AccessTokenResponse("newAccess");
            given(authQueryService.refreshAccessToken(request)).willReturn(expected);

            AccessTokenResponse response = authService.refreshAccessToken(request);

            assertThat(response).isEqualTo(expected);
            then(authQueryService).should().refreshAccessToken(request);
        }
    }
}
