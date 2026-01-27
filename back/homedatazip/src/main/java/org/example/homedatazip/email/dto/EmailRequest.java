package org.example.homedatazip.email.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @NotBlank(message = "이메일을 입력해주세요")
        String email
) {
}
