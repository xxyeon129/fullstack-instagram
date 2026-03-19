package com.example.instagram.user.service;

import com.example.instagram.global.exception.CustomException;
import com.example.instagram.global.security.JwtTokenProvider;
import com.example.instagram.user.dto.LoginRequest;
import com.example.instagram.user.dto.LoginResponse;
import com.example.instagram.user.dto.AccessTokenResponse;
import com.example.instagram.user.dto.RefreshTokenRequest;
import com.example.instagram.user.dto.SignupRequest;
import com.example.instagram.user.dto.SignupResponse;
import com.example.instagram.user.entity.User;
import com.example.instagram.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.refresh-token-exp-days}")
    private long refreshTokenExpDays;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 사용 중인 사용자 이름입니다.");
        }

        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getUsername());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        String key = "refresh:" + user.getId();
        stringRedisTemplate.opsForValue().set(key, refreshToken, Duration.ofDays(refreshTokenExpDays));

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public AccessTokenResponse refreshAccessToken(RefreshTokenRequest request) {
        Long userId;
        try {
            userId = jwtTokenProvider.getUserId(request.refreshToken());
        } catch (Exception e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }
        String key = "refresh:" + userId;
        String savedToken = stringRedisTemplate.opsForValue().get(key);

        if (savedToken == null || !savedToken.equals(request.refreshToken())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        return new AccessTokenResponse(accessToken);
    }
}
