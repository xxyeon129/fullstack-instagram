package com.example.instagram.user.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
