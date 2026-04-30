package com.example.instagram.post.dto;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        Long authorId,
        String authorUsername,
        String caption,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
