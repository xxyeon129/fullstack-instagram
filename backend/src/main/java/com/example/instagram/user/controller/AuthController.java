package com.example.instagram.user.controller;

import com.example.instagram.global.response.ApiResponse;
import com.example.instagram.user.dto.LoginRequest;
import com.example.instagram.user.dto.LoginResponse;
import com.example.instagram.user.dto.AccessTokenResponse;
import com.example.instagram.user.dto.RefreshTokenRequest;
import com.example.instagram.user.dto.SignupRequest;
import com.example.instagram.user.dto.SignupResponse;
import com.example.instagram.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("회원가입이 완료되었습니다.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("로그인에 성공했습니다.", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AccessTokenResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(ApiResponse.ok("액세스 토큰 재발급에 성공했습니다.", response));
    }
}
