package org.example.homedatazip.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요")
        String newPassword,

        @NotBlank(message = "비밀번호를 다시 입력해주세요")
        String confirmPassword
) {
}
