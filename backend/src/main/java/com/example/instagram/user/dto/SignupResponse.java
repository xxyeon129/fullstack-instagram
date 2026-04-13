package com.example.instagram.user.dto;

public record SignupResponse(
        Long userId,
        String email,
        String username
) {
}
