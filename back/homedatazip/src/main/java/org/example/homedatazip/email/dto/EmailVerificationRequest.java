package org.example.homedatazip.email.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @NotBlank(message = "이메일을 입력해주세요")
        String email,
        @NotBlank(message = "인증 코드를 입력해주세요")
        String authCode
) {
}
