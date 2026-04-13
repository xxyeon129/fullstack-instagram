package com.example.instagram.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?#^&+=])[A-Za-z\\d@$!%*?#^&+=]+$",
                message = "비밀번호는 영문 대소문자, 숫자, 특수문자(@$!%*?#^&+=)를 각각 하나 이상 포함해야 합니다.")
        String password,

        @NotBlank(message = "사용자 이름은 필수입니다.")
        @Size(min = 2, max = 50, message = "사용자 이름은 2자 이상 50자 이하여야 합니다.")
        String username
) {
}
