package com.example.instagram.post.dto;

import jakarta.validation.constraints.Size;

public record PostUpdateRequest(
        @Size(max = 2200, message = "캡션은 2200자 이하여야 합니다.")
        String caption
) {
}
