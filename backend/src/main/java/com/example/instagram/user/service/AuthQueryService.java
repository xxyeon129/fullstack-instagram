package com.example.instagram.user.service;

import com.example.instagram.global.exception.CustomException;
import com.example.instagram.global.exception.ErrorCode;
import com.example.instagram.global.security.JwtTokenProvider;
import com.example.instagram.user.dto.AccessTokenResponse;
import com.example.instagram.user.dto.RefreshTokenRequest;
import com.example.instagram.user.entity.User;
import com.example.instagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthQueryService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional(readOnly = true)
    public AccessTokenResponse refreshAccessToken(RefreshTokenRequest request) {
        Long userId;
        try {
            userId = jwtTokenProvider.getUserId(request.refreshToken());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String key = "refresh:" + userId;
        String savedToken = stringRedisTemplate.opsForValue().get(key);

        if (savedToken == null || !savedToken.equals(request.refreshToken())) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        return new AccessTokenResponse(accessToken);
    }
}
