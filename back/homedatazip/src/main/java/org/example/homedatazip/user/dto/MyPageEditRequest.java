package org.example.homedatazip.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MyPageEditRequest (
        @NotBlank
        @Size(min = 2, max = 30)
        String nickname,
        String password
){}
