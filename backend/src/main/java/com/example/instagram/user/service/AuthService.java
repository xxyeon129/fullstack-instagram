package com.example.instagram.user.service;

import com.example.instagram.user.dto.LoginRequest;
import com.example.instagram.user.dto.LoginResponse;
import com.example.instagram.user.dto.AccessTokenResponse;
import com.example.instagram.user.dto.RefreshTokenRequest;
import com.example.instagram.user.dto.SignupRequest;
import com.example.instagram.user.dto.SignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthCommandService authCommandService;
    private final AuthQueryService authQueryService;

    public SignupResponse signup(SignupRequest request) {
        return authCommandService.signup(request);
    }

    public LoginResponse login(LoginRequest request) {
        return authCommandService.login(request);
    }

    public AccessTokenResponse refreshAccessToken(RefreshTokenRequest request) {
        return authQueryService.refreshAccessToken(request);
    }
}
