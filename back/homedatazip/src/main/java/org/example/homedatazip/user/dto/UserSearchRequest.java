package org.example.homedatazip.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserSearchRequest(
        String type,
        @NotBlank(message = "키워드를 입력해주세요.")
        String keyword
) {
}
