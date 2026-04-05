package com.example.instagram.user.service;

import com.example.instagram.global.exception.CustomException;
import com.example.instagram.global.exception.ErrorCode;
import com.example.instagram.global.security.JwtTokenProvider;
import com.example.instagram.user.dto.LoginRequest;
import com.example.instagram.user.dto.LoginResponse;
import com.example.instagram.user.dto.SignupRequest;
import com.example.instagram.user.dto.SignupResponse;
import com.example.instagram.user.entity.User;
import com.example.instagram.user.repository.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.refresh-token-exp-days}")
    private long refreshTokenExpDays;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .build();

        User savedUser = userRepository.save(user);
        return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getUsername());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        String key = "refresh:" + user.getId();
        stringRedisTemplate.opsForValue().set(key, refreshToken, Duration.ofDays(refreshTokenExpDays));

        return new LoginResponse(accessToken, refreshToken);
    }
}
