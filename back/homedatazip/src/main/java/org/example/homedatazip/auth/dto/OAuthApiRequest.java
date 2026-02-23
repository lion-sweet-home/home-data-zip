package org.example.homedatazip.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthApiRequest(
        @NotBlank(message = "인가 코드가 필요합니다.")
        String code,
        @NotBlank(message = "리다이렉트 URI가 필요합니다.")
        String redirectUri
) {}
